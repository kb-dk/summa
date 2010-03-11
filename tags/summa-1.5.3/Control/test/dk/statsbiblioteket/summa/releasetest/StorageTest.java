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
package dk.statsbiblioteket.summa.releasetest;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.filter.RecordReader;
import dk.statsbiblioteket.summa.storage.api.watch.StorageWatcher;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StorageTest extends NoExitTestCase {
    private static Log log = LogFactory.getLog(StorageTest.class);

    @Override
    public void setUp () throws Exception {
        super.setUp();
        IngestTest.deleteOldStorages();
    }

/*    public void testReopen() throws Exception {
        Storage storage = IndexTest.fillStorage();
        SearchTest.ingest(new File(
                Resolver.getURL("data/search/input/part1").getFile()));
        assertEquals("There should be something in the first storage", 1, )
        Configuration storageConf = IngestTest.getStorageConfiguration();
        storageConf.set(DatabaseStorage.PROP_FORCENEW, false);
        storage = StorageFactory.createStorage(storageConf);
    }
  */  

    public void testSimpleStorage() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        log.debug("Creating Storage");
        Storage storage = StorageFactory.createStorage(storageConf);

        Record record = new Record("Dummy", "foo", new byte[0]);
        log.debug("Adding Record to Storage");
        storage.flush(record);

        assertNotNull("Storage should provide the Record",
                      storage.getRecord("Dummy", null));

        RecordReader reader = getStorageReader("foo", false);

        log.debug("Querying Storage");
        assertTrue("There should be at least one record in the Storage",
                   reader.hasNext());
        storage.close();
    }

    public void testSimpleRelatives() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);

        List<Record> input = getSampleData();

        log.debug("Adding Records to Storage");
        storage.flushAll(input);

        log.debug("Testing for existence of a Record");
        assertNotNull("Storage should provide the Record 'Middle'",
                      storage.getRecord("Middle", null));

        RecordReader reader = getStorageReader("foo", false);

        log.debug("Querying Storage with a reader");
        assertTrue("There should be at least one record in the Storage",
                   reader.hasNext());

        log.debug("Extracting all");
        List<Record> all = suck(reader);

        for (Record record: all) {
            log.debug("Extracted " + record);
        }

        log.debug("Counting");
        assertEquals("The number of extracted Records should match the input",
                     input.size(), all.size());
        reader.close(true);
        storage.close();
    }

    public void testImplicitRelatives() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);

        List<Record> input = getSampleData();
        input.remove(1); // Middle
        input.remove(1); // Child

        storage.flushAll(input);

        RecordReader reader = getStorageReader("foo", false);

        List<Record> all = suck(reader);

        for (Record record: all) {
            log.debug("Extracted " + record);
        }

        assertEquals("The number of extracted Records should match the input, "
                     + "including implicit",
                     input.size() + 2, all.size());

        Record parent = null;
        Record middleDirect = null;
        for (Record record: all) {
            if ("Parent".equals(record.getId())) {
                parent = record;
            }
            if ("Middle".equals(record.getId())) {
                middleDirect = record;
            }
        }
        assertNotNull("The parent should be located", parent);
        assertEquals("The parent should have the right number of children",
                     1, parent.getChildren().size());

        assertNotNull("The middle direct should be located", middleDirect);
        assertEquals("The middle direct should have the right number of "
                     + "children", 1, middleDirect.getChildren().size());
        assertEquals("The middle direct should have the right number of "
                     + "parents", 1, middleDirect.getParents().size());

        // Inferred from parent
        Record middleInferred = parent.getChildren().get(0);
        assertEquals("The middle inferred should have the right number of "
                     + "children", 1, middleInferred.getChildren().size());
        assertNotNull("The middle inferred should have parents",
                      middleInferred.getParents());
        assertEquals("The middle inferred should have the right number of "
                     + "parents", 1, middleInferred.getParents().size());

        reader.close(true);
        storage.close();
    }

