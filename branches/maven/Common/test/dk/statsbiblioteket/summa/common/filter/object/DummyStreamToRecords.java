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
package dk.statsbiblioteket.summa.common.filter.object;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test-class that takes a stream and transforms received data into Records.
 */
public class DummyStreamToRecords extends TestCase implements ObjectFilter {
    private static final Log log =
            LogFactory.getLog(DummyStreamToRecords.class);

    public static final String CONF_DATA_SIZE = "dummystreamtorecords.datasize";

    private ObjectFilter source;
    private int dataSize = 100;
    private Record record;
    private static AtomicInteger idCounter = new AtomicInteger();

    public DummyStreamToRecords() {
        // for unit test purpose
    }

    public void testDummy() {
        assertTrue(true);
    }

    public DummyStreamToRecords(Configuration configuration) throws
                                                             RemoteException {
        log.trace("Constructing DummyStreamToRecords");
        dataSize = configuration.getInt(CONF_DATA_SIZE, dataSize);
        log.trace("DummyStreamToRecords uses data size " + dataSize);
    }

    public boolean hasNext() {
        try {
            refreshRecord();
        } catch (IOException e) {
            throw new RuntimeException("IOException when checking for next ");
        }
        return record != null;
    }

    public void setSource(Filter source) {
        if (source instanceof ObjectFilter) {
            this.source = (ObjectFilter)source;
        } else {
            throw new UnsupportedOperationException("Only ObjectFilter is legal"
                                                    + " as source");
        }
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

    public Payload next() {
        try {
            refreshRecord();
            if (record == null) {
                throw new NoSuchElementException("No next element");
            }
            Record newRecord = record;
            record = null;
            return new Payload(newRecord);
        } catch (IOException e) {
            throw new RuntimeException("IOException when requesting next "
                                       + "record", e);
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("No removal, okay?");
    }

    // Note: This ignores the length of the body's, which is okay for testing
    private void refreshRecord() throws IOException {
        if (record != null) {
            return;
        }

        ByteArrayOutputStream content = new ByteArrayOutputStream(dataSize);
        int value;
        if (source.hasNext()) {
            record = source.next().getRecord();
        } else {
            record = null;
        }
        /*    content.write(value);
            if (content.size() == dataSize) break;
        }
        if (content.size() > 0) {
            record = new Record("DummyRecord_" + idCounter.getAndIncrement(),
                                "Dummy", content.toByteArray());
        } else {
            record = null;
        } */
    }

    public void close(boolean success) {
        log.debug("Closing with success " + success);
        source.close(success);
    }

    public static int getIdCount() {
        return idCounter.get();
    }

    public static void clearIdCount() {
        idCounter.set(0);
    }
}


