package dk.statsbiblioteket.summa.storage.filter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.storage.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseControl;
import dk.statsbiblioteket.summa.storage.io.Control;
import dk.statsbiblioteket.util.Files;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class RecordWriterTest extends TestCase {
    public RecordWriterTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        if (storageLocation.exists()) {
            Files.delete(storageLocation);
        }
        assertTrue("Storage location '" + storageLocation
                   + "' should be created", storageLocation.mkdirs());
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(RecordWriterTest.class);
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    private static final File storageLocation =
            new File(System.getProperty("java.io.tmpdir"), "kabloey");

    public void testWrite() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DatabaseControl.PROP_LOCATION, storageLocation.toString());
        Control control = StorageFactory.createController(conf);
        assertNotNull("A controller should be available now", control);
    }


    /* ObjectFilter test-implementation */
    class ObjectProvider implements ObjectFilter {
        List<Record> records;

        public ObjectProvider(int objectCount) {
            records = new ArrayList<Record>(objectCount);
            for (int i = 0 ; i < objectCount ; i++) {
                records.add(new Record("Dummy-" + i, "fooBase", new byte[10]));
            }
        }


        public boolean hasNext() {
            return records.size() > 0;
        }

        public Payload next() {
            if (!hasNext()) {
                //noinspection DuplicateStringLiteralInspection
                throw new NoSuchElementException("No more Records");
            }
            return new Payload(records.remove(0));
        }

        public void remove() {
            if (!hasNext()) {
                //noinspection DuplicateStringLiteralInspection
                throw new NoSuchElementException("No more Records");
            }
            records.remove(0);
        }

        public void setSource(Filter filter) {
            // Do nothing
        }

        public boolean pump() throws IOException {
            if (!hasNext()) {
                return false;
            }
            Payload next = next();
            if (next == null) {
                return false;
            }
            next.close();
            return true;
        }

        public void close(boolean success) {
            records.clear();
        }
    }
}
