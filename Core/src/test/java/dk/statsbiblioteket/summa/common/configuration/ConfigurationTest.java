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
package dk.statsbiblioteket.summa.common.configuration;

import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.util.Environment;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Configuration Tester.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, mke")
public class ConfigurationTest extends TestCase {
    /** Configurations XML. */
    private static final String CONFIGURATIONXML = "common/configurationFiles/configuration.xml";
    /** Simple storage XML. */
    private static final String SIMPLEXSTORAGEXML = "common/configurationFiles/simple_xstorage.xml";
    private static final String STRINGSXML = "common/configurationFiles/string_list_xstorage.xml";
    /** TMP path. */
    private static final String TMP = "target/tmp";

    public ConfigurationTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!new File(TMP).exists() && !new File(TMP).mkdirs()) {
            fail("Error creating '" + TMP + "'");
        }
        Environment.sysProps = null; // Emulate new JVM with regards to environment variables
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (new File(TMP).exists()) {
            if (!new File(TMP).delete()) {
                fail("Error deleting '" + TMP + "'");
            }
        }
    }

    public void testSetGet() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetStringsSingle() throws Exception {
        Configuration conf = new Configuration(
                                        new FileStorage(CONFIGURATIONXML
                                                ));
        List<String> l = conf.getStrings("summa.configuration.service.port");
        assertEquals("getStrings on a value without ,s should have size 1",
                     1, l.size());
        assertEquals("getStrings on a value without ,s should return the expected",
                     "2768", l.get(0));
    }

    public void testGetStringsSingleAgain() throws Exception {
        Configuration conf = Configuration.newMemoryBased("foo", "bar");
        List<String> l = conf.getStrings ("foo");
        assertEquals("getStrings on a value without ','s should have size 1",
                     1, l.size());
        assertEquals("bar", l.get(0));
    }

    public void testGetStringsMany() throws Exception {
        Configuration conf = new Configuration(new MemoryStorage());
        conf.set ("foobar", "foo, bar,baz"); // Intentially no space before baz
        List<String> l = conf.getStrings ("foobar");
        assertEquals("getStrings should tokenize correctly", 3, l.size());
        assertEquals("getStrings should give expected values", "foo", l.get(0));
        assertEquals("getStrings should give expected values", "bar", l.get(1));
        assertEquals("getStrings should give expected values", "baz", l.get(2));

    }

    public void testGetStringsList() throws Exception {
        Configuration conf = new Configuration(new MemoryStorage());
        conf.set ("foobar",
                  new ArrayList<>(Arrays.asList("foo", "bar", "baz")));
        List<String> l = conf.getStrings ("foobar");
        assertEquals("getStrings should return the list", 3, l.size());
        assertEquals("getStrings should give expected values", "foo", l.get(0));
        assertEquals("getStrings should give expected values", "bar", l.get(1));
        assertEquals("getStrings should give expected values", "baz", l.get(2));
    }

    public void testGetStringsListXStorage() throws Exception {
        Configuration conf = Configuration.load(STRINGSXML);
        List<String> l = conf.getStrings ("mykey");
        assertEquals("getStrings should return the list", 3, l.size());
        assertEquals("getStrings should give expected values", "foo", l.get(0));
        assertEquals("getStrings should give expected values", "bar", l.get(1));
        assertEquals("getStrings should give expected values", "baz", l.get(2));
    }

    public void testGetStringsListXStorageDefault() throws Exception {
        Configuration conf = Configuration.load(STRINGSXML);
        List<String> l = conf.getStrings ("mykey");
        assertEquals("getStrings should return the list", 3, l.size());
        assertEquals("getStrings should give expected values", "foo", l.get(0));
        assertEquals("getStrings should give expected values", "bar", l.get(1));
        assertEquals("getStrings should give expected values", "baz", l.get(2));
    }

    public void testGetString() throws Exception {
        Configuration conf = new Configuration(new FileStorage(
                CONFIGURATIONXML));
        String s = conf.getString ("summa.configuration.service.port");
        assertEquals("getString should return expected value", "2768", s);
    }

    public void testGetStringWithDefault () throws Exception {
        Configuration conf = new Configuration(new MemoryStorage());
        String s = conf.getString ("foobar", "baz");
        assertEquals("Should return default value", "baz", s);
    }

    public void testToString () throws Exception {
        Configuration conf = new Configuration(new FileStorage(
                CONFIGURATIONXML));
        assertNotNull(conf.toString());
    }

    public void testSimpleEquals() throws Exception {
        Configuration c1 = new Configuration(new FileStorage(
                CONFIGURATIONXML));
        Configuration c2 = new Configuration(new FileStorage(
                CONFIGURATIONXML));
        assertEquals("Empty configurations should equal", c1, c2);
    }

    public void testGetInt() throws Exception {
        Configuration configuration = new Configuration(new MemoryStorage());
        configuration.set("a", "12");
        configuration.set("b", "-12");
        configuration.set("c", "krabardaf");
        configuration.set("d", " 12");
        configuration.set("e", "12 ");
        configuration.set("f", " 12 ");
        assertEquals("a should give 12", 12, configuration.getInt("a"));
        assertEquals("b should give -12", -12, configuration.getInt("b"));
        assertEquals("d should give 12", 12, configuration.getInt("d"));
        assertEquals("e should give 12", 12, configuration.getInt("e"));
        assertEquals("f should give 12", 12, configuration.getInt("f"));
        try {
            configuration.getInt("c");
            fail("c should throw an exception");
        } catch (Exception e) {
            // Expected behaviour
        }
    }

    public void testGetLong() throws Exception {
        Configuration configuration = new Configuration(new MemoryStorage());
        configuration.set("a", "12");
        configuration.set("b", "-12");
        configuration.set("c", "krabardaf");
        configuration.set("d", " 12");
        configuration.set("e", "12 ");
        configuration.set("f", " 12 ");
        assertEquals("a should give 12", 12, configuration.getLong("a"));
        assertEquals("b should give -12", -12, configuration.getLong("b"));
        assertEquals("d should give 12", 12, configuration.getLong("d"));
        assertEquals("e should give 12", 12, configuration.getLong("e"));
        assertEquals("f should give 12", 12, configuration.getLong("f"));
        try {
            configuration.getLong("c");
            fail("c should throw an exception");
        } catch (Exception e) {
            // Expected behaviour
        }
    }

    public void testGetIntValues() throws Exception {
        Configuration configuration = new Configuration(new MemoryStorage());
        configuration.set("ok", "a 1, b(2),c(-3),  d ( 4  ) , e, f (5)");
        configuration.set("invalid1", "a(3a)");
        configuration.set("invalid2", "a((3))");
        // TODO File a bug on no error with non-static inner class
        List<Configuration.Pair<String, Integer>> elements =
                configuration.getIntValues("ok", 87);
        assertEquals("The value for a 1 should be 87",
                     new Integer(87), elements.get(0).getSecond());
        assertEquals("The value for b should be 2",
                     new Integer(2), elements.get(1).getSecond());
        assertEquals("The value for c should be -3",
                     new Integer(-3), elements.get(2).getSecond());
        assertEquals("The value for d should be 4",
                     new Integer(4), elements.get(3).getSecond());
        assertEquals("The value for e should be 87",
                     new Integer(87), elements.get(4).getSecond());
        assertEquals("The value for f should be 5",
                     new Integer(5), elements.get(5).getSecond());
    }

    public void testInvalidIntValues() {
        Configuration configuration = new Configuration(new MemoryStorage());
        configuration.set("bad", "a(3a), b((3))");
        List<Configuration.Pair<String, Integer>> elements =
                configuration.getIntValues("bad", 87);
        assertEquals("The first element should be named a(3a)",
                     "a(3a)", elements.get(0).getFirst());
        assertEquals("The first element should be named b(",
                     "b(", elements.get(1).getFirst());
    }

    public void testGetIntWithDefault() throws Exception {
        Configuration configuration = new Configuration(new MemoryStorage());
        int i = configuration.getInt("foobar", 27);
        assertEquals("Integer should be default value", 27, i);
    }

    public void testGetSystemConfigLocal() throws Exception {
        String confPath = CONFIGURATIONXML;
        //Thread.currentThread().getContextClassLoader()
        //  .getResource("configuration.xml").toString();
        System.setProperty(Configuration.CONF_CONFIGURATION_PROPERTY, confPath);

        Configuration conf = Configuration.getSystemConfiguration();
        Configuration originalConf = new Configuration(new FileStorage(
                CONFIGURATIONXML));

        assertEquals("Loading via getSystemConfiguration and directly should result in identical configurations",
                     conf, originalConf);

        System.clearProperty(Configuration.CONF_CONFIGURATION_PROPERTY);
    }

    // Disabled as we do not use remote (RMI) configurations anywhere
    public void disabledtestGetSystemConfigRemote() throws Exception {
        RemoteStorageTest remote = new RemoteStorageTest();
        remote.setUp();
        String serviceUrl = remote.getDirectStorage().getServiceUrl();
//        System.out.println("Service URL is: " + serviceUrl);

        System.setProperty(Configuration.CONF_CONFIGURATION_PROPERTY,
                           serviceUrl);

        Configuration testConf = Configuration.getSystemConfiguration();
        Configuration originalConf = new Configuration(new FileStorage(
                CONFIGURATIONXML));

        assertEquals("Loading via getSystemConfiguration and directly should result in identical configurations",
                     testConf, originalConf);

        System.clearProperty(Configuration.CONF_CONFIGURATION_PROPERTY);
    }

    public void disabledtestGetSystemConfigUnset() throws Exception {
        Configuration conf = Configuration.getSystemConfiguration(true);
        assertNotNull(conf);
        assertNotNull(conf.getStorage());

        try {
            Configuration.getSystemConfiguration(false);
            fail("Should not be able to retrieve system config when not set");
        } catch (Configurable.ConfigurationException e) {
            // expected
        }

    }

    public void testNewMemoryBasedEmpty() {
        Configuration conf = Configuration.newMemoryBased();
        int count = 0;

        //noinspection UnusedDeclaration
        for (Map.Entry<String, Serializable> entry : conf) {
            count++;
        }
        assertEquals(count, 0);
    }

    public void testNewMemoryBased() {
        int count = 0;
        ArrayList<String> list = new ArrayList<>();
        list.add("foo");
        list.add("bar");

        Configuration conf = Configuration.newMemoryBased(
                "hello.world", 10,
                "hello.universe", list,
                "hello.nothing", new String[]{"baz"}
        );

        //noinspection UnusedDeclaration
        for (Map.Entry<String, Serializable> entry : conf) {
            count++;
        }

        assertEquals(count, 3);
        assertEquals(conf.getInt("hello.world"), 10);

        List<String> l = conf.getStrings("hello.universe");
        assertEquals(l.size(), 2);
        assertEquals("foo", l.get(0));
        assertEquals("bar", l.get(1));

        l = conf.getStrings("hello.nothing");
        assertEquals(l.size(), 1);
        assertEquals("baz", l.get(0));
    }

    public void testSubConfigurations() throws Exception {
        Configuration conf = new Configuration(new XStorage(false));
        List<Configuration> subs = conf.createSubConfigurations("foo", 3);
        assertEquals("The length of the created list should be correct",
                     3, subs.size());
        List<Configuration> extracted = conf.getSubConfigurations("foo");
        assertEquals("The length of the extracted list should be correct",
                     3, extracted.size());

    }

    public void testLoadXConfiguration() throws Exception {
        Configuration conf = Configuration.load(
                SIMPLEXSTORAGEXML);
        assertTrue("The underlying Storage should be an XStorage",
                   conf.getStorage() instanceof XStorage);
    }

    public void testLoadXConfigurationFromFile() throws Exception {
        File tmp = new File(TMP, "tmpstorage.xml");
//        System.out.println(tmp.getAbsolutePath());
        Files.copy(Resolver.getFile(
                SIMPLEXSTORAGEXML), tmp, true);

        Configuration conf = Configuration.load(tmp.toString());
        assertTrue("The underlying Storage should be an XStorage",
                   conf.getStorage() instanceof XStorage);
        assertTrue("The underlying Storage should support sub storages",
                   conf.supportsSubConfiguration());
        if (!tmp.delete()) {
            fail("Error deleting '" + tmp.getName() + "'");
        }
    }

    public void testGetSystemConfigError () throws Exception {
        try {
            Configuration.getSystemConfiguration("summa.snafu", false);
            fail("Lookup of unexisting system config should fail");
        } catch (Configurable.ConfigurationException e) {
            // expected
        }
    }

    public void testGetSystemConfigAuto() throws Exception {
        Configuration conf = Configuration.getSystemConfiguration("summa.snafu", true);

        int count = 0;
        //noinspection UnusedDeclaration
        for (Object o : conf) {
            count++;
        }

        if (count == 0) {
            fail("Got empty system config. Expected non-empty config to be found");
        }

    }

    public void testListSystemProperties() {
        String props = Strings.join(System.getProperties().entrySet());
        assertFalse("The System properties should not be empty", "".equals(props));
        System.out.println("System properties: " + props);
    }

    public void testExpandedSysProp() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                "foo.bar", "${user.home}");

        assertEquals(System.getProperty("user.home"), conf.getString("foo.bar"));

        // Also test that we expand the default value
        assertEquals(System.getProperty("user.home"), conf.getString("b0rk", "${user.home}"));
    }

    public void testExpandedSysPropList() throws Exception {
        ArrayList<String> val = new ArrayList<>(Arrays.asList("${user.dir}", "bar"));
        Configuration conf = Configuration.newMemoryBased("foo.bar", val);

        List<String> expected = Arrays.asList(System.getProperty("user.dir"), "bar");
        assertEquals(expected, conf.getStrings("foo.bar"));

        // Also test that we expand the default value
        assertEquals(expected,
                     conf.getStrings("b0rk", val));
    }

    public void testExpandedSysPropIntValues () throws Exception {
        System.setProperty("intprop", "27");
        String val = "a(1), b(${intprop}), ${os.name}";
        Configuration conf = Configuration.newMemoryBased( "foo.bar", val);

/*        Arrays.asList(
                new Configuration.Pair<String, Integer>("a", 1),
                new Configuration.Pair<String, Integer>("b", 2)
        );*/
        // TODO check expected
        List<Configuration.Pair<String, Integer>> result = conf.getIntValues("foo.bar", 3);

        // Clear the sys prop before the test has a chance of failing
        System.clearProperty("intprop");

        assertEquals(3, result.size());

        assertEquals(new Configuration.Pair<>("a", 1), result.get(0));

        assertEquals(new Configuration.Pair<>("b", 27), result.get(1));

        assertEquals(new Configuration.Pair<>(System.getProperty("os.name"), 3), result.get(2));
    }

    public void testExpandSysPropSimpleValues() throws Exception {
        System.setProperty("intprop", "27");
        System.setProperty("boolprop", "true");
        System.setProperty("longprop", "270");

        Configuration conf = Configuration.newMemoryBased(
                "intprop", "${intprop}",
                "boolprop", "${boolprop}",
                "longprop", "${longprop}");

        int intprop = conf.getInt("intprop");
        boolean boolprop = conf.getBoolean("boolprop");
        long longprop = conf.getLong("longprop");

        // Clear the sys prop before the test has a chance of failing
        System.clearProperty("intprop");
        System.clearProperty("boolprop");
        System.clearProperty("longprop");

        assertEquals("intprop", 27, intprop);
        assertTrue("boolprop", boolprop);
        assertEquals("longprop", 270, longprop);
    }

    public void testExpandedSysPropArray() throws Exception {
        String[] val = new String[]{"${user.dir}", "bar"};
        Configuration conf = Configuration.newMemoryBased("foo.bar",
                                                          val);

        String[] expected = new String[]{System.getProperty("user.dir"), "bar"};

        assertTrue(Arrays.equals(expected,
                                 conf.getStrings("foo.bar", new String[0])));

        // Also test that we expand the default value
        assertTrue(Arrays.equals(expected,
                                 conf.getStrings("b0rk", val)));
    }

    public void testUnEscape() throws Exception {
        File file = new File("mypath#klaf/somefile.xml");
        URL url = file.toURI().toURL();
        assertNotSame("Got URL '" + url + "' expected '" + file + "'", file,
                url);
        assertNotSame("Got File from URL: '" + url.getFile() + "' expected '"
                + file + "'.", file, url.getFile());
        assertEquals("new File(URI): " + new File(url.toURI()),
               file.getAbsolutePath(), new File(url.toURI()).getAbsolutePath());

    }

    public void testPathEscape() {
        assertTrue("mykey should exist in XProperties in plainPath",
                   Configuration.load("common/plainPath/simpleconf.xml").
                           valueExists("mykey"));
        assertTrue("mykey should exist in XProperties in special#%Path",
                   Configuration.load("common/special#%Path/simpleconf.xml").
                           valueExists("mykey"));
        assertTrue("myOldKey should exist in Properties in plainPath",
                   Configuration.load("common/plainPath/oldconf.xml").
                           valueExists("myOldKey"));
        assertTrue("myOldKey should exist in Properties in special#%Path",
                   Configuration.load("common/special#%Path/oldconf.xml").
                           valueExists("myOldKey"));
    }

    public void testGetClass() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetStorage() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetFirst() throws Exception {
        //TODO: Test goes here...
    }

    public void testGetSecond() throws Exception {
        //TODO: Test goes here...
    }

    // This uses RMI and does not work, reasons unknown, under XStream 1.4.10 and OpenJDK 1.8
    // As we don't use RMI configurations in any Summa installations, we ignore this
    public void disabledtestImportConfiguration() throws Exception {
        Configuration conf = Configuration.getSystemConfiguration(true);
        assertNotNull(conf);
        assertNotNull(conf.getStorage());

        conf.importConfiguration(conf);
    }

    public static Test suite() {
        return new TestSuite(ConfigurationTest.class);
    }
}
