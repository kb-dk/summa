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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.facetbrowser.FacetSearchNode;
import dk.statsbiblioteket.summa.facetbrowser.api.FacetKeys;
import dk.statsbiblioteket.summa.index.IndexControllerImpl;
import dk.statsbiblioteket.summa.index.lucene.LuceneManipulator;
import dk.statsbiblioteket.summa.index.lucene.StreamingDocumentCreator;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.resource.metadata.impl.ConfigurationGroup_impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.PrivateKey;
import java.util.List;

/**
 * Tests Exposed collator key concatenated fields.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ConcatTest extends TestCase {
    private static Log log = LogFactory.getLog(ConcatTest.class);

    public static final File INDEX_DESCRIPTOR = Resolver.getFile("search/concat/concat_IndexDescriptor.xml");
    public static final File INDEX_LOCATION = new File("concat_index_tmp");

    public static final String ONE = "search/concat/documents/one_concat.xml";
    public static final String TWO = "search/concat/documents/two_concat.xml";

    public ConcatTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        INDEX_LOCATION.mkdirs();
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

    public void testConcatFaceting() throws IOException {
        createIndex(ONE);
        ResponseCollection responses = search(new Request(
                DocumentKeys.SEARCH_QUERY, "id:one",
                DocumentKeys.SEARCH_COLLECT_DOCIDS, true,
                FacetKeys.SEARCH_FACET_FACETS, "concat"
        ));
        assertTrue("The responses should contain a tag with ål\n" + responses.toXML(),
                responses.toXML().contains("<tag name=\"concat ål\" addedobjects=\"1\" reliability=\"PRECISE\">"));
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

