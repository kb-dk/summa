package dk.statsbiblioteket.summa.support.suggest;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.support.api.SuggestKeys;
import dk.statsbiblioteket.util.Files;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.rmi.RemoteException;

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
        assertGet(node, "Foo", "queryCount=\"1\">foo fighters");
        put(node, "Foo Fighters", 87);
        assertGet(node, "Foo", "queryCount=\"2\">foo fighters");
        node.close();
    }

    public void testAddWithQueryCount() throws Exception {
        SuggestSearchNode node = new SuggestSearchNode(
                Configuration.newMemoryBased());
        node.open(storageRoot.toString());
        put(node, "Foo Fighters", 87);
        put(node, "Foo Bars", 123);
        assertGet(node, "Foo", "queryCount=\"1\">foo fighters");
        put(node, "Foo Fighters", 87, 100);
        assertGet(node, "Foo", "queryCount=\"100\">foo fighters");
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
        assertGet(node2, "Foo", "queryCount=\"1\">foo fighters");
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
        assertTrue(String.format("The string '%s' should be in the result '%s'",
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
