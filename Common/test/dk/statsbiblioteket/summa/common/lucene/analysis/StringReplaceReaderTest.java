package dk.statsbiblioteket.summa.common.lucene.analysis;

import junit.framework.TestCase;

import java.io.StringReader;

import dk.statsbiblioteket.util.Strings;

/**
 *
 */
public class StringReplaceReaderTest extends TestCase {

    StringReplaceReader r;

    public void testDefaultReplacements() throws Exception {
        r = new StringReplaceReader(new StringReader(".net"),
                                    null, true);
        assertEquals("dotnet", Strings.flushLocal(r));

        r = new StringReplaceReader(new StringReader("c#"),
                                    null, true);
        assertEquals("csharp", Strings.flushLocal(r));

        r = new StringReplaceReader(new StringReader("c++"),
                                    null, true);
        assertEquals("cplusplus", Strings.flushLocal(r));
    }

}
