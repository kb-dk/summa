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
package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.database.*;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.*;

import java.io.File;

/**
 * @author Henrik Kirk <mailto:hbk@statsbiblioteket.dk>
 * @since 03/29/2010
 * @version 1.0
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hbk"
        )
public class StorageWSTest extends TestCase  {
    StorageWS storageWS = null;
    Storage storage = null;
    protected static String testDBRoot = "test_db";

    public StorageWSTest(String name) throws Exception {
        super(name);
        storageWS = new StorageWS();
        File dbRoot = new File(testDBRoot);
        if (dbRoot.exists()) {
            Files.delete (dbRoot);
        }

        storage = new RMIStorageProxy(Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, testDBRoot,
                DatabaseStorage.CONF_CREATENEW, true,
                DatabaseStorage.CONF_FORCENEW, true));
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        if(storage == null) {
            return;
        }
        if (storage instanceof DatabaseStorage) {
            ((DatabaseStorage)storage).destroyDatabase();
        }
        /* We get spurious errors where the connection to the db isn't ready
         * when running the unit tests in batch mode */
        Thread.sleep(200);

        storage.close();
        /* We get spurious errors where the connection to the db isn't ready
         * when running the unit tests in batch mode */
        Thread.sleep(200);
    }

    public static Test suite() {
        return new TestSuite(StorageWSTest.class);
    }

    public void testGetRecords() throws Exception {
        Record r1 = new Record("id1", "base1", "data".getBytes());
        Record r2 = new Record("id2", "base1", "data".getBytes());

        storage.flush(r1);
        storage.flush(r2);

        // test get all records.
        String[] ids = new String[] {"id1", "id2"} ;
        String result = storageWS.getRecords(ids);
        assertNotNull(result);
        assertTrue(result.contains("id1"));
        assertTrue(result.contains("id2"));

        // test not all records.
        ids = new String[] {"id1"} ;
        result = storageWS.getRecords(ids);
        assertNotNull(result);
        assertTrue(result.contains("id1"));
        assertFalse(result.contains("id2"));

        // test no records
        ids = new String[] {} ;
        result = storageWS.getRecords(ids);
        assertNotNull(result);
        assertFalse(result.contains("id1"));
        assertFalse(result.contains("id2"));
    }
}
