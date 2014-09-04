/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.search;

import com.ibm.icu.text.Collator;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.facetbrowser.FacetSearchNode;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetResultExternal;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.lucene.LuceneManipulator;
import dk.statsbiblioteket.summa.index.lucene.StreamingDocumentCreator;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.index.*;
import org.apache.lucene.search.exposed.ExposedUtil;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Tests Exposed collator key concatenated fields.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ConcatTest extends TestCase {
//    private static Log log = LogFactory.getLog(ConcatTest.class);

    public static final File INDEX_DESCRIPTOR = Resolver.getFile("search/concat/concat_IndexDescriptor.xml");
    public static final File INDEX_LOCATION = new File("concat_index_tmp");

    public static final String ONE = "search/concat/documents/one_concat.xml";
    public static final String TWO = "search/concat/documents/two_concat.xml";
    public static final String MANY = "search/concat/documents/many_concat.xml";

    public ConcatTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!INDEX_LOCATION.mkdirs()) {
            System.out.println("Unable to create folder '" + INDEX_LOCATION.getAbsolutePath() + "'");
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (INDEX_LOCATION.exists()) {
            Files.delete(INDEX_LOCATION);
        }
    }

    public static Test suite() {
        return new TestSuite(ConcatTest.class);
    }

    public void testKeyGeneration() {
        Collator collator = Collator.getInstance(new Locale("da"));
        ExposedUtil.addCollator("da", collator);
        for (String term: new String[]{"crab", "crème", "crow"}) {
            BytesRef concat = ExposedUtil.concat("da", new BytesRef(term), null);
            System.out.println(term + ": " + ExposedUtil.getConcatHex(concat));
        }
    }

    public void testTruncation() throws IOException {
        createIndex(ONE, TWO, MANY);
        {
            ResponseCollection responses = search(new Request(
                    DocumentKeys.SEARCH_QUERY, "concat:concat\\ ze*",
                    DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
                    FacetKeys.SEARCH_FACET_FACETS, "concat"
            ));
            assertEquals("The number of hits should be correct",
                    1, getHitcount(responses));
            assertTrue("The result should contain document many",
                    responses.toXML().contains("<field name=\"id\">many</field>"));
        }
    }

    private long getHitcount(ResponseCollection responses) {
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                DocumentResponse docs = (DocumentResponse)response;
                return docs.getHitCount();
            }
        }
        return -1;
    }

    // TODO: Develop test that fails when terms are double collator sorted to verify single sorting
    public void testSorting() throws IOException {
        createIndex(ONE, TWO, MANY);
        ResponseCollection responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "*:*",
                DocumentKeys.SEARCH_SORTKEY, "sort",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
                FacetKeys.SEARCH_FACET_FACETS, "concat"
        ));
        List<String> ids = getIDs(responses);
        assertEquals("The order of the ids should be as expected", "two, one, many", Strings.join(ids, ", "));
    }

    private List<String> getIDs(ResponseCollection responses) {
        List<String> ids = new ArrayList<>();
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                DocumentResponse docs = (DocumentResponse)response;
                for (DocumentResponse.Record record: docs.getRecords()) {
                    for (DocumentResponse.Field field: record.getFields()) {
                        if ("id".equals(field.getName())) {
                            ids.add(field.getContent());
                        }
                    }
                }
            }
        }
        return ids;
    }

    public void testPlainFaceting() throws IOException {
        createIndex(ONE);
        ResponseCollection responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "id:one",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
                FacetKeys.SEARCH_FACET_FACETS, "plain"
        ));
        assertTrue("The responses should contain a tag with ål\n" + responses.toXML(),
                responses.toXML().contains("<tag name=\"plain ål\" addedobjects=\"1\" reliability=\"PRECISE\">"));
    }

    @SuppressWarnings("ConstantConditions")
    public void testIndexedConcat() throws IOException {
        createIndex(ONE);
        IndexReader indexReader = DirectoryReader.open(MMapDirectory.open(
                new File(INDEX_LOCATION.listFiles()[0], "lucene")));
        int count = 0;
        for (AtomicReaderContext context: indexReader.leaves()) {
            AtomicReader reader = context.reader();
            Terms terms = reader.terms("concat");
            TermsEnum te = terms.iterator(null);
            BytesRef term;
            while ((term = te.next()) != null) {
                String hexTerm = "";
                int zeroPos = -1;
                count++;
                for (int i = 0 ; i < term.length ; i++) {
                    byte b = term.bytes[term.offset + i];
                    hexTerm += Integer.toHexString(b) + " ";
                    if (b == 0 && zeroPos == -1) {
                        zeroPos = i;
                    }
                }
                assertFalse("There should be a 0 in the term [" + hexTerm + "]",
                        zeroPos == -1);
                assertFalse("The position of the first 0 should not be at the end in the term [" + hexTerm + "]",
                        term.offset+term.length-1 == zeroPos);
            }
        }
        assertEquals("There should be the correct number of terms", 1, count);
    }

    public void testConcatSingleTagFaceting() throws IOException {
        createIndex(ONE);
        ResponseCollection responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "id:one",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
                FacetKeys.SEARCH_FACET_FACETS, "concat"
        ));
        assertTrue("The responses should contain a tag with ål\n" + responses.toXML(),
                responses.toXML().contains("<tag name=\"concat ål\" addedobjects=\"1\" reliability=\"PRECISE\">"));
    }

    public void testConcatMultiTagFaceting() throws IOException {
        createIndex(ONE, TWO, MANY);
        ResponseCollection responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "*:*",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
                FacetKeys.SEARCH_FACET_FACETS, "concat"
        ));
        assertOrdered(responses, "concat");
    }

    // This does not really belong here
    public void testResponseFields() throws IOException {
        createIndex(TWO);
        ResponseCollection responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "*:*",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false,
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, plain"
        ));
        assertEquals("The number of terms for field 'plain' should be as expected in\n" + responses.toXML(),
                2, getTerms(responses, "plain").size());
