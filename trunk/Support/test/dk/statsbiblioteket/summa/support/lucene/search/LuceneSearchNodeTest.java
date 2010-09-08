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
package dk.statsbiblioteket.summa.support.lucene.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexCommon;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.support.api.LuceneKeys;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.rmi.RemoteException;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class LuceneSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(LuceneSearchNodeTest.class);

    private static final String testRootPath =
                                             "Support/target/tmp/summaTestRoot";
    private static final String indexDescriptor = "indexDescriptor.xml";
    private static final String fagref_indexDescriptor =
                                       "data/fagref/fagref_IndexDescriptor.xml";
    private static final String configurationFile = "configuration.xml";
    private static final String luceneSearcherConf =
                                                  "LuceneSearcherTest_conf.xml";
    private static final String sourceDirPath = "data/fagref";

    private File testRoot;
    private File sourceDir;
    private File descLocation;

    public LuceneSearchNodeTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testRoot = new File(testRootPath);
        if (testRoot.exists()) {
            Files.delete(testRoot);
        }
        if(!testRoot.mkdirs()) {
            fail("Test rook could not be created");
        }

        sourceDir = new File(Thread.currentThread().getContextClassLoader()
                                         .getResource(sourceDirPath).getFile());
        descLocation = new File(testRoot, indexDescriptor );

        assertTrue("The sourceDir '" + sourceDir + "' should exist",
                   sourceDir.exists());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(LuceneSearchNodeTest.class);
    }

    /* Returns the location of the configuration file */
    private File basicSetup() throws Exception {
        Files.copy(new File(sourceDir, fagref_indexDescriptor),
                   descLocation, false);

        String configuration = Resolver.getUTF8Content(luceneSearcherConf);
        /*configuration = configuration.replace(
                "/tmp/summatest/index",
                testRoot.getAbsolutePath());
        configuration = configuration.replace(
                "/tmp/summatest/data/fagref/fagref_IndexDescriptor.xml",
                descLocation.getAbsolutePath());*/
        File confLocation = new File(testRoot, configurationFile);
        Files.saveString(configuration, confLocation);

        assertNotNull("The configuration should be available", confLocation);

        return confLocation;
    }

/*    public void testStrings() throws Exception {
        Configuration conf = Configuration.load(basicSetup().toString());
        for (String s: conf.getStrings(SummaSearcher.CONF_RESULT_FIELDS)) {
            log.info(s);
        }
    }*/

    public void testBasicSearcher() throws Exception {
        makeIndex();
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "hans");
        String result = searcher.search(request).toXML();
        log.debug("Search with query for 'hans' gave\n" + result);
        assertEquals("We should get the right number of hits",
                     1, getHitCount(result));
        searcher.close();
    }

    public void testMatchAll() throws Exception {
        makeIndex();
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "*");
        String result = searcher.search(request).toXML();
        log.debug("Search with query for '*' gave\n" + result);
        assertEquals("We should get the right number of hits",
                     3, getHitCount(result));
        searcher.close();
    }

    Pattern HITS = Pattern.compile(".*hitCount.\"([0-9]+)\".*", Pattern.DOTALL);
    private int getHitCount(String result) {
        Matcher matcher = HITS.matcher(result);
        assertTrue("We should be able to find hitCount in\n" + result,
                   matcher.matches());
        return Integer.parseInt(matcher.group(1));
    }

    public void testFilterSearcher() throws Exception {
        makeIndex();
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_FILTER, "hans");
        String result = searcher.search(request).toXML();
        log.debug("Search with filter for 'hans' gave\n" + result);
        assertEquals("We should get the right number of hits",
                     1, getHitCount(result));
        searcher.close();
    }

    public void testDocIDCollector() throws Exception {
        makeIndex();
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        conf.set(DocumentSearcher.CONF_COLLECT_DOCIDS, true);
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "hans");
        ResponseCollection responses = searcher.search(request);
        log.debug("Search for 'hans' gave\n" + responses.toXML());
        // TODO: Inject fake searcher that tests for docID count
