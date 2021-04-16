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
package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.object.PayloadBufferFilter;
import dk.statsbiblioteket.summa.common.filter.object.PushFilter;
import dk.statsbiblioteket.summa.common.util.PayloadMatcher;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Files;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

/**
 * Unit tests for {@link ClearBaseFilter}
 */
public class ClearBaseFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(ClearBaseFilterTest.class);

    Storage storage;
    ClearBaseFilter filter;
    PayloadBufferFilter chain;

    public PayloadBufferFilter prepareFilterChain(ObjectFilter filter, Record... records) {
        // Set up the source filter
        PushFilter source = new PushFilter(records.length + 1, 2048);

        for (Record record : records) {
            source.add(new Payload(record));
        }
        source.signalEOF();

        // Set up the endpoint filter
        PayloadBufferFilter buf = new PayloadBufferFilter(Configuration.newMemoryBased());

        // Connect filters
        filter.setSource(source);
        buf.setSource(filter);

        return buf;

    }

    public void assertBaseCount(String base, long expected) throws Exception {
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> iter = new StorageIterator(storage, iterKey);
        long actual = 0;
        while (iter.hasNext()) {
            Record r = iter.next();
            if (!r.isDeleted()) {
                actual++;
            }
        }

        if (actual != expected) {
            fail("Base '" + base + "' should contain " + expected + " records, but found " + actual);
        }
    }

    public Storage createTestStorage() throws Exception {
        String dbLocation = "target" + File.separator + "summatest" + File.separator + "testDB";
        File dbFile = new File(dbLocation);
        if (dbFile.getParentFile().exists()) {
            Files.delete(dbFile.getParentFile());
        }

        storage = StorageFactory.createStorage(Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, dbLocation));

        assertBaseCount("base", 0);
        return storage;
    }

    /**
     * Test how we fare with no bases and no connection configured
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public void testNoBases() throws Exception {
        filter = new ClearBaseFilter(Configuration.newMemoryBased());
        chain = prepareFilterChain(filter, new Record("id", "base", "data".getBytes(StandardCharsets.UTF_8)));

        while (chain.pump()) {
        }

        assertEquals(1, chain.size());
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void testOneBaseOneRecord() throws Exception {
        createTestStorage();

        Record rec = new Record("id", "base", "data".getBytes(StandardCharsets.UTF_8));
        storage.flush(rec);
        assertBaseCount("base", 1);

        filter = new ClearBaseFilter(storage, Arrays.asList("base"));
        chain = prepareFilterChain(filter, rec);

        while (chain.pump()) { }

        assertEquals(1, chain.size());

        assertBaseCount("base", 0);
    }

    public void testOneBaseTwoRecords() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base", "data".getBytes(StandardCharsets.UTF_8));
        Record rec2 = new Record("id2", "base", "data".getBytes(StandardCharsets.UTF_8));
        storage.flush(rec1);
        storage.flush(rec2);
        assertBaseCount("base", 2);

        filter = new ClearBaseFilter(storage, Arrays.asList("base"));
        chain = prepareFilterChain(filter, rec1, rec2);

        //noinspection StatementWithEmptyBody
        while (chain.pump()) { }

        assertEquals(2, chain.size());

        assertBaseCount("base", 0);
    }

    public void testEpoch() throws Exception {
        Calendar cal = Calendar.getInstance();
        //     2009-08-31 16:09:32 => 1251727772660 
        cal.set(Calendar.YEAR, 2009);
        cal.set(Calendar.MONTH, Calendar.AUGUST);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        cal.set(Calendar.HOUR_OF_DAY, 16);
        cal.set(Calendar.MINUTE, 9);
        cal.set(Calendar.SECOND, 32);
        log.info(String.format(Locale.ROOT, ProgressTracker.TIMESTAMP_FORMAT, cal));
    }

    public void testPayloadMatcher() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base", "data".getBytes(StandardCharsets.UTF_8));
        Record rec11 = new Record("id11", "base", "data".getBytes(StandardCharsets.UTF_8));
        Record rec2 = new Record("id2", "base", "data".getBytes(StandardCharsets.UTF_8));
        storage.flush(rec1);
        storage.flush(rec11);
        storage.flush(rec2);
        assertBaseCount("base", 3);


        Configuration conf = Configuration.newMemoryBased(
                PayloadMatcher.CONF_ID_REGEX, "id2",
                ClearBaseFilter.CONF_CLEAR_BASES, "base"
        );

        filter = new ClearBaseFilter(storage, conf);
        chain = prepareFilterChain(filter, rec1, rec11, rec2);

        assertEquals("The first pumped record should match", "id1", chain.next().getId());
        assertEquals("The chain should contain the right number of Payloads", 1, chain.size());
        assertBaseCount("base", 3);
        chain.pump();
        chain.pump();
        assertBaseCount("base", 0);
    }
}