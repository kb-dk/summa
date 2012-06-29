package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.tools.QueryRewriter;
import dk.statsbiblioteket.summa.support.summon.search.SummonSearchNode;
import dk.statsbiblioteket.summa.support.summon.search.SummonTestHelper;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.rmi.RemoteException;

/**
 * Tests that the rewriter produces semantically equivalent queries when the term queries are not rewritten
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hsm")
public class QueryRewriterIntegrationTest extends TestCase {
    private static Log log = LogFactory.getLog(QueryRewriterIntegrationTest.class);

    private SummonSearchNode summon;
    private QueryRewriter requestRewriter;

    public static Test suite() {
        return new TestSuite(QueryRewriterIntegrationTest.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        summon = SummonTestHelper.createSummonSearchNode();
        requestRewriter = new QueryRewriter(
            Configuration.newMemoryBased(QueryRewriter.CONF_TERSE, true),
            null,
            new QueryRewriter.Event() {
                @Override
                public Query onQuery(TermQuery query) {
                    return query;
                }
            });
    }

    @Override
    public void tearDown() {
        try {
            summon.close();
        } catch (RemoteException e) {
            log.warn("Exception while closing SummonSearchNode", e);
        }
        summon = null;
        requestRewriter = null;
    }

    public void testRewrite1() throws RemoteException, ParseException {
        checkQuery("foo OR (bar AND baz)");
    }


    private void checkQuery(String query)
            throws RemoteException, ParseException {
        String rewritten = requestRewriter.rewrite(query);
        HarmoniseTestHelper.compareHits(query + "' and '" + rewritten, search(query), search(rewritten));
    }

    // Avoids query rewriting inside SummonSearchNode
    private void checkQueryDirect(String query)
            throws RemoteException, ParseException {
        String rewritten = requestRewriter.rewrite(query);
        HarmoniseTestHelper.compareHits(query + "' and '" + rewritten, searchDirect(query), searchDirect(rewritten));
    }

    private ResponseCollection search(String query) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, query);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);

        summon.search(request, responses);

        return responses;
    }

    private ResponseCollection searchDirect(String query) throws RemoteException {
        ResponseCollection responses = new ResponseCollection();
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, query);
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        request.put(SummonSearchNode.SEARCH_PASSTHROUGH_QUERY, true);

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
        //  Summon no longer adheres to Lucene's way of parsing boolean queries.
        //
        // Details: http://wiki.apache.org/lucene-java/BooleanQuerySyntax
        // https://issues.apache.org/jira/browse/LUCENE-1823
        // https://issues.apache.org/jira/browse/LUCENE-167
        //
        // That means query does not return the same number of hits
        String query = "foo AND bar AND baz OR spam AND eggs OR ham";
        assertFalse(HarmoniseTestHelper.countResults(search(query)) ==
                    HarmoniseTestHelper.countResults(search(requestRewriter.rewrite(query))));
    }

    public void testRewrite4Alternative() throws RemoteException, ParseException {
        // This is what Summon actually does (for the moment). AND has higher preference than OR and both are
        // left associative. This is the common way to handle logical operators.
        String query = "foo AND bar AND baz OR spam AND eggs OR ham";
        requestRewriter.rewrite(query);
        String andBindsHarder = "((foo AND bar) AND baz) OR (spam AND eggs) OR ham";

        HarmoniseTestHelper.compareHits(query, search(query), search(andBindsHarder));
        HarmoniseTestHelper.compareHits(query, search(query), search(requestRewriter.rewrite(andBindsHarder)));
    }

    public void testRewrite5() throws RemoteException, ParseException {
        String query = "foo AND +bar AND baz OR +(-spam) AND (eggs OR -ham)";
        assertFalse(HarmoniseTestHelper.countResults(search(query)) ==
                    HarmoniseTestHelper.countResults(search(requestRewriter.rewrite(query))));
    }

    public void testRewrite6() throws RemoteException, ParseException {
        checkQuery("foo AND bar AND baz OR spam");
    }

    public void testRewrite7() throws RemoteException, ParseException {
        String query = "foo OR bar AND baz";
        assertFalse(HarmoniseTestHelper.countResults(search(query)) ==
                    HarmoniseTestHelper.countResults(search(requestRewriter.rewrite(query))));
    }

    public void testRewrite7Alternative() throws RemoteException, ParseException {
        String query = "foo OR bar AND baz";
        String rewritten = "(foo OR (+bar +baz))";

        HarmoniseTestHelper.compareHits(query + "' and '" + rewritten, search(query), search(rewritten));
    }

    public void testRewrite8() throws RemoteException, ParseException {
        checkQuery("foo AND bar OR baz");
    }

/*    public void testRewriteBZ2416() throws RemoteException, ParseException {
        checkQuery("Ungdomsuddannelse - Sociale Publikationer");
    }*/

    public void testRewriteWithField() throws RemoteException, ParseException {
        checkQuery("chaos -recordBase:sb*");
    }

    public void testRewriteDivider() throws RemoteException, ParseException {
        checkQuery("foo - bar");
    }

    public void testRewriteTitleMatch() throws RemoteException, ParseException {
        checkQuery("miller genre as social action");
    }

    public void testRewriteTitleMatchDirect() throws RemoteException, ParseException {
        checkQueryDirect("miller genre as social action");
    }

    /*
     * This tests whether the exact same query yields different results. It seems that Serial Solutions performs
     * updates around noon, Danish time, that results in fluxing search results for about an hour.
     */
    public void testSameSearch() throws RemoteException, ParseException {
        final String query = "Learning and child development: a cultural-historical study";
//        final String query = "miller genre as social action";
        final int RUNS = 5;
        for (int i = 0 ; i < RUNS ; i++) {
            log.debug("Issuing same query '" + query + ": " + (i+1) + "/" + RUNS);
            HarmoniseTestHelper.compareHits(query, searchDirect(query), searchDirect(query));
        }
    }

    public void testRewriteTitleMatchWeightDirect() throws RemoteException, ParseException {
        checkQueryDirect("miller genre^2 as social action");
    }

    public void testColonSpace() throws RemoteException, ParseException {
        checkQuery("foo: bar");
    }

    // This fails as the DisMax treats the direct query as "+foo +bar +baz", which it does not support.
    // The rewritten query is reduced to "foo bar baz".
