/**
 * Created: te 18-02-2008 22:42:55
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.ingest.records;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.ingest.stream.DummyReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DummyStreamToRecordsTest extends TestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(DummyStreamToRecordsTest.class);
    }

    public void testBasics() throws Exception {
        long BODY_SIZE = 100;
        int BODY_COUNT = 3;
        int DATA_SIZE = 51;

        int RECORDS = 7;
        Configuration conf = Configuration.newMemoryBased();
        conf.set(DummyReader.CONF_BODY_SIZE, BODY_SIZE);
        conf.set(DummyReader.CONF_BODY_COUNT, BODY_COUNT);
        conf.set(DummyStreamToRecords.CONF_DATA_SIZE, DATA_SIZE);

        DummyReader reader = new DummyReader(conf);
        DummyStreamToRecords recorder = new DummyStreamToRecords(conf);
        recorder.setSource(reader);
        for (int i = 0 ; i < RECORDS ; i++) {
            assertNotNull("We should be able to get Record " + (i+1),
                          recorder.getNextRecord());
        }
        assertNull("After depleting, the next record should be null",
                   recorder.getNextRecord());
        assertNull("After depleting and getting next record, the next record "
                   + "should still be null",
                   recorder.getNextRecord());
    }

}
