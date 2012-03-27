package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.stream.ZIPParserTest;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class UnpackFilterTest extends TestCase {
    public UnpackFilterTest(String name) {
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
        return new TestSuite(UnpackFilterTest.class);
    }

    public void testDump() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        UnpackFilter unpacker = new UnpackFilter(conf);

        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(new Payload(Resolver.getURL(
                        "data/zip/subdouble/sub2/double_stuffed.zip").
                    openStream())));
        unpacker.setSource(feeder);

        while (unpacker.hasNext()) {
            Payload payload = unpacker.next();
            System.out.println(payload + " has origin "
                               + payload.getData(Payload.ORIGIN));
        }
    }

    public void testRecursive() throws Exception {
        assertContains(
            "data/zip/subdouble/sub2/double_stuffed.zip",
            Arrays.asList(
                "double_zipped_foo2", "foo", "kaboom", "zoo", "zoo2"));
    }

    public void testRecursiveDoubleSub() throws Exception {
        assertContains(
            "data/zip/double_stuffed2.zip",
            Arrays.asList(
                "double_zipped_foo2", "double_zipped_foo2b",
                "foo", "kaboom", "zoo", "zoo2"));
    }

    public void assertContains(String source, List<String> expected)
                                                              throws Exception {
        ZIPParserTest.assertContent(getUnpackChain(source), expected);
    }

    
    // Requires a zip64 encoded file of size 2GB+.   
    public void XtestEOF() throws IOException {
        ObjectFilter provider = getUnpackChain(
            "/home/te/tmp/fase3/sb+aleph+1307342370.536.4455+full.zip");
        long counter = 0;
        while (provider.hasNext()) {
            provider.next().close();
            counter++;
            if (counter % 1000 == 0) {
                System.out.print("*");
            }
        }
        System.out.println("\nCounted " + counter + " Payloads");
    }

    public ObjectFilter getUnpackChain(String source) throws IOException {
        assertNotNull("The resource '" + source + "' should be resolvable",
                      Resolver.getURL(source));
        Configuration conf = Configuration.newMemoryBased();
        UnpackFilter unpacker = new UnpackFilter(conf);

        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(new Payload(Resolver.getURL(source).
                    openStream())));
        unpacker.setSource(feeder);

        assertTrue("The unpacker should have at least one element",
                   unpacker.hasNext());
        return unpacker;
    }

    public void testContent1() throws IOException {
        assertSimpleIteration("data/zip/double_stuffed2.zip");
    }

/*    public void testContentSingleGiant() throws IOException {
        assertSimpleIteration(
            "data/zip/sb:aleph:1266199637.735.21556:full.zip");
    }
  */
/*    public void testContentAleph() throws IOException {
        assertSimpleIteration(
            "data/zip/sb:aleph:1293268834.954.18412:full.zip");
    }*/

    // TODO: Generate a zip64 test-file and test iteration
    public void xtestContentAlephSub() throws IOException {
        assertSimpleIteration("data/zip/full_part.zip");
    }

    public void assertSimpleIteration(String source) throws IOException {
        ObjectFilter chain = getUnpackChain(source);
        int counter = 0;
        long content = 0;
        while (chain.hasNext()) {
            Payload payload = chain.next();
            content += countStreamContent(payload);
            assertNotNull("The stream content for " + payload
                          + " (Payload #" + counter + ") should not be null",
                          content);
            counter++;
            if (counter % 1000 == 0) {
                System.out.print(".");
                if (counter % 10000 == 0) {
                    System.out.println(
                        " " + counter + " elements unpacked ("
                        + content / 1048576 + "MB)");
                }
            }
        }
        chain.close(true);
        System.out.println("\nUnpacked " + counter + " elements from '"
                           + source + "' (" + content / 1048576 + "MB)");
    }

    public static long countStreamContent(Payload payload) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Streams.pipe(payload.getStream(), out);
        return out.size();
    }

}
