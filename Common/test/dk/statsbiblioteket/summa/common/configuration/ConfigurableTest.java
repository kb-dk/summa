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

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.storage.FileStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.RemoteStorage;
import dk.statsbiblioteket.summa.common.configuration.storage.JStorage;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class ConfigurableTest extends TestCase {
    
    public void setUp () throws Exception {

    }

    public void tearDown () throws Exception {

    }

    /**
     * Try and instantiate each {@link ConfigurationStorage} as a {@link Configurable}
     * through {@link Configuration#create}.
     *
     * Assert that Configuration based on these storages are equal (this is a deep check)
     */
    public void testStorageInstantiations () throws Exception {
        Configuration base = new Configuration(
                                          new FileStorage("configuration.xml"));

        Configuration fileConf = new Configuration(
                (ConfigurationStorage)Configuration.create(FileStorage.class,
                                                          base));
        assertTrue (base.equals(fileConf));

        Configuration memConf = new Configuration(
                (ConfigurationStorage)Configuration.create(MemoryStorage.class,
                                                          base));
        assertTrue (base.equals(memConf));

        Configuration remoteConf = new Configuration(
                (ConfigurationStorage)Configuration.create(RemoteStorage.class,
                                                          base));
        assertTrue (base.equals(remoteConf));

        Configuration jConf = new Configuration(
                (ConfigurationStorage)Configuration.create(JStorage.class,
                                                          base));
        assertTrue (base.equals(jConf));
    }
}




