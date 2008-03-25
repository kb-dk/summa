/**
 * Created: te 18-02-2008 22:17:40
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.ingest.records;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicInteger;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.ingest.stream.StreamFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test-class that takes a stream and transforms received data into Records.
 */
public class DummyStreamToRecords implements RecordFilter {
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

    public boolean hasMoreRecords() throws IOException {
        refreshRecord();
        return record != null;
    }

    public void setSource(StreamFilter source) {
        this.source = source;
    }

    public void setSource(RecordFilter source) {
        throw new UnsupportedOperationException("Only StreamFilter as source");
    }

    public Record getNextRecord() throws IOException {
        refreshRecord();
        return record;
    }

    // Note: This ignores the length of the body's, which is okay for testing
    private void refreshRecord() throws IOException {
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
