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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.rmi.RMIStorageProxy;
import dk.statsbiblioteket.util.Files;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class AggregatingStorageTest extends TestCase {
    private static Log log = LogFactory.getLog(AggregatingStorageTest.class);

    /** Storage communication port. */
    private static final int PORT = 29000;
    static int test_db_counter = 0;
    String test_db_1 = "test_db_1_";
    String test_db_2 = "test_db_2_";

    Configuration conf, subConf1, subConf2;
    Configuration storageConf1, storageConf2;
    String base1 = "base1";
    String base2 = "base2";
    String base3 = "base3";
    String testId1 = "testId1";
    String testId2 = "testId2";
    String testId3 = "testId3";
    String testId4 = "testId4";
    byte[] testContent1 = "Summa rocks your socks!".getBytes(StandardCharsets.UTF_8);
    List<Configuration> storageConfs;
    Storage storage;

    @Override
    public void setUp() throws Exception {
        conf = new Configuration(new XStorage(false));
        conf.set(Storage.CONF_CLASS, AggregatingStorage.class);

        storageConfs = conf.createSubConfigurations(AggregatingStorage.CONF_SUB_STORAGES, 2);

        /* Clean up old databases */
        String db1 = test_db_1 + test_db_counter;
        if (new File(db1).exists()) {
            Files.delete(db1);
        }

        String db2 = test_db_2 + test_db_counter;
        if (new File(db2).exists()) {
            Files.delete(db2);
        }

        /* New db names */
        test_db_counter++;
        db1 = test_db_1 + test_db_counter;
        db2 = test_db_2 + test_db_counter;

        /* SUB 1 */
        subConf1 = storageConfs.get(0);
        subConf1.set(AggregatingStorage.CONF_SUB_STORAGE_BASES, (Serializable) Arrays.asList(base1, base2));
        subConf1.set(ConnectionConsumer.CONF_RPC_TARGET, "//localhost:29000/summa-storage-sub1");
        storageConf1 = subConf1.createSubConfiguration(AggregatingStorage.CONF_SUB_STORAGE_CONFIG);
        storageConf1.set(Storage.CONF_CLASS, RMIStorageProxy.class);
        storageConf1.set(DatabaseStorage.CONF_LOCATION, db1);
        storageConf1.set(RMIStorageProxy.CONF_REGISTRY_PORT, PORT);
        storageConf1.set(RMIStorageProxy.CONF_SERVICE_NAME, "summa-storage-sub1");
        storageConf1.set(DatabaseStorage.CONF_FORCENEW, true);

        /* SUB 2 */
        subConf2 = storageConfs.get(1);
        subConf2.set(AggregatingStorage.CONF_SUB_STORAGE_BASES,
                     (Serializable) Arrays.asList(base3));
        subConf2.set(ConnectionConsumer.CONF_RPC_TARGET, "//localhost:29000/summa-storage-sub2");
        storageConf2 = subConf2.createSubConfiguration(AggregatingStorage.CONF_SUB_STORAGE_CONFIG);
        storageConf2.set(Storage.CONF_CLASS, RMIStorageProxy.class);
        storageConf2.set(DatabaseStorage.CONF_LOCATION, db2);
        storageConf2.set(RMIStorageProxy.CONF_REGISTRY_PORT, PORT);
        storageConf2.set(RMIStorageProxy.CONF_SERVICE_NAME, "summa-storage-sub2");
        storageConf2.set(DatabaseStorage.CONF_FORCENEW, true);

        /* Create the aggregating storage and child storages */
        storage = StorageFactory.createStorage(conf);
    }

    @Override
    public void tearDown() throws Exception {
        storage.close();
    }

    // Only made to check a specific problem with RMI access
    public static void testLocalRMIAccess() throws IOException {
        final String RECORD_ID = "summon_FETCH-crossref_primary_10_1002_piuz_2015900083";
        final String RPC_ADDRESS = "//localhost:57400/summon-storage";

        Configuration conf = Configuration.newMemoryBased();
        Configuration sub = conf.createSubConfigurations(AggregatingStorage.CONF_SUB_STORAGES, 1).get(0);
        sub.set(AggregatingStorage.CONF_SUB_STORAGE_BASES, "summon");
        sub.set(ConnectionConsumer.CONF_RPC_TARGET, RPC_ADDRESS);
        ReadableStorage aggregator = new AggregatingStorage(conf);

        Record result = aggregator.getRecord(RECORD_ID, null);
        assertNotNull("Looking up " + RECORD_ID + " should provide a record", result);
        assertEquals("The id of the returned record should match the lookup-ID", RECORD_ID, result.getId());
        log.debug("Got record:\n" + result);
    }



    public void testGetNonExisitingRecord() throws Exception {
        Record r = storage.getRecord("nosuchrecord", null);

        assertNull(r);
    }

    public void testAddOne() throws Exception {
        Record r = new Record(testId1, base1, testContent1);
        storage.flush(r);

        assertBaseCount(base1, 1);
        assertEquals(r, storage.getRecord(testId1, null));
    }

    public void testIteration() throws Exception {
        storage.clearBase(base1);
        assertBaseCount(base1, 0);

        storage.flush(new Record(testId1, base1, testContent1));
        storage.flush(new Record(testId2, base1, testContent1));
        assertBaseCount(base1, 2);
    }

    public void testIterationAllBases() throws Exception {
        storage.clearBase(base1);
        storage.clearBase(base2);
        storage.clearBase(base3);

        assertBaseCount(base1, 0);
        Thread.sleep(2);
        assertBaseCount(base2, 0);
        Thread.sleep(2);
        assertBaseCount(base3, 0);

        storage.flush(new Record(testId1, base1, testContent1));
        storage.flush(new Record(testId2, base2, testContent1));
        storage.flush(new Record(testId3, base3, testContent1));
        storage.flush(new Record(testId4, base3, testContent1));

        assertBaseCount(base1, 1);
        Thread.sleep(2);
        assertBaseCount(base2, 1);
        Thread.sleep(2);
        assertBaseCount(base3, 2);
        assertBaseCount(null, 4);
    }

    public void testNonExistingBaseIteration() throws Exception {
        assertBaseCount("nonexistingbase", 0);
    }

    public void testMtime() throws Exception {
        testIterationAllBases();

        long mtime1 = storage.getModificationTime(base1);
        long mtime2 = storage.getModificationTime(base2);
        long mtime3 = storage.getModificationTime(base3);

        assertTrue(mtime1 < mtime2);
        assertTrue(mtime2 < mtime3);

        long globalMtime = storage.getModificationTime(null);
        assertEquals(globalMtime, mtime3);
    }

    public void assertBaseCount(String base, long expected) throws Exception {
        log.info("basecount: " + base + ", expected " + expected);
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        log.info("Iteration key: " + iterKey);
        Iterator<Record> iter = new StorageIterator(storage, iterKey);

        log.info("Counter records in iterator: " + iterKey);
        long actual = 0;
        while (iter.hasNext()) {
            iter.next();
            actual++;
        }

        if (actual != expected) {
            fail("Base '" + base + "' should contain " + expected + " records, but found " + actual);
        }
    }
}