//        DocIDCollector collector = (DocIDCollector)
//                responses.getTransient().get(DocumentSearcher.DOCIDS);
//        assertEquals("The right number of docIDs should be collected",
//                     1, collector.getDocCount());
        searcher.close();
    }

    public void testMoreLikeThis() throws Exception {
        makeIndex();
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        conf.set(DocumentSearcher.CONF_COLLECT_DOCIDS, true);
        Configuration moreConf = conf.createSubConfiguration(
                LuceneSearchNode.CONF_MORELIKETHIS_CONF);
        moreConf.set(
                LuceneSearchNode.CONF_MORELIKETHIS_MAXNUMTOKENSPARSED, 10000);
        moreConf.set(LuceneSearchNode.CONF_MORELIKETHIS_MAXQUERYTERMS, 10000);
        moreConf.set(LuceneSearchNode.CONF_MORELIKETHIS_MAXWORDLENGTH, 10000);
        moreConf.set(LuceneSearchNode.CONF_MORELIKETHIS_MINDOCFREQ, 0);
        moreConf.set(LuceneSearchNode.CONF_MORELIKETHIS_MINTERMFREQ, 0);
        moreConf.set(LuceneSearchNode.CONF_MORELIKETHIS_MINWORDLENGTH, 0);

        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Request request = new Request();
        request.put(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID,
                    "fagref:jh@example.com");
        ResponseCollection responses = searcher.search(request);
        assertTrue("MoreLikeThis should return something else than the input",
                   responses.toXML().contains("fagref:gm@example.com"));
        assertFalse("MoreLikeThis should not return the input",
                    responses.toXML().contains(
                            "<field name=\"recordID\">fagref:jh@example.com"
                            + "</field>"));
        log.debug("Search for 'hans' gave\n" + responses.toXML());

        request = new Request();
        request.put(LuceneKeys.SEARCH_MORELIKETHIS_RECORDID,
                    "nonexisting");
        try {
            searcher.search(request);
            fail("Performinf MoreLikeThis on nonexisting recordID should fail");
        } catch (RemoteException e) {
            log.debug("Got exception as expected in MoreLikeThis", e);
        }
    }

    public void testDumpSearch() throws Exception {
        makeIndex();
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        conf.set(DocumentSearcher.CONF_COLLECT_DOCIDS, true);
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "hans");
        ResponseCollection responses = searcher.search(request);
        log.debug("Search for 'hans' gave\n" + responses.toXML());

        request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY,
                    "recordID:\"fagref:hj@example.com\"");
        responses = searcher.search(request);
        log.debug("Search for recordID gave\n" + responses.toXML());
    }

    public void testDiscovery() throws Exception {
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "hans");
        try {
            searcher.search(request).toXML();
            fail("An IndexException should be thrown as no index data are"
                 + " present yet");
        } catch (RemoteException e) {
            // Expected
        }
        makeIndex();
        Thread.sleep(2000); // 2 * Min retention time
        // Search should work now
        searcher.search(request);
        searcher.close();
    }

    public void testNonEscape() throws Exception {
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "hans");
        makeIndex();
        Thread.sleep(2000); // 2 * Min retention time
        // Search should work now
        ResponseCollection response = searcher.search(request);
        searcher.close();
        log.info("Got response\n" + response.toXML());
        // TODO assert
    }

    private File makeIndex() throws Exception {
        File index = new File(testRoot, IndexCommon.getTimestamp());
        File sourceIndex = new File(sourceDir, "index");
        assertTrue("The source index '" + sourceIndex + "' should exist",
                   sourceIndex.exists());

        File lucene = new File(index, LuceneIndexUtils.LUCENE_FOLDER);
        Files.copy(sourceIndex, lucene, true);

        assertTrue("The index-folder '" + index + "' should exist",
                   index.exists());
        Files.saveString(Long.toString(System.currentTimeMillis()),
                         new File(index, IndexCommon.VERSION_FILE));
        return lucene;
    }

    public void testDisplayNonDeletedIDs() throws Exception {
        File indexLocation = makeIndex();
        log.info("Test-index at " + indexLocation);

        // Do this every time the index is updated
//        File indexLocation = new File("myhome/myindexfolder");
        IndexReader reader =
                            IndexReader.open(new NIOFSDirectory(indexLocation));
        BitSet deleted = new BitSet(reader.maxDoc());
        for (int i = 0 ; i < reader.maxDoc() ; i++) {
            if (reader.isDeleted(i)) {
                deleted.set(i);
            }
        }
        QueryParser parser =
                new QueryParser(Version.LUCENE_30, "freetext",
                                     new StandardAnalyzer(Version.LUCENE_30));

        // Do this for every search
        Query query = parser.parse("java");
        QueryWrapperFilter filter = new QueryWrapperFilter(query);
        DocIdSet workset = filter.getDocIdSet(reader);
        assertNotNull(workset);
        //workset.or(deleted);
        // workset now marks all the docids that is either matching or deleted
        log.info("Non-matching documents: ");

        for (int i = 0 ; i < reader.maxDoc() ; i++) {
            // TODO shouldn't this testcase test anything?
            /*if (!workset.get(i)) {
                log.info(i + " ");
            } */
        }
        // TODO assert
    }

    public void testDisplayNonDeletedIDsV2() throws Exception {
        File indexLocation = makeIndex();
        log.info("Test-index at " + indexLocation);

        QueryParser parser =
                new QueryParser(Version.LUCENE_30, "freetext",
                                     new StandardAnalyzer(Version.LUCENE_30));
        IndexReader reader = IndexReader.open(new NIOFSDirectory(indexLocation));
        new IndexSearcher(new NIOFSDirectory(indexLocation));

        Query query = parser.parse("java");
        BooleanQuery notQuery = new BooleanQuery();
        notQuery.add(query, BooleanClause.Occur.MUST_NOT);
        // Is a boolean with a single NOT clause valid?
        QueryWrapperFilter filter = new QueryWrapperFilter(notQuery);
        DocIdSet nonmatching = filter.getDocIdSet(reader);
        log.info("Got '" + nonmatching.toString() + "' non-matches");

        // workset now marks all the docids that is either matching or deleted
        // TODO shouldn't this testcase test anything?
        log.info("Non-matching documents: ");
        for (int i = 0 ; i < reader.maxDoc() ; i++) {
            /*if (nonmatching.get(i)) {
                log.info(i + " ");
            } */
        }
    }
}