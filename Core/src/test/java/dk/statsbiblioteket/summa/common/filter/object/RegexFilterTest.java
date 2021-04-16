/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.PayloadMatcher;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Test cases for {@link RegexFilter}
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class RegexFilterTest extends TestCase {

    ObjectFilter filter;

    public RegexFilter createRegexFilter(String idRegex,
                                         String baseRegex,
                                         String contentRegex,
                                         boolean isInclusive) {
        Configuration conf = Configuration.newMemoryBased(
                PayloadMatcher.CONF_ID_REGEX, idRegex,
                PayloadMatcher.CONF_BASE_REGEX, baseRegex,
                PayloadMatcher.CONF_CONTENT_REGEX, contentRegex,
                RegexFilter.CONF_MODE, isInclusive ? "inclusive" : "exclusive"
        );

        return new RegexFilter(conf);
    }

    private static final String MULTI_SAMPLE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<MetadataRecord><recordID>phonofile-0000123456789012</recordID><status>deleted</status></MetadataRecord>";
    public void testMultiLine() throws UnsupportedEncodingException {
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(new Payload(
                new Record("foo", "bar", MULTI_SAMPLE.getBytes(StandardCharsets.UTF_8)))));
        RegexFilter regex = new RegexFilter(Configuration.newMemoryBased(
                AbstractDiscardFilter.CONF_MARK, true,
                PayloadMatcher.CONF_CONTENT_REGEX, new ArrayList<>(Arrays.asList(
                "(?s).{1,1000}?<status>deleted</status>.*"))
        ));
        regex.setSource(feeder);
        assertTrue("There should be a Payload available", regex.hasNext());
        assertTrue("The Payload should be marked as deleted", regex.next().getRecord().isDeleted());
    }

    public static final String CONTENT_SAMPLE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<record xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xmlns=\"http://www.openarchives.org/OAI/2.0/\"><header>" +
            "<identifier>oai:cds.cern.ch:2013456</identifier>" +
            "<datestamp>2015-04-29T21:55:23Z</datestamp>" +
            "<setSpec>cerncds:FULLTEXT</setSpec>" +
            "</header><metadata><oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
            "xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ " +
            "http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">" +
            "<dc:identifier>http://cds.cern.ch/record/2012345</dc:identifier><dc:language>eng</dc:language>" +
            "<dc:creator>International Organization for Standardization. Geneva</dc:creator>" +
            "<dc:title>Sunfire Academy: Part 132: guide for the holding of fireants and sampling</dc:title>" +
            "<dc:subject>Engineering</dc:subject><dc:publisher>ISO</dc:publisher>" +
            "<dc:identifier>oai:cds.cern.ch:2012345</dc:identifier>" +
            "<dc:date>2015</dc:date></oai_dc:dc></metadata></record>";

    public void testIDAndContentMatcher() throws IOException {
        Configuration conf = Configuration.newMemoryBased(
                PayloadMatcher.CONF_ID_REGEX, new ArrayList<>(Arrays.asList(
                        "nonmatching_id1", "nonmatching_id2")),
                PayloadMatcher.CONF_CONTENT_REGEX, new ArrayList<>(Collections.singletonList(
                        "<dc.publisher>ISO<.dc.publisher>")),
                PayloadMatcher.CONF_MATCH_METHOD, PayloadMatcher.MATCH_METHOD.partial
        );

        RegexFilter matcher = new RegexFilter(conf);
        Payload noMatch = new Payload(new Record(
                "noMatch", "dummy", new byte[10]));
        Payload recordMatchPayload = new Payload(new Record(
                "recordMatch", "dummy", CONTENT_SAMPLE.getBytes(StandardCharsets.UTF_8)));
        PayloadBufferFilter buf = prepareFilterChain(
                matcher,
                noMatch, recordMatchPayload);

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(1, buf.size());
        assertEquals("noMatch", buf.get(0).getRecord().getId());
//        assertEquals("recordMatch", buf.get(1).getRecord().getId());
    }

    public PayloadBufferFilter prepareFilterChain(ObjectFilter filter,
                                                  Record... records) {
        // Set up the source filter
        PushFilter source = new PushFilter(records.length+1, 2048);

        for (Record record : records) {
            Payload p = new Payload(record);
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

    public PayloadBufferFilter prepareFilterChain(ObjectFilter filter,
                                                  Payload... payloads) {
        // Set up the source filter
        PushFilter source = new PushFilter(payloads.length+1, 5048);

        for (Payload payload : payloads) {
            source.add(payload);
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
                       new Record("id1", "base1", "test content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2", "base1", "test content 2".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(2, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
        assertEquals("id2", buf.get(1).getRecord().getId());
    }

    public void testEqualityEqual() throws Exception {
        PayloadBufferFilter buf = getEqualityResult(PayloadMatcher.MATCH_AMOUNT_EQUALITY.equal, 2);
        assertEquals(1, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
    }
    public void testEqualityLess() throws Exception {
        PayloadBufferFilter buf = getEqualityResult(PayloadMatcher.MATCH_AMOUNT_EQUALITY.less, 2);
        assertEquals(1, buf.size());
        assertEquals("id2", buf.get(0).getRecord().getId());
    }
    public void testEqualityMore() throws Exception {
        PayloadBufferFilter buf = getEqualityResult(PayloadMatcher.MATCH_AMOUNT_EQUALITY.more, 1);
        assertEquals(1, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
    }

    private PayloadBufferFilter getEqualityResult(
            PayloadMatcher.MATCH_AMOUNT_EQUALITY matchEquality, int matchAmount) throws IOException {
        RegexFilter filter = new RegexFilter(Configuration.newMemoryBased(
                PayloadMatcher.CONF_CONTENT_REGEX, "test",
                PayloadMatcher.CONF_MATCH_METHOD, PayloadMatcher.MATCH_METHOD.partial,
                PayloadMatcher.CONF_MATCH_AMOUNT, matchAmount,
                PayloadMatcher.CONF_MATCH_AMOUNT_EQUALITY, matchEquality,
                RegexFilter.CONF_MODE, "inclusive"
        ));

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "test content 1a and test content 1b".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2", "base1", "test content 2".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}
        return buf;
    }

    public void testUnconfiguredInclusive() throws Exception {
        filter = createRegexFilter(null, null, null, true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "test content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2", "base1", "test content 2".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(0, buf.size());
    }

    public void testIdFilterInclusive() throws Exception {
        filter = createRegexFilter("good.*", null, null, true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("good1", "base1", "test content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("bad2", "base1", "test content 2".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(1, buf.size());
        assertEquals("good1", buf.get(0).getRecord().getId());
    }

    public void testMetaNullValueRegexp() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                PayloadMatcher.CONF_META_KEY, "foo",
                RegexFilter.CONF_MODE, "inclusive"
        );

        RegexFilter matcher = new RegexFilter(conf);
        Payload noMatch = new Payload(new Record(
                "noMatch", "dummy", new byte[10]));
        Record recordMatch = new Record(
                "recordMatch", "dummy", new byte[10]);
        recordMatch.getMeta().put("foo", "ost");
        Payload recordMatchPayload = new Payload(recordMatch);
        Payload match = new Payload(new Record(
                "match", "dummy", new byte[10]));
        match.getData().put("foo", "klarbardaf");
        PayloadBufferFilter buf = prepareFilterChain(
                       matcher,
                       noMatch, match, recordMatchPayload);

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(2, buf.size());
        assertEquals("match", buf.get(0).getRecord().getId());
        assertEquals("recordMatch", buf.get(1).getRecord().getId());
    }

    public void testMetaValueRegexp() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                PayloadMatcher.CONF_META_KEY, "foo",
                PayloadMatcher.CONF_META_VALUE_REGEXP, ".*a.*",
                RegexFilter.CONF_MODE, "inclusive"
        );

        RegexFilter matcher = new RegexFilter(conf);
        Payload noMatch = new Payload(new Record(
                "noMatch", "dummy", new byte[10]));
        Record recordMatch = new Record(
                "recordMatch", "dummy", new byte[10]);
        recordMatch.getMeta().put("foo", "osteanretning");
        Payload recordMatchPayload = new Payload(recordMatch);
        Payload match = new Payload(new Record(
                "match", "dummy", new byte[10]));
        match.getData().put("foo", "klarbardaf");
        Payload noMatch2 = new Payload(new Record(
                "noMatch2", "dummy", new byte[10]));
        noMatch2.getData().put("foo", "hest");
        PayloadBufferFilter buf = prepareFilterChain(
                       matcher,
                       noMatch, match, recordMatchPayload, noMatch2);

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(2, buf.size());
        assertEquals("match", buf.get(0).getRecord().getId());
        assertEquals("recordMatch", buf.get(1).getRecord().getId());
    }

    public void testIdFilterExclusive() throws Exception {
        filter = createRegexFilter("good.*", null, null, false);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("good1", "base1", "test content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("bad2", "base1", "test content 2".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(1, buf.size());
        assertEquals("bad2", buf.get(0).getRecord().getId());
    }

    public void testBaseFilterInclusive() throws Exception {
        filter = createRegexFilter(null, "good.*", null, true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "goodBase", "test content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2", "badBase", "test content 2".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(1, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
    }

    public void testBaseFilterExclusive() throws Exception {
        filter = createRegexFilter(null, "good.*", null, false);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "goodBase", "test content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2", "badBase", "test content 2".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(1, buf.size());
        assertEquals("id2", buf.get(0).getRecord().getId());
    }

    public void testContentFilterInclusive() throws Exception {
        filter = createRegexFilter(null, null, "good.*", true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "good content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2", "base1", "bad content 2".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(1, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
    }

    public void testContentFilterExclusive() throws Exception {
        filter = createRegexFilter(null, null, "good.*", false);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "base1", "good content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2", "base1", "bad content 2".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(1, buf.size());
        assertEquals("id2", buf.get(0).getRecord().getId());
    }

    public void testBaseContentFilterInclusive() throws Exception {
        filter = createRegexFilter(null, "good.*", "good.*", true);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "good1", "test content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2", "bad2",  "bad content 2".getBytes(StandardCharsets.UTF_8)),
                       new Record("id3", "bad3",  "bad content 3".getBytes(StandardCharsets.UTF_8)),
                       new Record("id4", "base4", "good content 4".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(2, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
        assertEquals("id4", buf.get(1).getRecord().getId());
    }

    public void testBaseContentFilterExclusive() throws Exception {
        filter = createRegexFilter(null, "good.*", "good.*", false);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1", "good1", "good content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2", "bad2",  "bad content 2".getBytes(StandardCharsets.UTF_8)),
                       new Record("id3", "bad3",  "good content 3".getBytes(StandardCharsets.UTF_8)),
                       new Record("id4", "good4", "bad content 4".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(1, buf.size());
        assertEquals("id2", buf.get(0).getRecord().getId());
    }

    public void testTwoIdFiltersExclusive() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                PayloadMatcher.CONF_ID_REGEX,
                "bad1, bad2"
        );
        filter = new RegexFilter(conf);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("id1",  "base1", "content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("id2",  "base1", "content 2".getBytes(StandardCharsets.UTF_8)),
                       new Record("bad1", "base1", "content 3".getBytes(StandardCharsets.UTF_8)),
                       new Record("bad2", "base1", "content 4".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(2, buf.size());
        assertEquals("id1", buf.get(0).getRecord().getId());
        assertEquals("id2", buf.get(1).getRecord().getId());
    }

    public void testTwoIdFiltersInclusive() throws Exception {
        Configuration conf = Configuration.newMemoryBased(
                PayloadMatcher.CONF_ID_REGEX,
                "good1, good2",
                RegexFilter.CONF_MODE, "inclusive"
        );
        filter = new RegexFilter(conf);

        PayloadBufferFilter buf = prepareFilterChain(
                       filter,
                       new Record("good1",  "base1", "content 1".getBytes(StandardCharsets.UTF_8)),
                       new Record("good2",  "base1", "content 2".getBytes(StandardCharsets.UTF_8)),
                       new Record("id3", "base1", "content 3".getBytes(StandardCharsets.UTF_8)),
                       new Record("id4", "base1", "content 4".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        //
        assertEquals(2, buf.size());
        assertEquals("good1", buf.get(0).getRecord().getId());
        assertEquals("good2", buf.get(1).getRecord().getId());
    }

    public void testTwoOddIdsFiltersExclusive() throws Exception {
        // The dots in these regexes are part of the id's and should
        // strictly speaking be escaped, but in this test we are lazy...
        Configuration conf = Configuration.newMemoryBased(
                PayloadMatcher.CONF_ID_REGEX,
                "oai:doaj-articles:b37e5a0253e3ca1090ee7b6268050a44, " +
                "oai:pangaea.de:doi:10.1594/PANGAEA.712421"
        );
        filter = new RegexFilter(conf);

        PayloadBufferFilter buf = prepareFilterChain(
                filter,
                new Record("good1", "base1", "content 1".getBytes(StandardCharsets.UTF_8)),
                new Record("oai:doaj-articles:b37e5a0253e3ca1090ee7b6268050a44",
                           "base1", "content 2".getBytes(StandardCharsets.UTF_8)),
                new Record("oai:pangaea.de:doi:10.1594/PANGAEA.712421",
                           "base1", "content 3".getBytes(StandardCharsets.UTF_8)),
                new Record("good2", "base1", "content 4".getBytes(StandardCharsets.UTF_8)));

        // Flush the filter chain
        while (buf.pump()){}

        assertEquals(2, buf.size());
        assertEquals("good1", buf.get(0).getRecord().getId());
        assertEquals("good2", buf.get(1).getRecord().getId());
    }
}

