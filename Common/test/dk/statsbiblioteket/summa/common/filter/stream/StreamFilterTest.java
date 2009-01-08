package dk.statsbiblioteket.summa.common.filter.stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.util.BitUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * StreamFilter Tester.
 *
 * @author <Authors name>
 * @since <pre>03/26/2008</pre>
 * @version 1.0
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class StreamFilterTest extends TestCase {
    public StreamFilterTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(StreamFilterTest.class);
    }

    class LongFilter extends StreamFilter {
        public void setSource(StreamFilter source) {
            // Do Nothing
        }

        public void setSource(Filter filter) {
        }

        public boolean pump() throws IOException {
            return false;
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
            byte[] theLong = BitUtil.longToBytes(value);
            for (byte l: theLong) {
                content.add(0xff & l);
            }
        }

        public int read() throws IOException {
            return content.remove(0);
        }

        public boolean hasNext() {
            throw new UnsupportedOperationException("Just a dummy");
        }
        public Payload next() {
            throw new UnsupportedOperationException("Just a dummy");
        }
        public void remove() {
            throw new UnsupportedOperationException("Just a dummy");
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
    public void testMetaInfo(int contentLength) throws Exception {
        byte[] content = new byte[contentLength];
        new Random().nextBytes(content);
        ByteArrayInputStream contentStream = new ByteArrayInputStream(content);
        StreamFilter.MetaInfo meta = new StreamFilter.MetaInfo("foo",
                                                               content.length);
        InputStream sequence = meta.appendHeader(contentStream);

        StreamFilter.MetaInfo extractedMeta =
                StreamFilter.MetaInfo.getMetaInfo(sequence);
        assertEquals("The extracted id should be correct",
                     "foo", extractedMeta.getId());
        assertEquals("The extracted content length should match",
                     content.length, extractedMeta.getContentLength());
        for (int i = 0 ; i < extractedMeta.getContentLength() ; i++) {
            assertEquals("The byte at position " + i + " should match",
                         content[i], (byte)sequence.read());
        }
        assertEquals("EOF should be reached after the content",
                     Payload.EOF, sequence.read());
    }

    public void testMetaInfo() throws Exception {
        testMetaInfo(100);
        testMetaInfo(0);
    }
}



