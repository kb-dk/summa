package dk.statsbiblioteket.summa.search;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.index.IndexException;
import dk.statsbiblioteket.summa.common.index.IndexCommon;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class LuceneSearcherTest extends TestCase {
    private static Log log = LogFactory.getLog(LuceneSearcherTest.class);

    public LuceneSearcherTest(String name) {
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
//            Files.delete(testRoot); 
        }
    }

    public static Test suite() {
        return new TestSuite(LuceneSearcherTest.class);
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
                "dk/statsbiblioteket/summa/search/LuceneSearcherTest_conf.xml");
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
        LuceneSearcher searcher = new LuceneSearcher(conf);
        log.debug("Search for 'hans' gave\n"
                  + searcher.fullSearch(null, "hans", 0, 10,
                                        null, false, null, null));
    }

    public void testDiscovery() throws Exception {
        Configuration conf = Configuration.load(basicSetup().getAbsolutePath());
        LuceneSearcher searcher = new LuceneSearcher(conf);
        try {
            searcher.fullSearch(null, "hans", 0, 10, null, false, null, null);
            fail("An IndexException should be throws as not index data are"
                 + " present yet");
        } catch (IndexException e) {
            // Expected
        }

        makeIndex();
        Thread.sleep(2000); // 2 * Min retention time

        assertNotNull("Search should work now",
                      searcher.fullSearch(null, "hans", 0, 10, null, false,
                                          null, null));
    }

    private void makeIndex() throws Exception {
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
    }
}
