package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.util.Streams;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class SanitiseUnicodeFilterTest extends TestCase {
    public SanitiseUnicodeFilterTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SanitiseUnicodeFilterTest.class);
    }

    byte[] faulty = new byte[]{0x00, 0x0A, 0x20, (byte)0x88};
    byte[] sanitisedDefaultReplace = new byte[]{'?', 0x0A, 0x20, (byte)0x88};
    byte[] sanitisedNoReplace = new byte[]{0x0A, 0x20, (byte)0x88};
    public void testPlainRecord() throws Exception {
        PayloadFeederHelper feeder = new PayloadFeederHelper(
            Arrays.asList(new Payload(new Record("foo", "bar", faulty))));
        SanitiseUnicodeFilter sanitiser =
            new SanitiseUnicodeFilter(Configuration.newMemoryBased());
        sanitiser.setSource(feeder);
        Payload standardResult = sanitiser.next();
        check("Standard record",
              sanitisedDefaultReplace, standardResult.getRecord().getContent());
    }

    public void testRecordNoReplace() throws Exception {
        PayloadFeederHelper feeder = new PayloadFeederHelper(
            Arrays.asList(new Payload(new Record("foo", "bar", faulty))));
        SanitiseUnicodeFilter sanitiser =
            new SanitiseUnicodeFilter(Configuration.newMemoryBased(
                SanitiseUnicodeFilter.CONF_REPLACEMENT_CHAR, ""));
        sanitiser.setSource(feeder);
        Payload noReplaceResult = sanitiser.next();
        check("Standard record no replace",
              sanitisedNoReplace, noReplaceResult.getRecord().getContent());
    }

    public void testPlainStream() throws Exception {
        PayloadFeederHelper feeder = new PayloadFeederHelper(
            Arrays.asList(new Payload(new ByteArrayInputStream(faulty))));
        SanitiseUnicodeFilter sanitiser =
            new SanitiseUnicodeFilter(Configuration.newMemoryBased());
        sanitiser.setSource(feeder);
        Payload standardResult = sanitiser.next();
        ByteArrayOutputStream receiver = new ByteArrayOutputStream();
        Streams.pipe(standardResult.getStream(), receiver);
        byte[] content = receiver.toByteArray();
        check("Standard stream", sanitisedDefaultReplace, content);
    }

    private void check(String message, byte[] expected, byte[] content) {
        assertEquals(message + ". The number of passed bytes should be correct",
                     expected.length, content.length);
        for (int i = 0 ; i < expected.length ; i++) {
            assertEquals(message + ". The bytes at index " + i
                         + " should be equal",
                         expected[i], content[i]);
        }

    }
}
