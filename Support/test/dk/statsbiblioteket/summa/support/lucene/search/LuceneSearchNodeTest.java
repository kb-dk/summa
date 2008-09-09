package dk.statsbiblioteket.summa.support.lucene.search;

import java.io.File;
import java.util.BitSet;
import java.rmi.RemoteException;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexCommon;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.document.DocumentSearcher;
import dk.statsbiblioteket.summa.search.document.DocIDCollector;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class LuceneSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(LuceneSearchNodeTest.class);

    public LuceneSearchNodeTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        if (testRoot.exists()) {
            Files.delete(testRoot);
        }
        testRoot.mkdirs();
        new File(testRoot, "index");
        assertTrue("The sourceDir '" + sourceDir + "' should exist",
                   sourceDir.exists());
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (testRoot.exists()) {
            Files.delete(testRoot);
        }
    }

    public static Test suite() {
        return new TestSuite(LuceneSearchNodeTest.class);
    }

    private File testRoot = new File(
            System.getProperty("java.io.tmpdir"), "summaTestRoot");

    File sourceDir = new File(Resolver.getURL(
            "data/fagref/fagref_IndexDescriptor.xml").
            getFile()).getParentFile();
    File descLocation = new File(testRoot, "indexDescriptor.xml");

    /* Returns the location of the configuration file */
    private File basicSetup() throws Exception {
        Files.copy(new File(sourceDir, "fagref_IndexDescriptor.xml"),
                   descLocation, false);

        String configuration = Resolver.getUTF8Content(
                "dk/statsbiblioteket/summa/support/lucene/search/LuceneSearcherTest_conf.xml");
        configuration = configuration.replace(
                "/tmp/summatest/index",
                testRoot.getAbsolutePath());
        configuration = configuration.replace(
                "/tmp/summatest/data/fagref/fagref_IndexDescriptor.xml",
                descLocation.getAbsolutePath());
        File confLocation = new File(testRoot, "configuration.xml");
        Files.saveString(configuration, confLocation);

        assertNotNull("The configuration should be available", confLocation);
        return confLocation;
    }

/*    public void testStrings() throws Exception {
        Configuration conf = Configuration.load(basicSetup().toString());
        for (String s: conf.getStrings(SummaSearcher.CONF_RESULT_FIELDS)) {
            System.out.println(s);
        }
    }*/

    public void testBasicSearcher() throws Exception {
        makeIndex();
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        SummaSearcherImpl searcher = new SummaSearcherImpl(conf);
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "hans");
        log.debug("Search for 'hans' gave\n"
                  + searcher.search(request).toXML());
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
        System.out.println("Test-index at " + indexLocation);

        // Do this every time the index is updated
//        File indexLocation = new File("myhome/myindexfolder");
        IndexReader reader = IndexReader.open(indexLocation);
        BitSet deleted = new BitSet(reader.maxDoc());
        for (int i = 0 ; i < reader.maxDoc() ; i++) {
            if (reader.isDeleted(i)) {
                deleted.set(i);
            }
        }
        QueryParser parser =
                new QueryParser("freetext", new StandardAnalyzer());

        // Do this for every search
        Query query = parser.parse("java");
        QueryWrapperFilter filter = new QueryWrapperFilter(query);
        BitSet workset = filter.bits(reader);
        workset.or(deleted);
        // workset now marks all the docids that is either matching or deleted
        System.out.print("Non-matching documents: ");
        for (int i = 0 ; i < reader.maxDoc() ; i++) {
            if (!workset.get(i)) {
                System.out.print(i + " ");
            }
        }
    }

    public void testDisplayNonDeletedIDsV2() throws Exception {
        File indexLocation = makeIndex();
        System.out.println("Test-index at " + indexLocation);

        QueryParser parser =
                new QueryParser("freetext", new StandardAnalyzer());
        IndexReader reader = IndexReader.open(indexLocation);
        IndexSearcher searcher = new IndexSearcher(indexLocation.toString());

        Query query = parser.parse("java");
        BooleanQuery notQuery = new BooleanQuery();
        notQuery.add(query, BooleanClause.Occur.MUST_NOT);
        // Is a boolean with a single NOT clause valid?
        QueryWrapperFilter filter = new QueryWrapperFilter(notQuery);
        BitSet nonmatching = filter.bits(reader);
        System.out.println("Got " + nonmatching.cardinality() + " non-matches");

        // workset now marks all the docids that is either matching or deleted
        System.out.print("Non-matching documents: ");
        for (int i = 0 ; i < reader.maxDoc() ; i++) {
            if (nonmatching.get(i)) {
                System.out.print(i + " ");
            }
        }
    }

}
