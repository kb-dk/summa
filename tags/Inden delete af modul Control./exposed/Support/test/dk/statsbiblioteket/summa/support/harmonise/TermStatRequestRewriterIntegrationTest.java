package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.support.summon.search.SummonSearchNode;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;

/**
 * Tests that the rewriter produces semantically quivalent queries when the term queries are not rewritten
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hsm")
public class TermStatRequestRewriterIntegrationTest extends TestCase {
    private static Log log = LogFactory.getLog(
        TermStatRequestRewriterIntegrationTest.class);

    private static final File SECRET = new File(
        System.getProperty("user.home") + "/summon-credentials.dat");
    private String id;
    private String key;

    private SummonSearchNode summon;
    private QueryRewriter requestRewriter;

    public static Test suite() {
        return new TestSuite(TermStatRequestRewriterIntegrationTest.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!SECRET.exists()) {
            throw new IllegalStateException(
                "The file '" + SECRET.getAbsolutePath() + "' must exist and "
                + "contain two lines, the first being access ID, the second"
                + "being access key for the Summon API");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(SECRET), "utf-8"));
        id = br.readLine();
        key = br.readLine();
        br.close();
        log.debug("Loaded credentials from " + SECRET);

        Configuration conf = Configuration.newMemoryBased(
                SummonSearchNode.CONF_SUMMON_ACCESSID, id,
                SummonSearchNode.CONF_SUMMON_ACCESSKEY, key
        );

        summon = new SummonSearchNode(conf);
        requestRewriter = new QueryRewriter(new QueryRewriter.Event() {
            @Override
            public Query onQuery(TermQuery query) {
                return query;
            }
        });
    }

    @Override
    public void tearDown() {
        summon = null;
        requestRewriter = null;
    }

    public void testRewrite1() throws RemoteException, ParseException {
        checkQuery("foo OR (bar AND baz)");
    }

    private void checkQuery(String query)
                                        throws RemoteException, ParseException {
        String rewritten = requestRewriter.rewrite(query);

        compareHits(search(query), search(rewritten));
    }

    private void compareHits(ResponseCollection rc1, ResponseCollection rc2) {
        assertEquals(countResults(rc1), countResults(rc2));
    }

    private int countResults(ResponseCollection responses) {
        for (Response response: responses) {
            if (response instanceof DocumentResponse) {
                return (int)((DocumentResponse)response).getHitCount();
            }
        }
        throw new IllegalArgumentException(
            "No documentResponse in ResponseCollection");
    }

    private ResponseCollection search(String query) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, query);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);

        summon.search(request, responses);

        return responses;
    }

    public void testRewrite2() throws RemoteException, ParseException {
        checkQuery("foo AND (bar AND baz)");
    }

    public void testRewrite3() throws RemoteException, ParseException {
        checkQuery("foo AND bar AND baz");
    }

    public void testRewrite4() throws RemoteException, ParseException {
        checkQuery("foo AND bar AND baz OR spam AND eggs OR ham");
    }

    public void testRewrite5() throws RemoteException, ParseException {
        checkQuery("foo AND +bar AND baz OR +(-spam) AND (eggs OR -ham)");
    }

    public void testRewrite6() throws RemoteException, ParseException {
        checkQuery("foo AND bar AND baz OR spam");
    }

    public void testRewrite7() throws RemoteException, ParseException {
        checkQuery("foo OR bar AND baz");
    }

    public void testRewrite8() throws RemoteException, ParseException {
        checkQuery("foo AND bar OR baz");
    }
}
