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

import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class RemoteStorageTest extends ConfigurationStorageTestCase {
    private static Log log = LogFactory.getLog(RemoteStorageTest.class);
    /** For debugging purposes */
    public RemoteStorage direct_storage;

    public RemoteStorageTest () throws Exception {
        super (new FileStorage("configurationFiles/configuration.xml"));
        testName = this.getClass().getSimpleName();
    }

    public void setUp () throws Exception {
        Configuration conf = new Configuration(storage);

        log.info(testName + ": Creating remote storage");
        direct_storage =
                (RemoteStorage) Configuration.create(RemoteStorage.class, conf);

        log.info(testName + ": Connecting to registry on " +
                                                  conf.getString(RemoteStorage.CONF_REGISTRY_HOST) +
                                                  ":" +
                                                  conf.getInt(RemoteStorage.CONF_REGISTRY_PORT));

        Registry reg = LocateRegistry.getRegistry(conf.getString(RemoteStorage.CONF_REGISTRY_HOST),
                                                  conf.getInt(RemoteStorage.CONF_REGISTRY_PORT));

        log.info("Connecting to remote storage at '"
                           + conf.getString(RemoteStorage.CONF_NAME) + "'");
        storage = (ConfigurationStorage) reg.lookup(conf.getString(RemoteStorage.CONF_NAME));

        log.info(testName + ": Remote storage prepared");
    }

    @Override
    public void testConfigurationInstantiation () throws Exception {
        // This test case will not work the way it is implemented
        // in the base class
        ;
    }
}