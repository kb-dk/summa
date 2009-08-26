/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.releasetest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.watch.StorageWatcher;
import dk.statsbiblioteket.summa.storage.api.filter.RecordReader;
import dk.statsbiblioteket.summa.storage.StorageMonkeyHelper;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.unittest.NoExitTestCase;

import java.io.StringWriter;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

/**
 * te forgot to document this class.
 */
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

    public void testStorageWatcher() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);

        MemoryStorage ms = new MemoryStorage();
        ms.put(RecordReader.CONF_START_FROM_SCRATCH, true);
        ms.put(StorageWatcher.CONF_POLL_INTERVAL, 500);
        ms.put(ConnectionConsumer.CONF_RPC_TARGET,
               "//localhost:28000/summa-storage");
        ms.put(RecordReader.CONF_STAY_ALIVE, true);
        ms.put(RecordReader.CONF_BASE, "fagref");
        Configuration conf = new Configuration(ms);

        RecordReader reader = new RecordReader(conf);
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

    public void testSmallMonkey() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);
        StorageMonkeyHelper monkey = new StorageMonkeyHelper(
                0, 1000000, 0.01, 0.02, null, null, 0, 5, 0, 50);
        monkey.monkey(10, 5, 2, 10, 2, 1, 100);
        storage.close();
    }

    public void testCharMonkey() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);

        StringWriter chars = new StringWriter(65535);
        for (char c = 0 ; c < 65535 ; c++) {
            chars.append(c);
        }
        StorageMonkeyHelper monkey = new StorageMonkeyHelper(
                0, 1000000, 0.01, 0.02, null, null, 0, 5, 0, 50);
        monkey.monkey(10, 5, 2, 10, 2, 1, 100);
        storage.close();
    }

    public void testMediumMonkey() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);
        StorageMonkeyHelper monkey = new StorageMonkeyHelper(
                0, 1000000, 0.01, 0.02, null, null, 0, 5, 0, 50);
        monkey.monkey(1000, 200, 100, 1000, 5, 1, 100);
        storage.close();
    }

    public void testUpdateMonkey() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);
        StorageMonkeyHelper monkey = new StorageMonkeyHelper(
                0, 1000000, 0.01, 0.02, null, null, 0, 5, 0, 50);
        monkey.monkey(1000, 2000, 100, 1000, 5, 1, 100);
        storage.close();
    }

    public void disabledtestLargeMonkey() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);
        StorageMonkeyHelper monkey = new StorageMonkeyHelper(
                0, 1000000, 0.01, 0.02, null, null, 0, 5, 0, 50);
        monkey.monkey(10000, 2000, 1000, 1000, 5, 1, 100);
        storage.close();
    }

    public void testPauseResume() throws Exception {
        Configuration storageConf = IngestTest.getStorageConfiguration();
        Storage storage = StorageFactory.createStorage(storageConf);
        StorageMonkeyHelper monkey = new StorageMonkeyHelper(
                50, 2000, 0.01, 0.02, null, null, 0, 5, 0, 50);
        List<StorageMonkeyHelper.Job> primaryJobs =
                monkey.createJobs(10000, 0, 0, 10000, 100, 100);
        log.info("Handling primary jobs");
        monkey.doJobs(primaryJobs, 1);
        log.info("Sleeping 10 seconds");
        Thread.sleep(10 * 1000);
        log.info("Handling secondary jobs");
        List<StorageMonkeyHelper.Job> secondaryJobs =
                monkey.createJobs(1000, 0, 0, 1000, 100, 100);
        monkey.doJobs(secondaryJobs, 1);
        log.info("Finished");
        storage.close();
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



