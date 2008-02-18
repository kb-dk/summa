/**
 * Created: te 18-02-2008 22:42:55
 * CVS:     $Id:$
 */
package dk.statsbiblioteket.summa.ingest.records;

import junit.framework.TestCase;
import junit.framework.Test;
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
        int  BODY_COUNT =  3;
        int RECORD_SIZE = 55;

    }

}
