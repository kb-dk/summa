package dk.statsbiblioteket.summa.ingest.split;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Test cases for {@link ThreadedStreamParser}
 */
public class ThreadedStreamParserTest extends TestCase {

    abstract static class BaseParser extends ThreadedStreamParser {

        private int numProcessed;

        BaseParser(Integer queueSize, Integer queueByteSize, Integer timeOut) {
            super(createConfig(queueSize, queueByteSize, timeOut));
            numProcessed = 0;
        }

        private static Configuration createConfig(Integer queueSize,
                                                  Integer queueByteSize,
                                                  Integer timeOut) {
            Configuration conf = Configuration.newMemoryBased();
            if (queueSize != null) {
                conf.set(ThreadedStreamParser.CONF_QUEUE_SIZE,
                         queueSize);
            }
            if (queueByteSize != null) {
                conf.set(ThreadedStreamParser.CONF_QUEUE_BYTESIZE,
                         queueByteSize);
            }
            if (timeOut != null) {
                conf.set(ThreadedStreamParser.CONF_QUEUE_TIMEOUT, timeOut);
            }

            return conf;
        }
    }

    /**
     * Creates one payload per byte in the source stream
     */
    static class PayloadFiller extends BaseParser {

        PayloadFiller (int queueSize) {
            super(queueSize, Integer.MAX_VALUE, 1000);
        }

        @Override
        public void protectedRun() {
            InputStream in = sourcePayload.getStream();

            try {
                int codePoint;
                while ((codePoint = in.read()) != -1) {

                addToQueue(new Record("id" + codePoint, "base", new byte[0]));
            }
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
    }

    /**
     * Run 20 payloads through a parser with room for 5
     */
    public void testExceedCapacity() {
        PayloadFiller filler = new PayloadFiller(5);
        filler.open(new Payload(new ByteArrayInputStream(new byte[20])));

        int count = 0;
        while (filler.hasNext()) {
            Payload p = filler.next();
            count++;

            if (count > 20) {
                // We break out here because the test might
                // otherwise end in an endless loop
                fail("Receieved more than 20 payloads, bailing out");
            }
        }

        assertEquals("Exactly 20 payloads expected", 20, count);
    }

    public void testDoubleDefaultNamespace() throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
        XMLStreamReader reader = inputFactory.createXMLStreamReader(
                Resolver.getURL("data/double_default_oai.xml").openStream(),
                "utf-8");
        while (reader.hasNext()) {
            reader.next();
        }
    }
}
