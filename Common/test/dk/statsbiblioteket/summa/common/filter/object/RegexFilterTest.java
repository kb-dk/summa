package dk.statsbiblioteket.summa.common.filter.object;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;

import java.io.StringReader;

/**
 * Test cases for {@link RegexFilter}
 */
public class RegexFilterTest extends TestCase {

    ObjectFilter filter;

    public RegexFilter createRegexFilter(String idRegex,
                                         String baseRegex,
                                         String contentRegex,
                                         boolean isInclusive) {
        Configuration conf = Configuration.newMemoryBased(
                RegexFilter.CONF_ID_REGEX, idRegex,
                RegexFilter.CONF_BASE_REGEX, baseRegex,
                RegexFilter.CONF_CONTENT_REGEX, contentRegex,
                RegexFilter.CONF_MODE, isInclusive ? "inclusive" : "exclusive"
        );

        return new RegexFilter(conf);
    }

    public PayloadBufferFilter prepareFilterChain(ObjectFilter filter,
                                                  Record... records) {
        // Set up the source filter
        PushFilter source = new PushFilter(records.length+1, 2048);

        for (int i = 0; i < records.length; i++) {
            Payload p = new Payload(records[i]);
            source.add(p);
        }
        source.signalEOF();

        // Set up the endpoint filter
        PayloadBufferFilter buf = new PayloadBufferFilter(
                                                Configuration.newMemoryBased());

        // Connect filters
        filter.setSource(source);
        buf.setSource(filter);

        return buf;
    }

    public void testUnconfiguredExclusive() throws Exception {
        filter = createRegexFilter(null, null, null, false);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "test content 1".getBytes()),
                       new Record("id2", "base1", "test content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(2, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
        assertEquals("id1", buf.get(0).getRecord().getId());
    }

    public void testUnconfiguredInclusive() throws Exception {
        filter = createRegexFilter(null, null, null, true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "test content 1".getBytes()),
                       new Record("id2", "base1", "test content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(0, buf.size());
    }

    public void testIdFilterInclusive() throws Exception {
        filter = createRegexFilter("good.*", null, null, true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("good1", "base1", "test content 1".getBytes()),
                       new Record("bad2", "base1", "test content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(1, buf.size());
        assertEquals("good1", buf.get(0).getRecord().getId());
    }

    public void testIdFilterExclusive() throws Exception {
        filter = createRegexFilter("good.*", null, null, false);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("good1", "base1", "test content 1".getBytes()),
                       new Record("bad2", "base1", "test content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(1, buf.size());
        assertEquals("bad2", buf.get(0).getRecord().getId());
    }

    public void testBaseFilterInclusive() throws Exception {
        filter = createRegexFilter(null, "good.*", null, true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "goodBase", "test content 1".getBytes()),
                       new Record("id2", "badBase", "test content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(1, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
    }

    public void testBaseFilterExclusive() throws Exception {
        filter = createRegexFilter(null, "good.*", null, false);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "goodBase", "test content 1".getBytes()),
                       new Record("id2", "badBase", "test content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(1, buf.size());
        assertEquals("id2", buf.get(0).getRecord().getId());
    }

    public void testContentFilterInclusive() throws Exception {
        filter = createRegexFilter(null, null, "good.*", true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "good content 1".getBytes()),
                       new Record("id2", "base1", "bad content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(1, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
    }

    public void testContentFilterExclusive() throws Exception {
        filter = createRegexFilter(null, null, "good.*", false);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "good content 1".getBytes()),
                       new Record("id2", "base1", "bad content 2".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(1, buf.size());
        assertEquals("id2", buf.get(0).getRecord().getId());
    }

    public void testBaseContentFilterInclusive() throws Exception {
        filter = createRegexFilter(null, "good.*", "good.*", true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "good1", "good content 1".getBytes()),
                       new Record("id2", "bad2",  "bad content 2".getBytes()),
                       new Record("id3", "bad3",  "good content 3".getBytes()),
                       new Record("id4", "good4", "bad content 4".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(1, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
    }

    public void testBaseContentFilterExclusive() throws Exception {
        filter = createRegexFilter(null, "good.*", "good.*", false);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "good1", "good content 1".getBytes()),
                       new Record("id2", "bad2",  "bad content 2".getBytes()),
                       new Record("id3", "bad3",  "good content 3".getBytes()),
                       new Record("id4", "good4", "bad content 4".getBytes()));

        // Flush the filter chain
        while (buf.pump()){;}

        assertEquals(1, buf.size());
        assertEquals("id2", buf.get(0).getRecord().getId());
    }

}
