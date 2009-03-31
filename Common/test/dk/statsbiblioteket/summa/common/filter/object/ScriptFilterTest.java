package dk.statsbiblioteket.summa.common.filter.object;

import junit.framework.TestCase;

import java.io.StringReader;

/**
 * Test cases for {@link ScriptFilter}
 */
public class ScriptFilterTest extends TestCase {

    public void testSimpleJS() throws Exception {

        ObjectFilter filter = new ScriptFilter(new StringReader("print(true);"));
        assertEquals(true, filter.pump());

    }

}