//    public void testRewriteExplicitAndDirect() throws RemoteException, ParseException {
//        checkQueryDirect("foo AND bar AND baz");
//    }

    public void testApostrophes() throws RemoteException, ParseException {
        String simple = "miller genre as social action";
        String apostrophed = "\"miller\" \"genre\" \"as\" \"social\" \"action\"";
        HarmoniseTestHelper.compareHits(
            simple + " and " + apostrophed, searchDirect(simple), searchDirect(apostrophed));
    }

    public void testEscaping() throws RemoteException {
        String raw =     "Learning and child development: a cultural-historical study";
        String escaped = "Learning and child development\\: a cultural-historical study";
        String quoted =  "Learning and child \"development:\" a cultural-historical study";
        HarmoniseTestHelper.compareHits(escaped, true, searchDirect(escaped), searchDirect(quoted));
        HarmoniseTestHelper.compareHits(raw, false, searchDirect(raw), searchDirect(quoted));
    }

    public void testSpecificTitle() throws RemoteException, ParseException {
        checkQuery("towards a cooperative experimental system development approach");
        checkQueryDirect("towards a cooperative experimental system development approach");
    }

    public void testApostrophesExplicitAnd() throws RemoteException, ParseException {
        String simple = "miller genre as social action";
         // Solr DisMax query parser does not like this
        String apostrophed = "+\"miller\" +\"genre\" +\"as\" +\"social\" +\"action\"";
        HarmoniseTestHelper.compareHits(simple, false, searchDirect(simple), searchDirect(apostrophed));
    }

    public void testExplicitMust() throws RemoteException, ParseException {
        String simple = "miller genre as social action";
        String must = "+miller +genre +as +social +action"; // Solr DisMax query parser does not like this
        HarmoniseTestHelper.compareHits(simple, false, searchDirect(simple), searchDirect(must));
    }

    public void testRewriteWeightsDirect() throws RemoteException, ParseException {
        String query = "foo^2 bar^3";
        checkQueryDirect(query);
    }

}
