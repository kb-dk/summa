package dk.statsbiblioteket.summa.ingest.split;

import junit.framework.TestCase;
import static dk.statsbiblioteket.summa.ingest.split.ThreadedStreamParserTest.*;
import dk.statsbiblioteket.summa.common.filter.object.PushFilter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

/**
 * Test cases for StreamController
 *
 * @author mke
 * @since Oct 13, 2009
 */
public class StreamControllerTest extends TestCase {

    StreamController controller;
    PayloadFiller filler;

    public void testMultipleSourcePayloads() {
        PushFilter source = new PushFilter(100, 10000);
        source.add(streamPayloadForBytes());
        source.add(streamPayloadForBytes(0, 1, 2));
        source.add(streamPayloadForBytes(3));
        source.add(streamPayloadForBytes(4, 5));
        source.signalEOF();

        controller = new StreamController(Configuration.newMemoryBased(
                StreamController.CONF_PARSER, PayloadFiller.class
        ));
        controller.setSource(source);
        filler = (PayloadFiller)controller.parser;

        int count = 0;
        while (controller.hasNext()) {
            Payload p = controller.next();

            if (count > 6) {
                // We break out here because the test might
                // otherwise end in an endless loop
                fail("Receieved more than 6 payloads, bailing out");
            }

            assertEquals("Ids should match expected sequence",
                         "id" + count, p.getId());

            assertNull("No errors should occur but after "
                       + count + " payloads got:"
                       + formatStackTrace(filler.getLastError()),
                       filler.getLastError());

            count++;
            System.out.println("Extracted payload " + p);
        }

        assertNull("No errors should occur but at end of run got:\n"
                   + formatStackTrace(filler.getLastError()),
                   filler.getLastError());
        assertEquals("Exactly 6 payloads expected", 6, count);

        controller.close(true);
        System.out.println("All good");
    }

    static Payload streamPayloadForBytes(int... bytes) {
        return new Payload(new ByteArrayInputStream(toByteArray(bytes)));
    }

    static byte[] toByteArray(int... ints) {
        byte[] bytes = new byte[ints.length];
        // Don't use System.arrayCopy because we want validation
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte)ints[i];
        }
        return bytes;
    }
}
