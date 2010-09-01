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
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.File;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class FileStorageTest extends ConfigurationStorageTestCase {

    public FileStorageTest () throws Exception {
        super (new FileStorage("data/configurationFiles/configuration.xml"));
    }

    public void testEmptyConstructor () throws Exception {
        FileStorage s = new FileStorage();
        System.out.println ("Created empty file storage at: " + s.getFilename());

        testPersitence();

        new File(s.getFilename()).delete();
    }

    public void testPersitence () throws Exception {
        System.out.println (testName + ": Testing persistence");

        String persitenceTestValue = "PersistenceTestValue";
        String persitenceTestKey = "PersistenceTestKey";
        String resultValue;

        storage.put (persitenceTestKey, persitenceTestValue);

        ConfigurationStorage newStorage = new FileStorage(configFilename);
        resultValue = (String) newStorage.get (persitenceTestKey);

        assertTrue ("Setting a value on a FileStorage should save the underlying configuration file", persitenceTestValue.equals (resultValue));

        storage.purge (persitenceTestKey);

    }
}