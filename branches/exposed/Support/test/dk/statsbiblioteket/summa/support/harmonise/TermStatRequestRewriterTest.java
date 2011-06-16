package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.search.TermQuery;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hsm")
public class TermStatRequestRewriterTest extends TestCase {

    public static Test suite() {
        return new TestSuite(TermStatRequestRewriterTest.class);
    }

    private TermStatRequestRewriter request;

    @Override
    public void setUp() {
        request = new TermStatRequestRewriter(new TermStatRequestRewriter.Event() {
            @Override
            public void onTermQuery(TermQuery query) {
            }
        });
    }

    @Override
    public void tearDown() {
        request = null;
    }

    public void testParenthesized1() {
        assertEquals("(foo OR (+bar +baz))", request.rewrite("foo OR (bar AND baz)"));
    }

    public void testParenthesized3() {
        assertEquals("(+foo +(+bar +baz))", request.rewrite("foo AND (bar AND baz)"));
    }

    public void testParenthesized4() {
        assertEquals("(+foo +bar +baz)", request.rewrite("foo AND bar AND baz"));
    }

    public void testParenthesized5() {
        assertEquals("(+foo +bar baz OR +spam eggs OR ham)", request.rewrite("foo AND bar AND baz OR spam AND eggs OR ham"));
    }

    public void testParenthesized6() {
        assertEquals("(+foo +bar baz OR +(-spam) +(eggs OR -ham))", request.rewrite("foo AND +bar AND baz OR +(-spam) AND (eggs OR -ham)"));
    }

    public void testBooleanToFlagged1() {
        assertEquals("(+foo +bar baz OR spam)", request.rewrite("foo AND bar AND baz OR spam"));
    }

    public void testBooleanToFlagged2() {
        assertEquals("(foo OR +bar +baz)", request.rewrite("foo OR bar AND baz"));
    }

     public void testBooleanToFlagged3() {
        assertEquals("(+foo bar OR baz)", request.rewrite("foo AND bar OR baz"));
    }
}
