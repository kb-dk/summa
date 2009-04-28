/* $Id: ConfigurationStorageTestCase.java,v 1.3 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:18 $
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
package dk.statsbiblioteket.summa.common.configuration;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorageMBean;
import dk.statsbiblioteket.util.qa.QAInfo;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ConfigurationStorageTestCase extends TestCase {

    static final String configFilename = "configuration.xml";
    public ConfigurationStorage storage;
    public String testName;

    public ConfigurationStorageTestCase (ConfigurationStorage storage) {
        this.storage = storage;
        testName = storage.getClass().getSimpleName();
    }

    public void testSet () throws Exception {
        System.out.println ("Sleeping 1s");
        Thread.sleep(1000);

        System.out.println (testName + ": Testing set()");
        String resultValue, testValue = "MyTestValue";
        storage.put (RemoteStorageMBean.CONF_NAME, testValue);
        resultValue = (String) storage.get (RemoteStorageMBean.CONF_NAME);

        assertTrue ("Setting and getting a property should leave it unchanged", testValue.equals(resultValue));
    }

    public void testPurge () throws Exception {
        System.out.println (testName + ": Testing purge()");
        String testPurgeKey = "testPurge";
        String testPurgeValue = "testValue";

        storage.put (testPurgeKey, testPurgeValue);
        storage.purge (testPurgeKey);

        assertNull("Purging a key should remove it from storage", storage.get (testPurgeKey));
    }

    /* This fails when called directly, by design */
    public void testConfigurationInstantiation () throws Exception {
        System.out.println (testName + ": Testing instantiation with Configuration");

        Configuration conf = Configuration.newMemoryBased("key", "value");

        ConfigurationStorage testStorage =
                Configuration.create(storage.getClass(), conf);
        assertEquals("value", testStorage.get("key"));
    }
}



