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
import java.io.UnsupportedEncodingException;
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
        Configuration conf = Configuration.newMemoryBased(
            SnippetFilter.CONF_MAX_LENGTH, 5
        );
        String snippet = filter(conf, input);
/*
        for (int i = 0 ; i < input.length() ; i++) {
            System.out.println(input.charAt(i) + " " + streamContent.charAt(i));
        }
        */
        assertEquals("The extracted snippet should be as expected",
                     "hello", snippet);
    }

    public String filter(Configuration conf, String input)
                                           throws UnsupportedEncodingException {
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(new ByteArrayInputStream(input.getBytes("utf-8")))));
        conf.set(SnippetFilter.CONF_DESTINATION, "hest");
        conf.set(SnippetFilter.CONF_PRESERVE_STREAM, true);
        SnippetFilter snipper = new SnippetFilter(conf);
        snipper.setSource(feeder);
        Payload processed = snipper.next();
        String streamContent = Strings.flushLocal(processed.getStream());
        assertEquals("The Stream content should be preserved",
                     "'" + input + "'", "'" + streamContent + "'");
        return (String)processed.getData("hest");
    }

    public void testSpaceRemove() throws Exception {
        String input = "<foo>hello<bar />     world</invalid>";
        Configuration conf = Configuration.newMemoryBased(
            SnippetFilter.CONF_MAX_LENGTH, 500
        );
        String snippet = filter(conf, input);

        assertEquals("The extracted snippet should be as expected",
                     "hello world ", snippet);
    }

    public void testSkip() throws Exception {
        String input = "<foo>hello<bar />     world</invalid>";
        Configuration conf = Configuration.newMemoryBased(
            SnippetFilter.CONF_MAX_LENGTH, 500,
            SnippetFilter.CONF_SKIP_FIRST, 5
        );
        String snippet = filter(conf, input);

        assertEquals("The extracted snippet should be as expected",
                     "world ", snippet);
    }
}
