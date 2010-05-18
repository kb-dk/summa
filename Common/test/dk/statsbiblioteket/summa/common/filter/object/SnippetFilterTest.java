package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SnippetFilterTest extends TestCase {
    public SnippetFilterTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SnippetFilterTest.class);
    }

    public void testPreserve() throws Exception {
        String input = "<foo>hello<bar /> world</invalid>";
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(new ByteArrayInputStream(input.getBytes("utf-8")))));
        Configuration conf = Configuration.newMemoryBased(
            SnippetFilter.CONF_DESTINATION, "hest",
            SnippetFilter.CONF_PRESERVE_STREAM, true,
            SnippetFilter.CONF_MAX_LENGTH, 5
        );
        SnippetFilter snipper = new SnippetFilter(conf);
        snipper.setSource(feeder);
        Payload processed = snipper.next();
        String streamContent = Strings.flushLocal(processed.getStream());
/*
        for (int i = 0 ; i < input.length() ; i++) {
            System.out.println(input.charAt(i) + " " + streamContent.charAt(i));
        }
        */
        assertEquals("The Stream content should be preserved",
                     "'" + input + "'", "'" + streamContent + "'");
        assertEquals("The extracted snippet should be as expected",
                     "hello", processed.getData("hest"));
    }
}
