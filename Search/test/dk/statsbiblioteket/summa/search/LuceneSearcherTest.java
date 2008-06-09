package dk.statsbiblioteket.summa.search;

import java.io.File;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;

public class LuceneSearcherTest extends TestCase {
    public LuceneSearcherTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(LuceneSearcherTest.class);
    }



    public void testBasicSearcher() throws Exception {
        URL confLocation = Resolver.getURL(
                "dk/statsbiblioteket/summa/search/LuceneSearcherTest_conf.xml");
        assertNotNull("The configuration should be available", confLocation);
        Configuration conf = Configuration.load(confLocation.getFile());
        LuceneSearcher searcher = new LuceneSearcher(conf);
    }
}
