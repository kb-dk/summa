package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.object.PayloadBufferFilter;
import dk.statsbiblioteket.summa.common.filter.object.PushFilter;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.summa.storage.api.filter.RecordWriter;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Files;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 *  Unit tests for {@link UpdateFromFulldumpFilter}
 */
public class UpdateFromFulldumpFilterTest  extends TestCase {
    Storage storage;
    UpdateFromFulldumpFilter filter;
    PayloadBufferFilter chain;

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if(storage != null) {
            storage.close();
        }
    }

    public PayloadBufferFilter prepareFilterChain(ObjectFilter fullDumpFilter,
            Record... records) throws IOException {
        // Set up the source filter
        PushFilter source = new PushFilter(records.length+1, 2048);

        for (Record record : records) {
            Payload p = new Payload(record);
            source.add(p);
        }
        source.signalEOF();

        fullDumpFilter.setSource(source);

        RecordWriter writer = new RecordWriter(storage, 100, 10000);
        writer.setSource(fullDumpFilter);

        // Set up the endpoint filter
        PayloadBufferFilter buf = new PayloadBufferFilter(
                                                Configuration.newMemoryBased());

        // Connect filters
        buf.setSource(writer);
        
        return buf;
    }

    public void assertBaseCount(String base, long expected) throws Exception {
        long iterKey = storage.getRecordsModifiedAfter(0, base, null);
        Iterator<Record> iter = new StorageIterator(storage, iterKey);
        long actual = 0;
        while (iter.hasNext()) {
            Record r = iter.next();
            System.out.println("Record: '" + r.getId() + "', delete: " + r.isDeleted());
            if (!r.isDeleted()) {
                actual++;
            }
        }

        assertEquals("The base '" + base +"' should contain the right "
                     + "number of records", expected, actual);
    }

    public Storage createTestStorage() throws Exception {
        String dbLocation = "summatest" + File.separator + "testDB";
        File dbFile = new File(dbLocation);
        if (dbFile.getParentFile().exists()) {
            Files.delete(dbFile.getParentFile());
        }
        assertFalse("The DB should be removed",
                    dbFile.getParentFile().exists());

        storage = StorageFactory.createStorage(Configuration.newMemoryBased(
                Storage.CONF_CLASS, H2Storage.class,
                DatabaseStorage.CONF_LOCATION, dbLocation,
                DatabaseStorage.CONF_CREATENEW, true,
                DatabaseStorage.CONF_FORCENEW, true));
        storage.clearBase("base");
        
        assertBaseCount("base", 0);
        return storage;
    }

    /**
     * Test how we fare with no bases and no connection configured
     * @throws Exception if thrown
     */
    public void testNoBases() throws Exception {
        Storage storage = createTestStorage();
        filter = new UpdateFromFulldumpFilter(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base"));
        chain = prepareFilterChain(
                filter, new Record("id", "base", "data".getBytes()));
        assertBaseCount("base", 0);
        
        while(chain.pump()) {
            // intended
        }
        chain.close(true);

        assertEquals(1, chain.size());        

        assertBaseCount("base", 1);
    }

    public void testInsertExtraRecord() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base", "data".getBytes());
        Record rec2 = new Record("id2", "base", "data".getBytes());
        storage.flush(rec1);
        assertBaseCount("base", 1);

        filter = new UpdateFromFulldumpFilter(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base"));
        chain = prepareFilterChain(filter, rec1, rec2);

        while(chain.pump()) {
            // intended
        }
        chain.close(true);  // why isn't this called via pump?

        assertEquals(2, chain.size());
        assertBaseCount("base", 2);
    }

    public void testSameFullDump() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base", "data".getBytes());
        Record rec2 = new Record("id2", "base", "data".getBytes());
        storage.flush(rec1);
        storage.flush(rec2);
        assertBaseCount("base", 2);

        filter = new UpdateFromFulldumpFilter(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base"));
        chain = prepareFilterChain(filter, rec1, rec2);

        //noinspection StatementWithEmptyBody
        while(chain.pump()) {
            // intended
        }
        chain.close(true); // why isn't this called via pump?
        
        assertEquals(2, chain.size());

        assertBaseCount("base", 2);
    }

    public void testMissingInDump() throws Exception {
        createTestStorage();

        Record rec1 = new Record("id1", "base", "data".getBytes());
        Record rec2 = new Record("id2", "base", "data".getBytes());
        storage.flush(rec1);
        storage.flush(rec2);

        assertBaseCount("base", 2);

        filter = new UpdateFromFulldumpFilter(
                storage, Configuration.newMemoryBased(
                        UpdateFromFulldumpFilter.CONF_BASE, "base"));
        chain = prepareFilterChain(filter, rec1);

        //noinspection StatementWithEmptyBody
        while(chain.pump()) {
            // intended
        }
        chain.close(true); // why isn't this called via pump?

        assertEquals(1, chain.size());

        assertBaseCount("base", 1);
    }
    

}
