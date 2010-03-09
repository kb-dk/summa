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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.QueryOptions;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Profiler;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * A small test tool to assess the performance of a Storage impl.
 */
public class StorageStressTest extends TestCase {

    public static final Class<? extends Storage> DEFAULT_STORAGE =
                                                             H2Storage.class;

    public static final String CONF_NUM_RECORDS =
                                                "summa.storage.test.numrecords";

    public static final long DEFAULT_NUM_RECORDS = 10000;

    public static final String DEFAULT_DB_LOCATION =
                                            "/tmp/summa-storage-stress-test/db";

    public static void main (String[] args) throws Exception {
        Configuration conf = Configuration.getSystemConfiguration(true);

        if (!conf.valueExists(DatabaseStorage.CONF_LOCATION)) {
            conf.set(DatabaseStorage.CONF_LOCATION, DEFAULT_DB_LOCATION);
        }

        if (new File(conf.getString(DatabaseStorage.CONF_LOCATION)).exists()) {
            System.err.println("Database '"
                               +conf.getString(DatabaseStorage.CONF_LOCATION)
                               +"' already exist. Please delete it.");
            System.exit(1);
        }

        if (!conf.valueExists(Storage.CONF_CLASS)) {
            conf.set (Storage.CONF_CLASS, DEFAULT_STORAGE);
        }

        System.out.println("Using storage class: "
                           + conf.getClass(Storage.CONF_CLASS));
        Storage storage = StorageFactory.createStorage(conf);

        long numRecs = conf.getLong(CONF_NUM_RECORDS, DEFAULT_NUM_RECORDS);

        System.out.println("------------ Write test");
        StorageStressTest.stressFlush(storage, numRecs);

        System.out.println("------------ Read test");
        StorageStressTest.stressRead(storage, numRecs,
                                     0, TestRecordFactory.recordBase, null);

        System.out.println("------------ Closing");
        storage.close();
    }

    public void testDummy() {
        assertTrue(true);
    }

    public static void stressFlush(Storage storage, long numRecs)
                                                            throws IOException {
        System.out.println("Flushing " + numRecs + " records");
        Profiler p = new Profiler();
        p.setExpectedTotal(numRecs);

        for (long i = 0; i < numRecs; i++) {
            storage.flush(TestRecordFactory.next());
            p.beat();
        }
        double bps = p.getBps();
        long runningTime = p.getSpendMilliseconds();

        System.out.println("Done flushing " + numRecs + " records");
        System.out.println("Total running time: "
                           + Profiler.millisecondsToString(runningTime));
        System.out.println("Average: " + bps + " records/s");
    }

    public static void stressRead(Storage storage, long numRecs,
                                  long timeStamp, String base,
                                  QueryOptions options) throws IOException {
        System.out.println("Reading " + numRecs + " records");

        long iteratorLookupTime = System.currentTimeMillis();
        long iterHandle = storage.getRecordsModifiedAfter(timeStamp,
                                                          base, options);
        System.out.println("Iterator lookup time: "
                           + (System.currentTimeMillis() - iteratorLookupTime)
                           + "ms");
        StorageIterator iter = new StorageIterator(storage, iterHandle);

        Profiler p = new Profiler();
        p.setExpectedTotal(numRecs);

        long numRead;
        for (numRead = 0; numRead < numRecs; numRead++) {
            if (iter.hasNext()) {
                iter.next();
            } else {
                System.out.println("!! Iterator depleted prematurely after "
                                   + numRead + " records. Expected " + numRecs);
                break;
            }
            p.beat();
        }
        double bps = p.getBps();
        long runningTime = p.getSpendMilliseconds();

        System.out.println("Done reading " + numRead + " records");
        System.out.println("Total running time: "
                           + Profiler.millisecondsToString(runningTime));
        System.out.println("Average: " + bps + " records/s");
    }
}

