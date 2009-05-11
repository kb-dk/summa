package dk.statsbiblioteket.summa.common.lucene.analysis;

import junit.framework.TestCase;

import java.io.StringReader;

import dk.statsbiblioteket.util.Strings;

/**
 *
 */
public class LowerCasingReaderTest extends TestCase {

    LowerCasingReader r;

    public void testEmptyStream() throws Exception {
        r = new LowerCasingReader(new StringReader(""));
        assertEquals("", Strings.flushLocal(r));
    }

    public void testSingleChar() throws Exception {
        r = new LowerCasingReader(new StringReader("a"));
        assertEquals("a", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("A"));
        assertEquals("a", Strings.flushLocal(r));
    }

    public void testVarCase() throws Exception {
        r = new LowerCasingReader(new StringReader("ab"));
        assertEquals("ab", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("aB"));
        assertEquals("ab", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("Ab"));
        assertEquals("ab", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("AB"));
        assertEquals("ab", Strings.flushLocal(r));
    }

    public void testSymbols() throws Exception {
        r = new LowerCasingReader(new StringReader("#"));
        assertEquals("#", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("$"));
        assertEquals("$", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("£"));
        assertEquals("£", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("*"));
        assertEquals("*", Strings.flushLocal(r));
    }

}
