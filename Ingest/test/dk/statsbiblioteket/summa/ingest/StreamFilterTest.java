/**
 * Created: te 18-02-2008 23:30:32
 * CVS:     $Id:$
 */
package dk.statsbiblioteket.summa.ingest;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;

public class StreamFilterTest extends TestCase {

    // Used for testing the long to bytes conversion
    class LongFilter extends StreamFilter {
        public void setSource(StreamFilter source) {
            // Do Nothing
        }
        public void close(boolean success) {
            // Do nothing
        }

        ArrayList<Integer> content;

        /**
         * Set content to the bytes making up the given long.
         * @param value a long value to store in the content array.
         */
        protected void setLong(long value) {
            content = new ArrayList<Integer>( 8);
            byte[] theLong = longToBytes(value);
            for (byte l: theLong) {
                content.add(0xff & l);
            }
        }

        public int read() throws IOException {
            return content.remove(0);
        }
    }

    public void testLongs() throws Exception {
        LongFilter lf = new LongFilter();
        long[] testValues = new long[]{1, 127, 128, 255, 256, 32766, 32767,
                                       32768, 65535, 65536, Long.MAX_VALUE,
                                       Long.MIN_VALUE, -1, -127, -128, -129,
                                       -255, -256, -32768, -32767, -32769};
        for (long l: testValues) {
            lf.setLong(l);
            assertEquals("The stored long should be retrievable", 
                         l, lf.readLong());
        }
    }
}
