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
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.XProperties;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

/**
 * XStorage Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XStorageTest extends ConfigurationStorageTestCase {
    public static final File subLocation =
               new File("Common/test/data/substorage.xml").getAbsoluteFile();

    public XStorageTest() throws Exception {
        super(new XStorage());
    }

    public void setUp() throws Exception {
        super.setUp();
        assertTrue("The subLocation " + subLocation + " should exist",
                   subLocation.exists());
    }

    public void tearDown() throws Exception {
        super.tearDown();
        String storageFile = ((XStorage)super.storage).getFilename();
        if (new File(storageFile).exists()) {
            Files.delete (storageFile);
        }
    }

    public void testInstantiation () throws Exception {
        // pass
    }

    @SuppressWarnings({"RedundantCast"})
    public void testGetSubStorage() throws Exception {
        XStorage xs = new XStorage(subLocation);
        assertEquals("The storage should contain the number 7",
                     new Integer(7), (Integer)xs.get("seven"));
        ConfigurationStorage sub = xs.getSubStorage("submarine");
        assertNotNull("There should be a sub storage named submarine", sub);
        assertEquals("The sub storage should contain 8", new Integer(8),
                     (Integer)sub.get("eight"));
    }

    public void testConfigurationWrap() throws Exception {
        XStorage xs = new XStorage(subLocation);
        Configuration configuration = new Configuration(xs);
        assertEquals("The storage should contain 7",
                     7, configuration.getInt("seven"));
        assertTrue("The configuration should support sub configurations",
                   configuration.supportsSubConfiguration());
        Configuration subConf = configuration.getSubConfiguration("submarine");
        assertEquals("The sub configuration should contain 8",
                     8, subConf.getInt("eight"));
    }

    public static Test suite() {
        return new TestSuite(XStorageTest.class);
    }

    public void testDumpSubList() throws Exception {
        XStorage xs = new XStorage();
        Configuration configuration = new Configuration(xs);
        List<Configuration> subConfs =
                configuration.createSubConfigurations("foo", 1);
        subConfs.get(0).set("bar", "baz");
        System.out.println(new XProperties().getXStream().toXML(xs));
    }

    public void testnextAvailableConfigurationFile() throws Exception {
        XStorage xs = new XStorage();
        Pattern p = Pattern.compile(".*xconfiguration\\.\\d*\\.xml");
        String name = xs.getFilename();
        assertTrue(p.matcher(name).find());
        new File(name).delete();
    }
}




