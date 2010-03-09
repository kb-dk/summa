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
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.Iterator;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.storage.StorageTestBase
 *
 * @author mke
 * @since Jan 11, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class StorageTestBase extends TestCase {

    protected static Log log = LogFactory.getLog(JavascriptBatchJobTest.class);

    protected Storage storage;

    protected static String testDBRoot = "test_db";
    protected static String dbPrefix = "db";
    protected static String testBase1 = "foobar";
    protected static String testBase2 = "frobnibar";
    protected static String testId1 = "testId1";
    protected static String testId2 = "testId2";
    protected static String testId3 = "testId3";
    protected static String testId4 = "testId4";
    protected static int storageCounter = 0;
    protected static byte[] testContent1 = new byte[] {'s', 'u', 'm', 'm', 'a'};
    protected static byte[] testContent2 = new byte[] {'b', '0', 'r', 'k'};
    protected long testStartTime;

    protected static String lastStorageLocation = null;

    public static Configuration createConf () throws Exception {

        lastStorageLocation =
                testDBRoot + File.separator + dbPrefix + (storageCounter++);
        // H2 Config
        Configuration conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, lastStorageLocation
        );

        // Derby Config
        /*Configuration conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS,
                DerbyStorage.class,
                DatabaseStorage.CONF_LOCATION,
                testDBRoot + File.separator + dbPrefix + (storageCounter++)
        );*/

        // Postgres Config
        /*Configuration conf = Configuration.newMemoryBased(
                Storage.CONF_CLASS,
                PostgresStorage.class,
                DatabaseStorage.CONF_LOCATION,
                testDBRoot + File.separator + dbPrefix + (storageCounter++),
                DatabaseStorage.CONF_FORCENEW,
                true,
                DatabaseStorage.CONF_DATABASE,
                "summa",
                DatabaseStorage.CONF_USERNAME,
                "${user.name}",
                DatabaseStorage.CONF_PASSWORD,
                "",
                DatabaseStorage.CONF_HOST,
                ""
        );*/

        return conf;
    }

    public void testDummy() {
        assertTrue(true);
    }

    public void setUp () throws Exception {
        File dbRoot = new File(testDBRoot);

        if (dbRoot.exists()) {
            Files.delete (dbRoot);
        }

        storage = StorageFactory.createStorage(createConf());

        /* We get spurious errors where the connection to the db isn't ready
         * when running the unit tests in batch mode */
        Thread.sleep(200);

        testStartTime = System.currentTimeMillis();
    }

    public void tearDown () throws Exception {
        log.info("Test case tear down commencing");

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

    public void assertBaseEmpty (String base) throws Exception {
        assertBaseEmpty(base, -1);
    }

    public void assertBaseEmpty (String base, long count) throws Exception {
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> iter = new StorageIterator(storage, iterKey);
        long nonDeletedCount = 0;
        long fullCount = 0;
        while (iter.hasNext()) {
            Record r = iter.next();
            fullCount++;
            if (!r.isDeleted()) {
                nonDeletedCount++;
            }
        }

        if (nonDeletedCount != 0) {
            fail ("Base '" + base + "' should be empty, but found " + nonDeletedCount
                  + " records");
        }

        if (count != -1) {
            if (count != fullCount) {
                fail("Expected " + count
                      + " records in base, found " + fullCount);
            }
        }
    }

    public void assertBaseCount (String base, long expected) throws Exception {
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> iter = new StorageIterator(storage, iterKey);
        long actual = 0;
        while (iter.hasNext()) {
            iter.next();
            actual++;
        }

        if (actual != expected) {
            fail("Base '" + base + "' should contain " + expected
                 + " records, but found " + actual);
        }
    }
}

