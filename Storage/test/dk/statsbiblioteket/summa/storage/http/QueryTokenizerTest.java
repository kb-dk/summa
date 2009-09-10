package dk.statsbiblioteket.summa.storage.http;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.util.NoSuchElementException;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.storage.http.QueryTokenizerTest
 *
 * @author mke
 * @since Sep 10, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class QueryTokenizerTest extends TestCase {

    QueryTokenizer toks;

    public void setUp() {

    }

    public void tearDown() {

    }

    public void testEmpty() {
        toks = new QueryTokenizer("");
        assertFalse(toks.hasNext());
    }

    public void testSingleToken() {
        toks = new QueryTokenizer("foo=bar");
        assertTrue(toks.hasNext());
        assertEquals("foo=bar", toks.next().toString());
        assertFalse(toks.hasNext());
    }

    public void testTwoTokens() {
        toks = new QueryTokenizer("foo1=bar1&foo2=bar2");
        assertTrue(toks.hasNext());
        assertEquals("foo1=bar1", toks.next().toString());
        assertTrue(toks.hasNext());
        assertEquals("foo2=bar2", toks.next().toString());
        assertFalse(toks.hasNext());
    }

    public void testThreeTokens() {
        toks = new QueryTokenizer("a=hula_bob&b=g:;/2&c()=-");
        assertTrue(toks.hasNext());
        assertEquals("a=hula_bob", toks.next().toString());
        assertTrue(toks.hasNext());
        assertEquals("b=g:;/2", toks.next().toString());
        assertTrue(toks.hasNext());
        assertEquals("c()=-", toks.next().toString());
        assertFalse(toks.hasNext());
    }

    public void testUncompletedKey() {
        toks = new QueryTokenizer("abe");
        assertTrue(toks.hasNext());
        assertEquals("abe=", toks.next().toString());
    }

    public void testUncompletedKeyWithTrailingAmpersan() {
        toks = new QueryTokenizer("abe&");
        assertTrue(toks.hasNext());
        assertEquals("abe=", toks.next().toString());
    }

    public void testSingleWithTrailingAmpersan() {
        toks = new QueryTokenizer("abe=cape&");
        assertTrue(toks.hasNext());
        assertEquals("abe=cape", toks.next().toString());
    }

    public void testNullSequence() {
        toks = new QueryTokenizer(null);
        assertFalse(toks.hasNext());
        try {
            toks.next();
            fail("Expected a NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }
    }
}
