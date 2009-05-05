/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.storage.api.filter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Iterator;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.storage.api.StorageFactory;
import dk.statsbiblioteket.summa.storage.database.DatabaseStorage;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.summa.storage.api.StorageIterator;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordWriterTest extends TestCase {

    Log log = LogFactory.getLog(RecordWriterTest.class);

    private static final File storageLocation =
            new File(System.getProperty("java.io.tmpdir"), "kabloey");

    Storage storage;
    RecordWriter writer;

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

        Configuration conf = Configuration.newMemoryBased();
        Files.delete(storageLocation);
        conf.set(Storage.CONF_CLASS,
                 "dk.statsbiblioteket.summa.storage.database.h2.H2Storage");
        conf.set(DatabaseStorage.CONF_LOCATION, storageLocation.toString());
        storage = StorageFactory.createStorage(conf);
        assertNotNull("A storage should be available now", storage);

        writer = new RecordWriter(storage, 100, 1000);

        // We need to not emit interrupt() on the DB thread before it is ready
        Thread.sleep(200);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(RecordWriterTest.class);
    }

    public void testFewRecords() throws Exception {
        writer.setSource(new ObjectProvider(5));
        while (writer.pump()) {
            // Wait
        }
        writer.close(true);
        assertBaseCount("fooBase", 5);
    }

    public void testBatchSize() throws Exception {
        writer.setSource(new ObjectProvider(100));
        while (writer.pump()) {
            // Wait
        }
        writer.close(true);
        assertBaseCount("fooBase", 100);
    }

    public void testBatchOvershoot() throws Exception {
        writer.setSource(new ObjectProvider(100007));
        while (writer.pump()) {
            // Wait
        }
        writer.close(true);
        log.info("Flushed records. Checking count");
        assertBaseCount("fooBase", 100007);
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



