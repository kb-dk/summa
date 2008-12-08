package dk.statsbiblioteket.summa.common.util;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link Environment} class
 */
public class EnvironmentTest extends TestCase {

    public void testEscapeOneSysProp () {
        String home = System.getProperty("user.home");
        String source = "${user.home}";

        assertEquals(home,
                     Environment.escapeSystemProperties(source));
    }

    public void testEscapeTwoSysProps () {
        String home = System.getProperty("user.home");
        String ioTmpdir = System.getProperty("java.io.tmpdir");
        String source = "${user.home} ${java.io.tmpdir}";

        assertEquals(home + " " + ioTmpdir,
               Environment.escapeSystemProperties(source));
    }

    public void testEscapeTwoSysPropsPreserveSpaces () {
        String home = System.getProperty("user.home");
        String ioTmpdir = System.getProperty("java.io.tmpdir");
        String source = " ${user.home}  ${java.io.tmpdir} ";

        assertEquals(" " + home + "  " + ioTmpdir + " ",
               Environment.escapeSystemProperties(source));
    }

    /**
     * Unknown sy props should not be touched
     */
    public void testEscapeNonExistingSysProp () {
        String source = "${user.homer}";

        assertEquals(source,
                     Environment.escapeSystemProperties(source));
    }

    public void testUnclosedSysProp () {
        String source = "${user.home";

        assertEquals(source,
                     Environment.escapeSystemProperties(source));
    }

    public void testNull () {
        assertEquals(null,
                     Environment.escapeSystemProperties((String)null));
        assertEquals(null,
                     Environment.escapeSystemProperties((String[])null));
        assertEquals(null,
                     Environment.escapeSystemProperties((Iterable<String>)null));
    }
}
