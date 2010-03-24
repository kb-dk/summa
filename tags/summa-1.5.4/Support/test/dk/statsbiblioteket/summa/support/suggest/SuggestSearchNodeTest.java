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
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.support.api.SuggestKeys;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.rmi.RemoteException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class SuggestSearchNodeTest extends TestCase {
    private static Log log = LogFactory.getLog(SuggestSearchNodeTest.class);

    File storageRoot = new File(new File(System.getProperty("java.io.tmpdir")),
                                         "suggest_storage");


    @Override
    public void setUp() throws Exception {
        if (storageRoot.exists()) {
            Files.delete(storageRoot);
        }
    }

    @Override
    public void tearDown() throws Exception {

    }

    public void testEmptyConfigInstantiation() throws Exception {
        new SuggestSearchNode(Configuration.newMemoryBased()).close();
    }

    public void testOpen() throws Exception {
        SuggestSearchNode node = new SuggestSearchNode(
                Configuration.newMemoryBased());
        node.open(storageRoot.toString());
        File storageFolder = new File(storageRoot,
                                     SuggestSearchNode.SUGGEST_FOLDER);
        assertTrue(String.format(
                "The storage folder '%s' should exist", storageFolder),
                   storageFolder.exists());
        node.close();
    }

    public void testAddAndGet() throws Exception {
        SuggestSearchNode node = new SuggestSearchNode(
                Configuration.newMemoryBased());
        node.open(storageRoot.toString());
        put(node, "Foo Fighters", 87);
        put(node, "Foo Bars", 123);
        assertGet(node, "Foo", "queryCount=\"1\">Foo Fighters");
        put(node, "foo fighters", 87, 2);
        assertGet(node, "Foo", "queryCount=\"2\">foo fighters");
        assertGet(node, "Foo", "queryCount=\"1\">Foo Fighters");
        node.close();
    }

    public void testList() throws Exception {
        SuggestSearchNode node = new SuggestSearchNode(
                Configuration.newMemoryBased());
        node.open(storageRoot.toString());
        put(node, "Foo Fighters", 87, 12);
        put(node, "Foo Bars", 123, 13);
        for (int i = 0 ; i < 10 ; i++) {
            put(node, "Counter_" + i, 1000 + i*2, 20 + i*3);
        }
        List<String> queries = node.listSuggestions(0, Integer.MAX_VALUE);
        assertEquals("The # of extracted suggestions should match inserted",
                   12, queries.size());

        queries = node.listSuggestions(2, 5);
        assertEquals("The # of extracted suggestions should match requested",
                   5, queries.size());
        log.debug("Received queries:\n" + Strings.join(queries, "\n"));
        node.close();
    }

    public void testAddSuggestions() throws Exception {
        SuggestSearchNode node = new SuggestSearchNode(
                Configuration.newMemoryBased());
        node.open(storageRoot.toString());
        ArrayList<String> suggestions = new ArrayList<String>(Arrays.asList(
                "foo\t10\t2", "bar\t7", "zoo"
        ));
        node.addSuggestions(suggestions);
        assertGet(node, "Foo", "hits=\"10\" queryCount=\"2\">foo");
        assertGet(node, "bar", "hits=\"7\" queryCount=\"1\">bar");
        assertGet(node, "zoo", "hits=\"1\" queryCount=\"1\">zoo");
        node.close();
    }

    public void testAddWithQueryCount() throws Exception {
        SuggestSearchNode node = new SuggestSearchNode(
                Configuration.newMemoryBased(
                        SuggestSearchNode.CONF_NORMALIZE_QUERIES, false));
        node.open(storageRoot.toString());
        put(node, "Foo Fighters", 87);
        put(node, "Foo Bars", 123);
        assertGet(node, "Foo", "queryCount=\"1\">Foo Fighters");
        put(node, "Foo Fighters", 87, 100);
        assertGet(node, "Foo", "queryCount=\"100\">Foo Fighters");
        node.close();
    }

    public void testLowercase() throws Exception {
        SuggestSearchNode node = new SuggestSearchNode(
                Configuration.newMemoryBased(
                        SuggestSearchNode.CONF_NORMALIZE_QUERIES, true));
        node.open(storageRoot.toString());
        put(node, "Foo Fighters", 87, 12);
        put(node, "Foo fighters", 123);
        assertGet(node, "Foo", "queryCount=\"13\">foo fighters");
        node.close();
    }

    public void testPersistence() throws Exception {
        SuggestSearchNode node = new SuggestSearchNode(
                Configuration.newMemoryBased());
        node.open(storageRoot.toString());
        put(node, "Foo Fighters", 87);
        put(node, "Foo Bars", 123);
        node.close();

        SuggestSearchNode node2 = new SuggestSearchNode(
                Configuration.newMemoryBased());
        node2.open(storageRoot.toString());
        assertGet(node2, "Foo", "hits=\"87\" queryCount=\"1\">Foo Fighters");
        assertGet(node2, "Foo", "hits=\"123\" queryCount=\"1\">Foo Bars");
        node2.close();
    }

    /* Helpers */

    private void assertGet(SuggestSearchNode node, String prefix,
                           String expected) throws RemoteException {
        Request request = new Request();
        request.put(SuggestKeys.SEARCH_PREFIX, prefix);
        ResponseCollection responses = new ResponseCollection();

        node.search(request, responses);
        String xml = responses.toXML();
        assertTrue(String.format("The string '%s' should be in the result:\n%s",
                                 expected, xml),
                   xml.contains(expected));
        log.debug("Got xml for prefix '" + prefix + "':\n" + xml);
    }

    private void put(SuggestSearchNode node, String query, int hits)
                                                        throws RemoteException {
        Request request = new Request();
        request.put(SuggestKeys.SEARCH_UPDATE_QUERY, query);
        request.put(SuggestKeys.SEARCH_UPDATE_HITCOUNT, hits);
        node.search(request, new ResponseCollection());
    }

    private void put(SuggestSearchNode node, String query, int hits,
                     int queryCount) throws RemoteException {
        Request request = new Request();
        request.put(SuggestKeys.SEARCH_UPDATE_QUERY, query);
        request.put(SuggestKeys.SEARCH_UPDATE_HITCOUNT, hits);
        request.put(SuggestKeys.SEARCH_UPDATE_QUERYCOUNT, queryCount);
        node.search(request, new ResponseCollection());
    }
}

