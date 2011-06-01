package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReaderFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;

public class ARCParserTest extends TestCase {
    public ARCParserTest(String name) {
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
        return new TestSuite(ARCParserTest.class);
    }

/**    public static final File SAMPLE = new File(
        "/home/te/tmp/11-1-20100811072018-00000-summa-service.arc");

    public void testUncompressedStreaming() throws IOException {
        InputStream is = new FileInputStream(SAMPLE);
        ArchiveReader ar = marf.getWithFallback(SAMPLE.toString(), is);
        assertContent(ar);
    }

    public void testUncompressedPayloadStreaming() throws IOException {
        ObjectFilter feeder = getFeeder();
        ArchiveReader ar = marf.getWithFallback(
            SAMPLE.toString(), feeder.next().getStream());
        assertContent(ar);
    }

    public void testFaultyCompressedStreaming() throws IOException {
        InputStream is = new FileInputStream(SAMPLE);
        ArchiveReader ar;
        try {
            ARCReaderFactory.get(SAMPLE.toString(), is, true);
            fail("The reader was expected to throw an IOException, complaining "
                 + "that the content was 'Not in GZIP format'");
        } catch (IOException e) {
            // Expected
        }
    }


    public void testUncompressedFileAccess() throws IOException {
        ArchiveReader ar = marf.getUncompressed(SAMPLE);
        assertContent(ar);
    }

    public void testStandardFileAccess() throws IOException {
        ArchiveReader ar = ARCReaderFactory.get(SAMPLE);
        assertContent(ar);
    }

    public void assertContent(ArchiveReader ar) throws IOException {
        assertFalse("The reader should be uncompressed", ar.isCompressed());
        Iterator<ArchiveRecord> ari = ar.iterator();
        int count = 0;
        while (ari.hasNext()) {
            ArchiveRecord rec = ari.next();
//            System.out.println("Got next for record " + count);
            rec.getHeader();
//            System.out.println("Got header for record " + count);
            rec.close();
            count++;
        }
        ar.close();
        System.out.println("Got " + count + " records");

    }

    public void testStreaming() throws FileNotFoundException {
        Configuration conf = Configuration.newMemoryBased(
            StreamController.CONF_PARSER, ARCParser.class,
            ARCParser.CONF_USE_FILEHACK, false
        );
        testStreamControllerARCParser(conf);
    }

    public void testSFileHack() throws FileNotFoundException {
        Configuration conf = Configuration.newMemoryBased(
            StreamController.CONF_PARSER, ARCParser.class,
            ARCParser.CONF_USE_FILEHACK, true
        );
        testStreamControllerARCParser(conf);
    }

    public void testStreamControllerARCParser(Configuration conf)
                                                  throws FileNotFoundException {
        StreamController parser = new StreamController(conf);
        parser.setSource(getFeeder());
        assertTrue("There should be Payloads available", parser.hasNext());
        int count = 0;
        while (parser.hasNext()) {
            Payload p = parser.next();
            p.close();
            count++;
        }
        parser.close(true);
        System.out.println("Extracted " + count + " Payloads");
    }

    private ARCParser.MyARCReaderFactory marf =
        new ARCParser.MyARCReaderFactory();

    private ObjectFilter getFeeder() throws FileNotFoundException {
        Payload payload = new Payload(new FileInputStream(SAMPLE));
        payload.getData().put(Payload.ORIGIN, SAMPLE.toString());
        return new PayloadFeederHelper(Arrays.asList(payload));
    }
 */
}
