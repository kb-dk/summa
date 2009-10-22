package dk.statsbiblioteket.summa.common.filter.object;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.util.ReaderInputStream;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.reader.ReplaceFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ReplaceFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(ReplaceFilterTest.class);

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
        assertReplace("No match abc", "abc", "abc", conf);
        assertReplace("Two times double a to bc", "bcbc", "aaaa", conf);

        conf = Configuration.newMemoryBased(
                ReplaceFilter.CONF_PATTERN_REGEXP, "a{2}(.)b(.)",
                ReplaceFilter.CONF_PATTERN_REPLACEMENT, "$1-$2"
        );
        assertReplace("Group replacement", "zX-Yz", "zaaXbYz", conf);
    }

    public void testSingleline() throws Exception {
        // Dry run
        Pattern pattern = Pattern.compile("a(.*)b");
        String replaced = pattern.matcher("fooafooostbost").replaceAll("$1");
        assertEquals("Replacement over singleline should work",
                     "foofooostost", replaced);
    }

    public void testMultiline() throws Exception {
        Pattern pattern = Pattern.compile("(?s)a(.*)b");
        String replaced = pattern.matcher("fooafoo\nostbost").replaceAll("$1");
        assertEquals("Replacement over multi-line should work",
                     "foofoo\nostost", replaced);
    }

    public void testPlainReplace() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        List<Configuration> rules =
                conf.createSubConfigurations(ReplaceFilter.CONF_RULES, 2);
        rules.get(0).set(ReplaceFilter.CONF_RULE_TARGET, "abc");
        rules.get(0).set(ReplaceFilter.CONF_RULE_REPLACEMENT, "cba");
        rules.get(1).set(ReplaceFilter.CONF_RULE_TARGET, "xyz");
        rules.get(1).set(ReplaceFilter.CONF_RULE_REPLACEMENT, "");

        assertReplace("Clean match", "cba", "abcxyz", conf);
        assertReplace("First match", "zcbazyfoo", "zabczyfoo", conf);
        assertReplace("Second match", "ab", "abxyz", conf);
        assertReplace("No match", "kaboom", "kaboom", conf);
        assertReplace("Empty input", "", "", conf);
    }

    public void testShortTarget() throws Exception {
        testRemoval("abc");
    }

    public void testMediumTarget() throws Exception {
        testRemoval("abcdefghijk");
    }

    public void testStringReplacer() throws Exception {
        String target = "abcdefghijk";
        Map<String, String> rules = new HashMap<String, String>(1);
        rules.put(target, "");
        ReplaceFactory factory = new ReplaceFactory(rules);
        InputStream source =
                new ByteArrayInputStream((target + "foo").getBytes("utf-8"));
        Reader sourceReader = new InputStreamReader(source, "utf-8");
        InputStream processed = new ReaderInputStream(
                factory.getReplacer(sourceReader), "utf-8");
        ByteArrayOutputStream out = new ByteArrayOutputStream(100);
        Streams.pipe(processed, out);
        assertEquals("The result of the removal should be correct",
                     "foo", new String(out.toByteArray(), "utf-8"));
    }

    public void testLongTarget() throws Exception {
        testRemoval("abcdefghijklmnopqrstuvwxyz");
    }

    public void testJavascriptRemoval() throws Exception {
        String[] targets = new String[] {
                "<script",
                "<script language",
                "<script language=\"jav",
                "<script language=\"javascript\">function openwidnowb(linkname)"
                + "{window.open (linkname,\"_blank\",\"resizable=yes,location=1"
                + ",status=1,scrollbars=1\");} </script><script language=\"java"
                + "script\">function openwidnowb(linkname){window.open (linknam"
                + "e,\"_blank\",\"resizable=yes,location=1,status=1,scrollbars="
                + "1\");} </script>"
        };
        for (String target: targets) {
            testRemoval(target);
        }
    }
    public void testRemoval(String target) throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        List<Configuration> rules =
                conf.createSubConfigurations(ReplaceFilter.CONF_RULES, 1);
        rules.get(0).set(ReplaceFilter.CONF_RULE_TARGET, target);
        rules.get(0).set(ReplaceFilter.CONF_RULE_REPLACEMENT, "");
        assertEquals("The target should be stored properly", target,
                     conf.getSubConfigurations(ReplaceFilter.CONF_RULES).get(0).
                             getString(ReplaceFilter.CONF_RULE_TARGET));
        assertReplace("JavaScript should be removed",
                      "plain", target + "plain", conf);
    }

    /* Checks both content and stream replacement */
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
            log.debug("Extracting Payload #" + i);
            assertTrue("The ReplaceFilter should have a next for Payload #" + i,
                       replaceFilter.hasNext());
            processed.add(replaceFilter.next());
        }

        log.debug("Checking record");
        assertEquals(message + " record content only should match",
                     expected, processed.get(0).getRecord().getContentAsUTF8());

        log.debug("Checking stream");
        ByteArrayOutputStream bo = new ByteArrayOutputStream(100);
        Streams.pipe(processed.get(1).getStream(), bo);
        assertEquals(message + " stream only should match",
                     expected, bo.toString("utf-8"));

        log.debug("Checking record & stream");
        bo = new ByteArrayOutputStream(100);
        Streams.pipe(processed.get(2).getStream(), bo);
        assertEquals(message + " stream from stream and record should match",
                     expected, bo.toString("utf-8"));
        assertEquals(message + " record content from stream and record should "
                     + "match",
                     expected, processed.get(2).getRecord().getContentAsUTF8());
    }
}
