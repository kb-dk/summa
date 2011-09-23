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
package dk.statsbiblioteket.summa.common.configuration.storage;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageTestCase;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * XStorage Tester.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XStorageTest extends ConfigurationStorageTestCase {
    /** Local log instance. */
    private static Log log = LogFactory.getLog(XStorageTest.class);
    /** Sub storage XML location. */
    public static final File SUBSTORAGELOCATION =
               Resolver.getFile("data/configurationFiles/substorage.xml").
                   getAbsoluteFile();


    /**
     * Create s XStorageTest instance.
     * @throws Exception If error occur.
     */
    public XStorageTest() throws Exception {
        super(new XStorage(false));
    }

    @Override
    public final void setUp() throws Exception {
        super.setUp();
        assertTrue("The subLocation " + SUBSTORAGELOCATION + " should exist",
                   SUBSTORAGELOCATION.exists());
    }

    @Override
    public final void tearDown() throws Exception {
        super.tearDown();
        String storageFile = ((XStorage) super.storage).getFilename();
        if (storageFile != null && new File(storageFile).exists()) {
            Files.delete(storageFile);
        }
    }

    /**
     * Test instantiation.
     */
    public void testInstantiation() {
        // pass
    }

    @SuppressWarnings("redundantcast")
    public final void testGetSubStorage() {
        final int seven = 7;
        final int eight = 8;
        try {
            XStorage xs = new XStorage(SUBSTORAGELOCATION);
            assertEquals("The storage should contain the number 7",
                         new Integer(seven), (Integer) xs.get("seven"));
            ConfigurationStorage sub = xs.getSubStorage("submarine");
            assertNotNull("There should be a sub storage named submarine", sub);
            assertEquals("The sub storage should contain 8", new Integer(eight),
                         (Integer) sub.get("eight"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    /**
     * Test configuration wrap.
     */
    public final void testConfigurationWrap() {
        final int seven = 7;
        final int eight = 8;
        try {
            XStorage xs = new XStorage(SUBSTORAGELOCATION);
            Configuration configuration = new Configuration(xs);
            assertEquals("The storage should contain 7",
                         seven, configuration.getInt("seven"));
            assertTrue("The configuration should support sub configurations",
                       configuration.supportsSubConfiguration());
            Configuration subConf =
                                 configuration.getSubConfiguration("submarine");
            assertEquals("The sub configuration should contain 8",
                         eight, subConf.getInt("eight"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    /**
     * @return A test suite.
     */
    public static Test suite() {
        return new TestSuite(XStorageTest.class);
    }

    /**
     * Test dump sub lists.
     */
    public final void testDumpSubList() {
        try {
            XStorage xs = new XStorage(false);
            Configuration configuration = new Configuration(xs);
            List<Configuration> subConfs =
                    configuration.createSubConfigurations("foo", 1);
            subConfs.get(0).set("bar", "baz");
            log.info(new XProperties().getXStream().toXML(xs));
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    /**
     * Test next available configuration file.
     */
    public final void testnextAvailableConfigurationFile() {
        XStorage xs = null;
        try {
            xs = new XStorage(true);
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
        Pattern p = Pattern.compile(".*xconfiguration\\.\\d*\\.xml");
        String name = xs.getFilename();
        assertTrue(p.matcher(name).find());
        new File(name).delete();
    }

    public void testStringsList() throws IOException {
        File MISCSTORAGELOCATION =
            Resolver.getFile("data/configurationFiles/misc_storage.xml").
                getAbsoluteFile();
        XStorage xs = new XStorage(MISCSTORAGELOCATION);
        Configuration conf = new Configuration(xs);
        assertEquals("Single String value should be resolvable",
                     "foo", conf.getString("single"));
        assertEquals("Single-line multi String value should return the right "
                     + "number of Strings",
                     2, conf.getStrings("single_line").size());
        assertEquals("Multi-value multi String should return the right "
                     + "number of Strings",
                     2, conf.getStrings("multi_line").size());
    }
}
