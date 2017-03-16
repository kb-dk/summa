/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.util;

import junit.framework.TestCase;

import java.util.regex.Pattern;

/**
 * Unit tests for the {@link Environment} class
 */
public class EnvironmentTest extends TestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Environment.resetPropertyCache();
    }

    /** Test escape one system properties. */
    public void testEscapeOneSysProp() {
        String home = System.getProperty("user.home");
        String source = "${user.home}";

        assertEquals(home, Environment.escapeSystemProperties(source));
    }

    public void testEscapingWithDefaultValue() {
        System.setProperty("foo", "bar");
        assertEquals("Existing properties should be escaped properly",
                     "bar", Environment.escapeSystemProperties("${foo}"));
        assertEquals("Non-existing properties should be left unchanged",
                     "${zoo}", Environment.escapeSystemProperties("${zoo}"));
        assertEquals("Non-existing properties with default value should return that value",
                     "baz", Environment.escapeSystemProperties("${zoo:baz}"));
        assertEquals("Non-existing properties with empty default value should return empty",
                     "", Environment.escapeSystemProperties("${zoo:}"));
    }

    public void testPasswordMasking() {
        System.setProperty("key.password", "foo");
        System.setProperty("bar", "value.password");
        System.setProperty("zoo", "baz");
        String dump = Environment.dumpCachedEntries();
        assertTrue("The human readable dump of cached properties should redact values for keys containing the String " +
                   "'password'\n" + dump,
                   dump.contains("key.password=[defined]"));
        assertTrue("The human readable dump of cached properties should redact values containing the String " +
                   "'password'\n" + dump,
                   dump.contains("bar=[defined]"));
        assertTrue("The human readable dump of cached properties should not redact values for 'normal' key/value-pairs"
                   + "\n" + dump,
                   dump.contains("zoo=baz"));
    }

    /** Test escape two system properties. */
    public void testEscapeTwoSysProps() {
        String home = System.getProperty("user.home");
        String ioTmpdir = System.getProperty("java.io.tmpdir");
        String source = "${user.home} ${java.io.tmpdir}";

        assertEquals(home + " " + ioTmpdir, Environment.escapeSystemProperties(source));
    }

    /** Test escape two system properties with spaces. */
    public void testEscapeTwoSysPropsPreserveSpaces() {
        String home = System.getProperty("user.home");
        String ioTmpdir = System.getProperty("java.io.tmpdir");
        String source = " ${user.home}  ${java.io.tmpdir} ";

        assertEquals(" " + home + "  " + ioTmpdir + " ", Environment.escapeSystemProperties(source));
    }

    // Values containing $ are special when appending to matchers, so they need to be escaped by the implementation
    public void testReplacementQuoting() {
        String KEY = "mykey";
        String PROPERTIES = "${" + KEY + "}";
        String VALUE = "$MYVALUE";
        System.setProperty(KEY, VALUE);
        assertEquals("Escaping full value with String containing '$' should work",
                     VALUE, Environment.escapeSystemProperties(PROPERTIES));
    }

    /**
     * Unknown system. props should not be touched
     */
    public void testEscapeNonExistingSysProp () {
        String source = "${user.homer}";
        assertEquals(source, Environment.escapeSystemProperties(source));
    }

    public void testGetMachineName() {
        assertFalse("The machine name should be something", Environment.getMachineName().isEmpty());
    }

    /** Test unclosed system properties. */
    public void testUnclosedSysProp() {
        String source = "${user.home";

        assertEquals(source, Environment.escapeSystemProperties(source));
    }

    /** Test null in properties. */
    public void testNull() {
        assertEquals(null, Environment.escapeSystemProperties((String)null));
        assertEquals(null, Environment.escapeSystemProperties((String[])null));
        assertEquals(null, Environment.escapeSystemProperties((Iterable<String>)null));
    }

    /**
     * Check java version test.
     * @throws Exception If error.
     */
    public void testCheckJavaVersion() throws Exception {
        //System.out.println("Current JVM version: "
        //                   + Environment.checkJavaVersion());
        // TODO assert  
    }
}

