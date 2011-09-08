package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hsm")
public class TermStatRequestRewriterTest extends TestCase {

    public static Test suite() {
        return new TestSuite(TermStatRequestRewriterTest.class);
    }

    private QueryRewriter request;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        request = new QueryRewriter(new QueryRewriter.Event() {
            @Override
            public Query onQuery(TermQuery query) {
                return query;
            }
        });
    }

    @Override
    public void tearDown() {
        request = null;
    }

    public void testParenthesized1() throws ParseException {
        assertEquals(
            "(foo OR (+bar +baz))", request.rewrite(
            "foo OR (bar AND baz)"));
    }

    public void testParenthesized3() throws ParseException {
        assertEquals(
            "(+foo +(+bar +baz))", request.rewrite(
            "foo AND (bar AND baz)"));
    }

    public void testParenthesized4() throws ParseException {
        assertEquals(
            "(+foo +bar +baz)", request.rewrite(
            "foo AND bar AND baz"));
    }

    public void testParenthesized5() throws ParseException {
        assertEquals(
            "(+foo +bar baz OR +spam eggs OR ham)", request.rewrite(
            "foo AND bar AND baz OR spam AND eggs OR ham"));
    }

    public void testParenthesized6() throws ParseException {
        assertEquals(
            "(+foo +bar baz OR +(-spam) +(eggs OR -ham))", request.rewrite(
            "foo AND +bar AND baz OR +(-spam) AND (eggs OR -ham)"));
    }

    public void testBooleanToFlagged1() throws ParseException {
        assertEquals(
            "(+foo +bar baz OR spam)", request.rewrite(
            "foo AND bar AND baz OR spam"));
    }

    public void testBooleanToFlagged2() throws ParseException {
        assertEquals(
            "(foo OR +bar +baz)", request.rewrite(
            "foo OR bar AND baz"));
    }

     public void testBooleanToFlagged3() throws ParseException {
        assertEquals(
            "(+foo bar OR baz)", request.rewrite(
            "foo AND bar OR baz"));
    }

    public void testPhrase() throws ParseException {
        assertEquals(
            "(\"foo bar\" OR \"zoo AND baz\")", request.rewrite(
            "\"foo bar\" OR \"zoo AND baz\""));
    }

    public void testColon() throws ParseException {
        assertEquals(
            "foo:\"bar:zoo\"", request.rewrite(
            "foo:\"bar:zoo\""));
        assertEquals(
            "foo:\"bar:zoo:baz\"", request.rewrite(
            "foo:\"bar:zoo:baz\""));
    }

}
