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
    public static final String IMPORT_FILE = "suggest_in.dat";
    public static final String EXPORT_FILE = "suggest_out.dat";
    private static Log log = LogFactory.getLog(SuggestSearcherTest.class);

    File storageRoot = new File("test/tmp/", "suggest_storage");
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
        conf.set(SuggestSearchNode.CONF_NORMALIZE_QUERIES, false);
        return new SummaSearcherImpl(conf);
    }

    @Override
    public void tearDown() throws Exception {
        searcher.close();
    }

    public void testImport() throws Exception {
        String data = "importA\t10\t12\nimportB\t7\t8\n";
        File dump = new File(storageRoot, IMPORT_FILE);
        Files.saveString(data, dump);
        Request request = new Request();
        request.put(SuggestSearchNode.SEARCH_IMPORT, dump.toURI().toURL());
        searcher.search(request);
        assertGet("imp", "queryCount=\"12\">importA");
        assertGet("imp", "queryCount=\"8\">importB");
    }

    public void testExport() throws Exception {
        String data = "importA\t10\t12\nimportB\t7\t8\n";
        File dump = new File(storageRoot, IMPORT_FILE);
        Files.saveString(data, dump);
        Request request = new Request();
        request.put(SuggestSearchNode.SEARCH_IMPORT, dump.toURI().toString());
        searcher.search(request);

        request = new Request();
        File export = new File(storageRoot, EXPORT_FILE);
        request.put(SuggestSearchNode.SEARCH_EXPORT, export);
        searcher.search(request);

        String exportDump = Files.loadString(export);
        assertEquals("Import and export should match", data, exportDump);
    }

    public void testAddAndGet() throws Exception {
        put("Foo Fighters", 87);
        put("Foo Bars", 123);
        assertGet("Foo", "queryCount=\"1\">Foo Fighters");
        put("Foo Fighters", 87);
        assertGet("Foo", "queryCount=\"2\">Foo Fighters");
    }

    public void testCaseSensitivity() throws Exception {
        put("Foo Fighters", 87);
        put("Foo Bars", 123);
        assertGet("Foo", "queryCount=\"1\">Foo Fighters");
        assertGet("foo", "queryCount=\"1\">Foo Fighters");
        put("Foo fighters", 87, 2); // no query count, no update of  query count
        assertGet("Foo", "queryCount=\"1\">Foo Fighters");
        assertGet("Foo", "queryCount=\"2\">Foo fighters");
        //System.out.println(get("fo"));
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

    public void testOrdering() throws Exception {
        put("XOne", 87, 1);
        put("XTwo", 123, 2);
        put("XThree", 2, 3);
        String xml = get("X");
        assertTrue(xml.contains("XOne"));
        xml = eat(xml, "XThree");
        assertFalse(xml.contains("XTree"));
        xml = eat(xml, "XTwo");
        assertFalse(xml.contains("XTwo"));
        xml = eat(xml, "XOne");
        assertFalse(xml.contains("XOne"));

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

    /**
     * Eats everything out of food, up until, and including, bite.
     * Returns the remainer of food.
     * <p/>
     * The primary purpose of this method is to check for a certain sorted
     * subset of strings within food, in some specific order.
     *
     * @param food The food.
     * @param bite The bite.
     * @return the rest of the food.
     */
    public String eat(String food, String bite) {
        int i = food.indexOf(bite);
        if (i == -1) {
            fail("'" + bite + "' not found in :\n" + food);
        }

        return food.substring(i + bite.length());
    }
}
