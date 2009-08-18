package dk.statsbiblioteket.summa.storage.api.filter;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.PayloadBufferFilter;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.object.PushFilter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Files;

import java.io.IOException;
import java.io.File;
import java.util.Iterator;
import java.util.Arrays;

/**
 * Unit tests for {@link ClearBaseFilter}
 */
public class ClearBaseFilterTest extends TestCase {

    Storage storage;
    ClearBaseFilter filter;
    PayloadBufferFilter chain;

    public PayloadBufferFilter prepareFilterChain(ObjectFilter filter,
                                                  Record... records) {
        // Set up the source filter
        PushFilter source = new PushFilter(records.length+1, 2048);

        for (int i = 0; i < records.length; i++) {
            Payload p = new Payload(records[i]);
            source.add(p);
        }
        source.signalEOF();

        // Set up the endpoint filter
        PayloadBufferFilter buf = new PayloadBufferFilter(
                                                Configuration.newMemoryBased());

        // Connect filters
        filter.setSource(source);
        buf.setSource(filter);

        return buf;

    }

    public void assertBaseCount (String base, long expected) throws Exception {
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
            fail("Base '" + base + "' should contain " + expected
                 + " records, but found " + actual);
        }
    }

    public Storage createTestStorage() throws Exception {
        String dbLocation = "summatest" + File.separator + "testDB";
        File dbFile = new File(dbLocation);
        if (dbFile.getParentFile().exists()) {
            Files.delete(dbFile.getParentFile());
        }

        storage = StorageFactory.createStorage(
                        Configuration.newMemoryBased(
                                   Storage.CONF_CLASS, H2Storage.class,
                                   DatabaseStorage.CONF_LOCATION,
                                   dbLocation));

        assertBaseCount("base", 0);
        return storage;
    }

    /**
     * Test how we fare with no bases and no connection configured
     */
    public void testNoBases() throws Exception {
        filter = new ClearBaseFilter(Configuration.newMemoryBased());
        chain = prepareFilterChain(filter,
                                   new Record("id", "base", "data".getBytes()));

        while(chain.pump());

        assertEquals(1, chain.size());
    }

    public void testOneBaseOneRecord() throws Exception {
        createTestStorage();
        
        Record rec = new Record("id", "base", "data".getBytes());
        storage.flush(rec);
        assertBaseCount("base", 1);

        filter = new ClearBaseFilter(storage, Arrays.asList("base"));
        chain = prepareFilterChain(filter,
                                   rec);

        while(chain.pump());

        assertEquals(1, chain.size());

        assertBaseCount("base", 0);
    }

    public void testOneBaseTwoRecords() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base", "data".getBytes());
        Record rec2 = new Record("id2", "base", "data".getBytes());
        storage.flush(rec1);
        storage.flush(rec2);
        assertBaseCount("base", 2);

        filter = new ClearBaseFilter(storage, Arrays.asList("base"));
        chain = prepareFilterChain(filter,
                                   rec1, rec2);

        while(chain.pump());

        assertEquals(2, chain.size());

        assertBaseCount("base", 0);
    }
}

