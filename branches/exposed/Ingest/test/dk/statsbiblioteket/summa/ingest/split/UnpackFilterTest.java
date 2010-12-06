package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.stream.ZIPParserTest;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

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

    public void testBasics() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        UnpackFilter unpacker = new UnpackFilter(conf);

        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(new Payload(Resolver.getURL(
                        "data/zip/subdouble/sub2/double_stuffed.zip").
                    openStream())));
        unpacker.setSource(feeder);

        assertTrue("The unpacker should have at least one element",
                   unpacker.hasNext());

        List<String> expected = Arrays.asList(
                "double_zipped_foo2", "foo", "kaboom", "zoo", "zoo2");
        ZIPParserTest.assertContent(unpacker, expected);
    }

}