/*  We do not flush parents
    public void testImplicitRelativesParent() throws Exception {

        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);

        List<Record> input = getSampleData();
        input.remove(0); // Parent
        input.remove(0); // Middle

        storage.flushAll(input);
        RecordReader reader = getStorageReader("foo", false);
        List<Record> all = suck(reader);

        for (Record record: all) {
            log.debug("Extracted " + record);
        }

        assertEquals("The number of extracted Records should match the input, "
                     + "including implicit",
                     input.size() + 2, all.size());
        reader.close(true);
        storage.close();
    }
    */
    public List<Record> suck(ObjectFilter filter) throws Exception {
        List<Record> result = new ArrayList<Record>(10);
        while (filter.hasNext()) {
            result.add(filter.next().getRecord());
        }
        return result;
    }

    // 5 records of which 3 are related to each other as Parent->Middle->Child
    private List<Record> getSampleData() {
        List<Record> records = new ArrayList<Record>(10);

        Record parentRecord = new Record("Parent", "foo", new byte[0]);
        Record middleRecord = new Record("Middle", "foo", new byte[0]);
        Record childRecord =  new Record("Child",  "foo", new byte[0]);
        parentRecord.setChildren(Arrays.asList(middleRecord));
        middleRecord.setChildren(Arrays.asList(childRecord));
        middleRecord.setParents(Arrays.asList(parentRecord));
        childRecord.setParents(Arrays.asList(middleRecord));
        records.add(parentRecord);
        records.add(middleRecord);
        records.add(childRecord);

        records.add(new Record("NoRelatives", "foo", new byte[0]));
        records.add(new Record("StillNoRelatives", "foo", new byte[0]));
        return records;
    }
    private RecordReader getStorageReader(String base, boolean alive)
                                                            throws IOException {
        MemoryStorage ms = new MemoryStorage();
        ms.put(RecordReader.CONF_START_FROM_SCRATCH, true);
        ms.put(StorageWatcher.CONF_POLL_INTERVAL, 500);
        ms.put(ConnectionConsumer.CONF_RPC_TARGET,
               "//localhost:28000/summa-storage");
        ms.put(RecordReader.CONF_STAY_ALIVE, alive);
        ms.put(RecordReader.CONF_BASE, base);
        ms.put(RecordReader.CONF_EXPAND_PARENTS, true);
        ms.put(RecordReader.CONF_EXPAND_CHILDREN, true);
        Configuration conf = new Configuration(ms);
        return new RecordReader(conf);
    }

    public void testStorageWatcher() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);

        RecordReader reader = getStorageReader("fagref", true);
        IndexTest.fillStorage(storage);
        assertTrue("The reader should have something", reader.hasNext());
        reader.pump();
        assertTrue("The reader should still have something", reader.hasNext());
        reader.close(true);
        storage.close();
    }

    public void testStorageScaleSmall() throws Exception {
        testStorageScale(10, 100, 1000);
    }

    public void testStorageScaleMedium() throws Exception {
        testStorageScale(5, 100, 100000);
    }


    /**
     * Create a Storage and fill it with dummy Records.
     * @param batches    the number of batch storing to perform.
     * @param records    the number of Records to create for each batch.
     * @param recordSize the size of the content in the Record. The content
     *                   is made up of random bits.
     * @throws Exception if the scale-test failed.
     */
    public void testStorageScale(int batches, int records, int recordSize)
            throws Exception {
        Random random = new Random(87);
        Storage storage = IndexTest.fillStorage();
        Profiler profiler = new Profiler(records);
        List<Record> recordList = new ArrayList<Record>(records);
        for (int batch = 0 ; batch < batches ; batch++) {
            log.debug(String.format(
                    "Running batch %d/%d with a total of %d MB",
                    batch+1, batches, records * recordSize / 1048576));
            recordList.clear();
            for (int i = 0 ; i < records ; i++) {
                byte[] content = new byte[recordSize];
                random.nextBytes(content);
                Record record = new Record("Dummy_" + i, "dummy", content);
                recordList.add(record);
                profiler.beat();
            }
            storage.flushAll(recordList);
        }
        log.info(String.format(
                "Ingested %d records of %d bytes in %s. Average speed: %s "
                + "records/second",
                records, recordSize,
                profiler.getSpendTime(), profiler.getBps(false)));
        storage.close();
        IngestTest.deleteOldStorages();
        log.info("Finished scale-test");
    }

    public void testRecordReader() throws Exception {
        Storage storage = IndexTest.fillStorage();

        MemoryStorage ms = new MemoryStorage();
        ms.put(RecordReader.CONF_START_FROM_SCRATCH, true);
        ms.put(ConnectionConsumer.CONF_RPC_TARGET,
               "//localhost:28000/summa-storage");
        ms.put(RecordReader.CONF_BASE, "fagref");
        Configuration conf = new Configuration(ms);

        RecordReader reader = new RecordReader(conf);
        reader.clearProgressFile();
        reader.close(false);

        reader = new RecordReader(conf);
        assertTrue("The reader should have something", reader.hasNext());
        int pumps = 0;
        while (reader.pump()) {
            log.trace("Pump #" + ++pumps + " completed");
        }
        log.debug("Pumped at total of " + pumps + " times");
        reader.close(false);

        reader = new RecordReader(conf);
        assertTrue("The second reader should have something", reader.hasNext());
        int newPumps = 0;
        while (reader.hasNext()) {
            Payload payload = reader.next();
            log.trace("Pump #" + ++newPumps + " completed. Got " + payload);
        }
        log.debug("newPumps was " + newPumps);
        assertEquals("Pump-round 1 & 2 should give the same number",
                     pumps, newPumps);
        reader.close(true);

        ms.put(RecordReader.CONF_START_FROM_SCRATCH, false);
        reader = new RecordReader(conf);
        int thirdPumps = 0;
        while (reader.hasNext()) {
            Payload payload = reader.next();
            log.trace("Pump #" + ++thirdPumps + " completed. Got " + payload);
        }
        reader.close(true);
        assertEquals("The third reader should pump nothing", 0, thirdPumps);

        storage.close();
    }

}




