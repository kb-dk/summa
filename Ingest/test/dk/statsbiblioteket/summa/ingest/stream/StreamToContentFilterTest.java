package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class StreamToContentFilterTest extends TestCase {
    public StreamToContentFilterTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(StreamToContentFilterTest.class);
    }

    public void testSimpleCopy() throws Exception {
        String testData = "foo";
        testStringCopy(testData);
    }

    public void testEmptyCopy() throws Exception {
        String testData = "";
        testStringCopy(testData);
    }

    private void testStringCopy(String s) throws UnsupportedEncodingException {
        Payload in =
            new Payload(new ByteArrayInputStream(s.getBytes("utf-8")));
        ObjectFilter feeder = new PayloadFeederHelper(Arrays.asList(in));
        StreamToContentFilter copy =
            new StreamToContentFilter(Configuration.newMemoryBased(
                StreamToContentFilter.CONF_BASE, "bar"));
        copy.setSource(feeder);
        Payload out = copy.next();
        assertEquals("The content should match the stream",
                     s, out.getRecord().getContentAsUTF8());
    }
}