//        System.out.println(responses.toXML());
    }

    // This does not really belong here
    public void testAlias() throws IOException {
        createIndex(TWO);
        ResponseCollection responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "almindelig:p*",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, false,
                DocumentKeys.SEARCH_RESULT_FIELDS, "recordID, plain"
        ));
        assertEquals("The number of terms for field 'plain' should be as expected in\n" + responses.toXML(),
                2, getTerms(responses, "plain").size());
//        System.out.println(responses.toXML());
    }

    private void assertOrdered(ResponseCollection responses, String field) {
        List<String> tags = getTags(responses, field);
        assertNotNull("There should be tags for facet '" + field + "'", tags);

        List<String> expected = new ArrayList<>(tags);
        Collections.sort(expected, Collator.getInstance(new Locale("da")));

        for (int i = 0 ; i < tags.size() ; i++) {
            assertEquals("The tag at index " + i + " from facet " + field + " should be as expected\nTags: "
                    + Strings.join(tags, ", "),
                    expected.get(i), tags.get(i));
        }
    }

    private List<String> getTags(ResponseCollection responses, String field) {
        for (Response response: responses) {
            if (response instanceof FacetResultExternal) {
                FacetResultExternal facets = (FacetResultExternal)response;
                return facets.getTags(field);
            }
        }
        return null;
    }

    private List<String> getTerms(ResponseCollection responses, String field) {
        List<String> terms = new ArrayList<>();
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                DocumentResponse docs = (DocumentResponse)response;
                for (DocumentResponse.Record record: docs.getRecords()) {
                    for (DocumentResponse.Field f: record.getFields()) {
                        if (field.equals(f.getName())) {
                            terms.add(f.getContent());
                        }
                    }
                }
            }
        }
        return terms;
    }

    public void testResultField() throws IOException {
        createIndex(ONE);
        String responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "id:one",
                DocumentKeys.SEARCH_RESULT_FIELDS, "id, concat"
        )).toXML();
        assertTrue("The responses should contain a record with the id:one\n" + responses,
                responses.contains("<field name=\"id\">one</field>"));
        assertTrue("The responses should contain a record with ål\n" + responses,
                responses.contains("<field name=\"concat\">concat ål</field>"));
//        System.out.println(responses);
    }

    public void testIndexCreationAndSearch() throws IOException {
        createIndex(ONE);
        ResponseCollection responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "id:one"
        ));
        assertTrue("The responses should contain a record with the id:one\n" + responses.toXML(),
                responses.toXML().contains("<field name=\"id\">one</field>"));
    }

    private ResponseCollection search(Request request) throws IOException {
        SummaSearcher searcher = getSearcher();
        try {
            return searcher.search(request);
        } finally {
            searcher.close();
        }
    }

    private SummaSearcher getSearcher() throws IOException {
        Configuration searcherConf = Configuration.newMemoryBased(
                IndexWatcher.CONF_INDEX_WATCHER_INDEX_ROOT, INDEX_LOCATION,
                IndexWatcher.CONF_INDEX_WATCHER_CHECK_INTERVAL, 1000,
                SearchNodeFactory.CONF_NODE_CLASS, SearchNodeAggregator.class
        );
        List<Configuration> nodeConfs = searcherConf.createSubConfigurations(SearchNodeFactory.CONF_NODES, 2);

        Configuration luceneConf = nodeConfs.get(0);
        luceneConf.set(SearchNodeFactory.CONF_NODE_CLASS, LuceneSearchNode.class);
        luceneConf.set(LuceneSearchNode.CONF_RESULT_FIELDS, "id");

        Configuration facetConf = nodeConfs.get(1);
        facetConf.set(SearchNodeFactory.CONF_NODE_CLASS, FacetSearchNode.class);

        return new SummaSearcherImpl(searcherConf);
    }

    private void createIndex(String... inputFiles) throws IOException {
        PayloadFeederHelper feeder = new PayloadFeederHelper(0, inputFiles);
        ObjectFilter creator = new StreamingDocumentCreator(createIndexDescriptorConf());
        creator.setSource(feeder);
        Configuration indexConf = createIndexDescriptorConf(
                IndexControllerImpl.CONF_INDEX_ROOT_LOCATION, INDEX_LOCATION.getAbsolutePath(),
                IndexControllerImpl.CONF_CREATE_NEW_INDEX, true
        );
        Configuration luceneConf = indexConf.createSubConfigurations(IndexControllerImpl.CONF_MANIPULATORS, 1).get(0);
        luceneConf.set(IndexControllerImpl.CONF_MANIPULATOR_CLASS, LuceneManipulator.class);
        ObjectFilter indexer = new IndexControllerImpl(indexConf);
        indexer.setSource(creator);
        try {
            for (int i = 0 ; i < inputFiles.length ; i++) {
                assertTrue("Record #" + (i+1) + " should be ready", indexer.hasNext());
                assertNotNull("Record #" + (i+1) + " should be indexed", indexer.next());
            }
            assertFalse("There should be no more records after #" + inputFiles.length, indexer.hasNext());
        } finally {
            indexer.close(true);
        }
    }

    private Configuration createIndexDescriptorConf(Serializable... args) throws IOException {
        Configuration conf = Configuration.newMemoryBased(args);
        conf.createSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR).set(
                IndexDescriptor.CONF_ABSOLUTE_LOCATION, INDEX_DESCRIPTOR.getAbsolutePath());
        return conf;
    }
}

