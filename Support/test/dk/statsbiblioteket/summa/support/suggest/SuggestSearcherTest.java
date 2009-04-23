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
        return new SummaSearcherImpl(conf);
    }

    @Override
    public void tearDown() throws Exception {
        searcher.close();
    }

    public void testAddAndGet() throws Exception {
        put("Foo Fighters", 87);
        put("Foo Bars", 123);
        assertGet("Foo", "queryCount=\"1\">foo fighters");
        put("Foo Fighters", 87);
        assertGet("Foo", "queryCount=\"2\">foo fighters");
    }

    public void testAddWithQueryCount() throws Exception {
        put("Foo Fighters", 87);
        put("Foo Bars", 123);
        assertGet("Foo", "queryCount=\"1\">foo fighters");
        put("Foo Fighters", 87, 100);
        assertGet("Foo", "queryCount=\"100\">foo fighters");
    }

    public void testPersistence() throws Exception {
        put("Foo Fighters", 87);
        put("Foo Bars", 123);

        searcher.close();
        searcher = getSearcher();

        assertGet("Foo", "queryCount=\"1\">foo fighters");
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
            assertGet("Foo", "foo");
            getProfiler.beat();
            assertGet("B", "bar");
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
        System.out.println(xml);
        assertTrue(String.format("The string '%s' should be in the result '%s'",
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