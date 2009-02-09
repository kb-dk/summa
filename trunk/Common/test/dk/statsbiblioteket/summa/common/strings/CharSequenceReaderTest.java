package dk.statsbiblioteket.summa.common.strings;

import junit.framework.TestCase;

import java.io.Reader;
import java.io.IOException;

/**
 * Unit tests for the {@link CharSequenceReader}
 */
public class CharSequenceReaderTest extends TestCase {

    StringBuffer buf;
    CharSequenceReader seq;

    public void setUp() {
        buf = new StringBuffer();
    }

    public void testReadManySingleChar() throws Exception {
        seq = new CharSequenceReader(buf.append("foobar"));
        assertEquals("foobar", readFullySingleChar(seq));
    }

    public void testReadManySmallArray() throws Exception {
        seq = new CharSequenceReader(buf.append("foobar"));
        assertEquals("foobar", readFullySmallArray(seq));
    }

    public void testReadManyBigArray() throws Exception {
        seq = new CharSequenceReader(buf.append("foobar"));
        assertEquals("foobar", readFullyBigArray(seq));
    }

    public void testReadMoreSingleChar() throws Exception {
        seq = new CharSequenceReader(buf.append("autobiografia"));
        assertEquals("autobiografia", readFullySingleChar(seq));
    }

    public void testReadMoreSmallArray() throws Exception {
        seq = new CharSequenceReader(buf.append("autobiografia"));
        assertEquals("autobiografia", readFullySmallArray(seq));
    }

    public void testReadMoreBigArray() throws Exception {
        seq = new CharSequenceReader(buf.append("autobiografia"));
        assertEquals("autobiografia", readFullyBigArray(seq));
    }

    public void testReadSingleSingleChar() throws Exception {
        seq = new CharSequenceReader(buf.append("f"));
        assertEquals("f", readFullySingleChar(seq));
    }

    public void testReadSingleSmallArray() throws Exception {
        seq = new CharSequenceReader(buf.append("f"));
        assertEquals("f", readFullyBigArray(seq));
    }

    public void testReadSingleBigArray() throws Exception {
        seq = new CharSequenceReader(buf.append("f"));
        assertEquals("f", readFullyBigArray(seq));
    }

    public void testReadEmptySingleChar() throws Exception {
        seq = new CharSequenceReader(buf.append(""));
        assertEquals("", readFullySingleChar(seq));
    }

    public void testReadEmptySmallArray() throws Exception {
        seq = new CharSequenceReader(buf.append(""));
        assertEquals("", readFullyBigArray(seq));
    }

    public void testReadEmptyBigArray() throws Exception {
        seq = new CharSequenceReader(buf.append(""));
        assertEquals("", readFullyBigArray(seq));
    }

    public static String readFullySmallArray(Reader r) throws IOException {
        int len;
        char[] a = new char[5];
        StringBuffer tmp = new StringBuffer();

        while ((len = r.read(a)) != -1) {
            tmp.append(a, 0, len);
        }

        return tmp.toString();
    }

    public static String readFullySingleChar(Reader r) throws IOException {
        int val;
        StringBuffer tmp = new StringBuffer();

        while ((val = r.read()) != -1) {
            tmp.append((char)val);
        }

        return tmp.toString();
    }

    public static String readFullyBigArray(Reader r) throws IOException {
        int len;
        char[] a = new char[1024];
        StringBuffer tmp = new StringBuffer();

        while ((len = r.read(a)) != -1) {
            tmp.append(a, 0, len);
        }

        return tmp.toString();
    }

}
