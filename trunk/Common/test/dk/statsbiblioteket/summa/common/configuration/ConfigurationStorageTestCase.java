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

import dk.statsbiblioteket.summa.common.configuration.storage.*;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ConfigurationStorageTestCase extends TestCase {
    private static Log log = LogFactory.getLog(ConfigurationStorageTestCase.class);
    static final String CONFIGNAME =
            "data/configurationFiles/configuration.xml";
    public ConfigurationStorage storage;
    public String testName;

    public ConfigurationStorageTestCase() throws Exception {
        this(new FileStorage(CONFIGNAME));
    }

    public ConfigurationStorageTestCase(ConfigurationStorage storage) {
        this.storage = storage;
        testName = storage.getClass().getSimpleName();
    }

    public void testSet() throws Exception {
        log.info("Sleeping 1s");
        Thread.sleep(1000);

        log.info(testName + ": Testing set()");
        String resultValue, testValue = "MyTestValue";
        storage.put(RemoteStorageMBean.CONF_NAME, testValue);
        resultValue = (String) storage.get (RemoteStorageMBean.CONF_NAME);

        assertEquals("Setting and getting a property should leave it unchanged",
                     testValue, resultValue);
    }

    public void testPurge () throws Exception {
        log.info(testName + ": Testing purge()");
        String testPurgeKey = "testPurge";
        String testPurgeValue = "testValue";

        storage.put (testPurgeKey, testPurgeValue);
        assertEquals(testPurgeValue, storage.get(testPurgeKey));

        storage.purge (testPurgeKey);
        assertEquals("Purging a key should remove it from storage",
                     null, storage.get(testPurgeKey));
    }

    /* This fails when called directly, by design */
    public void testConfigurationInstantiation () throws Exception {
        log.info(testName + ": Testing instantiation with Configuration");

        Configuration conf = Configuration.newMemoryBased("key", "value");

        ConfigurationStorage testStorage =
                Configuration.create(storage.getClass(), conf);
        assertEquals("value", testStorage.get("key"));
    }
}