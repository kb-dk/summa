/**
 * Created: te 18-02-2008 22:17:40
 * CVS:     $Id$
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
import dk.statsbiblioteket.summa.common.filter.stream.StreamFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test-class that takes a stream and transforms received data into Records.
 */
public class DummyStreamToRecords implements ObjectFilter {
    private static final Log log =
            LogFactory.getLog(DummyStreamToRecords.class);

    public static final String CONF_DATA_SIZE = "DummyStreamToRecords.DataSize";

    private StreamFilter source;
    private int dataSize = 100;
    private Record record;
    private static AtomicInteger idCounter = new AtomicInteger();

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
        if (source instanceof StreamFilter) {
            this.source = (StreamFilter)source;
        } else {
            throw new UnsupportedOperationException("Only StreamFilter is legal"
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
        while ((value = source.read()) != StreamFilter.EOF) {
            content.write(value);
            if (content.size() == dataSize) break;
        }
        if (content.size() > 0) {
            record = new Record("DummyRecord_" + idCounter.getAndIncrement(),
                                "Dummy", content.toByteArray());
        } else {
            record = null;
        }
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