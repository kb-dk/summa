package dk.statsbiblioteket.summa.ingest.stream;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.summa.ingest.split.SBMARCParser;
import dk.statsbiblioteket.util.Streams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class PullParserTest extends TestCase {
    public PullParserTest(String name) {
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
        return new TestSuite(PullParserTest.class);
    }

    public void testBasics() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(StreamController.CONF_PARSER, PullParser.class);
        ObjectFilter puller = new StreamController(conf);

        String urls =
                Resolver.getURL("data/filereader/dummyA.xml").toString() + "\n"
                + Resolver.getURL("data/filereader/dummyC.xml").toString();
        Payload sourcePayload = new Payload(
                new ByteArrayInputStream(urls.getBytes("utf-8")));
        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(sourcePayload));
        puller.setSource(feeder);

        assertTrue("The puller should have at least one element",
                   puller.hasNext());
        Payload a = puller.next();
        assertTrue("The Stream should contain the word 'FilereaderTest'",
                   getStreamContent(a).contains("FileReaderTest"));
        a.close();

        assertTrue("The puller should have a second element",
                   puller.hasNext());
        Payload b = puller.next();
        b.close();

        assertFalse("The Puller shpold have no more Pauloads to deliver",
                    puller.hasNext());
        puller.close(true);
    }

    private String getStreamContent(Payload payload) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Streams.pipe(payload.getStream(), out);
        return out.toString("utf-8");
    }
}
