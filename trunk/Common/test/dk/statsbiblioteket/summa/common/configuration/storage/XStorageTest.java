/* $Id: XStorageTest.java,v 1.2 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.configuration.storage;

import java.io.File;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorageTestCase;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.XProperties;

/**
 * XStorage Tester.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XStorageTest extends ConfigurationStorageTestCase {
    File subLocation = new File("Common/test/dk/statsbiblioteket/summa/"
                                + "common/configuration/storage/"
                                + "substorage.xml").getAbsoluteFile();

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
        assertEquals("The sub storage should contain 8", new Integer(8), (Integer)sub.get("eight"));
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
}
