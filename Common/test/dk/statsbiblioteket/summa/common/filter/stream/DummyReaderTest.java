package dk.statsbiblioteket.summa.common.filter.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.filter.Payload;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * DummyReader Tester.
 *
 * @author <Authors name>
 * @since <pre>02/18/2008</pre>
 * @version 1.0
 */
public class DummyReaderTest extends TestCase {
    public DummyReaderTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(DummyReaderTest.class);
    }

    public void testBasics() throws Exception {
        long BODY_SIZE = 100;
        int  BODY_COUNT =  3;

        MemoryStorage memStore = new MemoryStorage();
        memStore.put(DummyReader.CONF_BODY_COUNT, BODY_COUNT);
        memStore.put(DummyReader.CONF_BODY_SIZE, BODY_SIZE);
        Configuration conf = new Configuration(memStore);
        DummyReader dummy = new DummyReader(conf);

        for (int b = 0 ; b < BODY_COUNT ; b++) {
            long size = BODY_SIZE;
            int[] sizeArray = new int[8];
            for (int i = 7 ; i >= 0 ; i--) {
                sizeArray[i] = (byte)size;
                size >>>= 8;
            }

            for (int l: sizeArray) {
                assertEquals("The length-bytes should match", l, dummy.read());
            }
            for (int i = 0 ; i < BODY_SIZE ; i++) {
                assertTrue("Content must not be EOF",
                           dummy.read() != Payload.EOF);
            }
        }
        assertEquals("Last value must be EOF", Payload.EOF, dummy.read());
    }
}


