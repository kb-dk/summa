package dk.statsbiblioteket.summa.support.suggest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.support.api.SuggestKeys;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Profiler;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

/**
 * Sets up a proper SummaSearcher with a Suggest inside.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class SuggestSearcherTest extends TestCase {
    private static Log log = LogFactory.getLog(SuggestSearcherTest.class);

    File storageRoot = new File(new File(System.getProperty("java.io.tmpdir")),
                                         "suggest_storage");
    SummaSearcher searcher;


    @Override
    public void setUp() throws Exception {
        if (storageRoot.exists()) {
            Files.delete(storageRoot);
        }
        searcher = getSearcher();
    }

    private SummaSearcher getSearcher() throws Exception {
        Configuration conf =
                Configuration.load("data/suggest/SuggestSearch.xml");
        conf.set(SummaSearcherImpl.CONF_STATIC_ROOT, storageRoot.toString());
        conf.set(SuggestSearchNode.CONF_LOWERCASE_QUERIES, false);
        return new SummaSearcherImpl(conf);
    }

    @Override
    public void tearDown() throws Exception {
        searcher.close();
    }

    public void testImport() throws Exception {
        String data = "importA\t10\t12\nimportB\t7\t8\n";
        Files.saveString(
                data, new File(storageRoot, SuggestStorage.IMPORT_FILE));
        Request request = new Request();
        request.put(SuggestSearchNode.SEARCH_IMPORT, true);
        searcher.search(request);
        assertGet("imp", "queryCount=\"12\">importA");
        assertGet("imp", "queryCount=\"8\">importB");
    }

    public void testExport() throws Exception {
        String data = "importA\t10\t12\nimportB\t7\t8\n";
        Files.saveString(
                data, new File(storageRoot, SuggestStorage.IMPORT_FILE));
        Request request = new Request();
        request.put(SuggestSearchNode.SEARCH_IMPORT, true);
        searcher.search(request);

        request = new Request();
        request.put(SuggestSearchNode.SEARCH_EXPORT, true);
        searcher.search(request);

        String dump = Files.loadString(new File(
                storageRoot, SuggestStorage.EXPORT_FILE));
        assertEquals("Import and export should match", data, dump);
    }

    public void testAddAndGet() throws Exception {
        put("Foo Fighters", 87);
        put("Foo Bars", 123);
        assertGet("Foo", "queryCount=\"1\">Foo Fighters");
        put("Foo Fighters", 87);
        assertGet("Foo", "queryCount=\"2\">Foo Fighters");
    }

    public void testCaseSensitiveness() throws Exception {
        put("Foo Fighters", 87);
        put("Foo Bars", 123);
        assertGet("Foo", "queryCount=\"1\">Foo Fighters");
        assertGet("foo", "queryCount=\"1\">Foo Fighters");
        put("Foo fighters", 87);
        assertGet("Foo", "queryCount=\"1\">Foo Fighters");
        assertGet("Foo", "queryCount=\"1\">Foo fighters");
    }

    public void testAddWithQueryCount() throws Exception {
        put("Foo Fighters", 87);
        put("Foo Bars", 123);
        assertGet("Foo", "queryCount=\"1\">Foo Fighters");
        put("Foo Fighters", 87, 100);
        assertGet("Foo", "queryCount=\"100\">Foo Fighters");
    }

    public void testPersistence() throws Exception {
        put("Foo Fighters", 87);
        put("Foo Bars", 123);

        searcher.close();
        searcher = getSearcher();

        assertGet("Foo", "queryCount=\"1\">Foo Fighters");
    }

    public void testPerformance() throws Exception {
        int SETTERS = 10000;
        int GETTERS = 10000;

        Profiler setProfiler = new Profiler(SETTERS);
        for (int i = 0 ; i < SETTERS ; i++) {
            put("Foo " + i % 3, i * 2);
            setProfiler.beat();
            put("Foo flam" + i % 3, i * 2);
            setProfiler.beat();
            put("Bar", 12);
            setProfiler.beat();
        }
        String updates =String.format(
                "Did %d updates at %s updates/sec",
                setProfiler.getBeats(), setProfiler.getBps(false));

        Profiler getProfiler = new Profiler(GETTERS);
        for (int i = 0 ; i < GETTERS ; i++) {
            assertGet("Foo", "Foo");
            getProfiler.beat();
            assertGet("B", "Bar");
            getProfiler.beat();
        }
        log.info(updates);
        log.info(String.format(
                "Did %d requests at %s requests/sec",
                getProfiler.getBeats(), getProfiler.getBps(false)));
    }

    /* Helpers */

    private void assertGet(String prefix, String expected) throws
                                                           IOException {
        String xml = get(prefix);
        assertTrue(String.format("The string '%s' should be in the result:\n%s",
                                 expected, xml),
                   xml.contains(expected));
        log.trace("Got xml for prefix '" + prefix + "':\n" + xml);
    }

    private String get(String prefix) throws IOException {
        Request request = new Request();
        request.put(SuggestKeys.SEARCH_PREFIX, prefix);
        ResponseCollection responses =  searcher.search(request);
        return responses.toXML();
    }


    private void put(String query, int hits) throws IOException {
        Request request = new Request();
        request.put(SuggestKeys.SEARCH_UPDATE_QUERY, query);
        request.put(SuggestKeys.SEARCH_UPDATE_HITCOUNT, hits);
        searcher.search(request);
    }

    private void put(String query, int hits, int queryCount) throws
                                                             IOException {
        Request request = new Request();
        request.put(SuggestKeys.SEARCH_UPDATE_QUERY, query);
        request.put(SuggestKeys.SEARCH_UPDATE_HITCOUNT, hits);
        request.put(SuggestKeys.SEARCH_UPDATE_QUERYCOUNT, queryCount);
        searcher.search(request);
    }
}