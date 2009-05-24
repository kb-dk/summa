package dk.statsbiblioteket.summa.common.filter.object;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.util.Streams;

import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;

/**
 * ReplaceFilter Tester.
 *
 * @author <Authors name>
 * @since <pre>05/24/2009</pre>
 * @version 1.0
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ReplaceFilterTest extends TestCase {
    public ReplaceFilterTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ReplaceFilterTest.class);
    }

    public void testRegexp() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                ReplaceFilter.CONF_PATTERN_REGEXP, "a{2}",
                ReplaceFilter.CONF_PATTERN_REPLACEMENT, "bc"
        );
        assertReplace("Double a to bc", "bca", "aaa", conf);
    }

    private void assertReplace(String message, String expected, String input,
                               Configuration conf) throws Exception {
        List<Payload> payloads = new ArrayList<Payload>(3);
        payloads.add(new Payload(new Record(
                "Dummy1", "Dummy", input.getBytes("utf-8"))));
        payloads.add(new Payload(
                new ByteArrayInputStream(input.getBytes("utf-8"))));
        payloads.add(new Payload(
                new ByteArrayInputStream(input.getBytes("utf-8")),
                new Record("Dummy2", "Dummy", input.getBytes("utf-8"))));
        PayloadFeederHelper feeder = new PayloadFeederHelper(payloads);
        ReplaceFilter replaceFilter = new ReplaceFilter(conf);
        replaceFilter.setSource(feeder);

        List<Payload> processed = new ArrayList<Payload>(payloads.size());
        for (int i = 0 ; i < 3 ; i++) {
            assertTrue("The ReplaceFilter should have a next for Payload #" + i,
                       replaceFilter.hasNext());
            processed.add(replaceFilter.next());
        }

        assertEquals(message + " record content only should match",
                     expected, processed.get(0).getRecord().getContentAsUTF8());

        ByteArrayOutputStream bo = new ByteArrayOutputStream(100);
        Streams.pipe(processed.get(1).getStream(), bo);
        assertEquals(message + " stream only should match",
                     expected, bo.toString("utf-8"));

        bo = new ByteArrayOutputStream(100);
        Streams.pipe(processed.get(2).getStream(), bo);
        assertEquals(message + " stream from stream and record should match",
                     expected, bo.toString("utf-8"));
        assertEquals(message + " record content from stream and record should "
                     + "match",
                     expected, processed.get(2).getRecord().getContentAsUTF8());
    }
}
