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
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RecordShaperFilter Tester.
 *
 * @author te <mailto:te@statsbiblioteket.dk>
 * @since <pre>06/27/2009</pre>
 * @version 1.0
 */
@SuppressWarnings( {"DuplicateStringLiteralInspection"} )
public class RecordShaperFilterTest extends TestCase {
    /**
     * Constructor with name.
     * @param name The name.
     */
    public RecordShaperFilterTest(String name) {
        super(name);
    }

    @Override
    public final void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public final void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @return The test suite.
     */
    public static Test suite() {
        return new TestSuite(RecordShaperFilterTest.class);
    }

    /**
     * Test direct assignments.
     */
    public void testDirectAssignments() {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(RecordShaperFilter.CONF_ID_REGEXP, "id_.+di");
        conf.set(RecordShaperFilter.CONF_BASE_REGEXP, "base.+esab");
        conf.set(RecordShaperFilter.CONF_CONTENT_REGEXP, "<real>.+</real>");
        Configuration subMeta = null;
        try {
            subMeta = conf.createSubConfigurations(RecordShaperFilter.CONF_META,
                                                   1).get(0);
        } catch (Exception e) {
            fail("No exception expected here");
        }
        subMeta.set(RecordShaperFilter.CONF_META_KEY, "mymeta");
        subMeta.set(RecordShaperFilter.CONF_META_REGEXP, "meta_(.+)_etam");
        subMeta.set(RecordShaperFilter.CONF_META_TEMPLATE, "$1_party");

        List<Payload> payloads = new ArrayList<>(1);

        String contentInner =
                "<real><identity>id_foo_di</identity>"
                + "<orig>base_bar_esab</orig>meta_zoo_etam</real>";
        String content = "<outer>" + contentInner + "</outer>";
        try {
            payloads.add(new Payload(new Record("dummyid", "dummybase",
                                     content.getBytes("utf-8"))));
        } catch (Exception e) {
            fail("No exception expected here");
        }

        RecordShaperFilter assigner = new RecordShaperFilter(conf);
        assigner.setSource(new PayloadFeederHelper(payloads));
        Record record = assigner.next().getRecord();
        assertEquals("The ID should match", "id_foo_di", record.getId());
        assertEquals("The base should match",
                     "base_bar_esab", record.getBase());
        assertEquals("The content should match",
                     contentInner, record.getContentAsUTF8());
        assertEquals("The correct meta for mymeta should match",
                     "zoo_party", record.getMeta().get("mymeta"));
    }

    /**
     * Test group assignments.
     */
    public void testGroupAssignments() {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(RecordShaperFilter.CONF_ID_REGEXP, "id_(.+)/(.+)_di");
        conf.set(RecordShaperFilter.CONF_ID_TEMPLATE, "$2/$1");

        List<Payload> payloads = new ArrayList<>(1);

        String contentInner =
                "<real><identity>id_foo/bar_di</identity>"
                + "<orig>base_bar_esab</orig>meta_zoo_etam</real>";
        String content = "<outer>" + contentInner + "</outer>";
        try {
            payloads.add(new Payload(new Record("dummyid", "dummybase",
                                            content.getBytes("utf-8"))));
        } catch (Exception e) {
            fail("No exception expected here");
        }

        RecordShaperFilter assigner = new RecordShaperFilter(conf);
        assigner.setSource(new PayloadFeederHelper(payloads));
        Record record = assigner.next().getRecord();
        assertEquals("The ID should match", "bar/foo", record.getId());
    }

    /** Content. */
    public static final String CONTENT =
            "<foo>\n"
            + "    <bar class=\"booga\">kabloey</bar>\n"
            + "    <bar class=\"id\">21</bar>\n"
            + "    <bar class=\"ids\">a, b, c</bar>\n"
            + "</foo>";
    /** ID regular expression. */
    public static final String ID_REGEXP =
            "(?s)<foo.*>.*<bar .*class=\"id\">(.+?)</bar>";
            //"(?m)<foo.*>.*<bar .*class=\"id\".*>(.+)</bar>";

    public void testMultiline() throws UnsupportedEncodingException {
        Configuration conf = Configuration.newMemoryBased(
            RecordShaperFilter.CONF_ID_REGEXP, ID_REGEXP,
            RecordShaperFilter.CONF_ID_TEMPLATE, "$1");

        List<Payload> payloads = new ArrayList<>(1);
        payloads.add(new Payload(new Record(
            "id1", "base1", CONTENT.getBytes("utf-8"))));
        RecordShaperFilter assigner = new RecordShaperFilter(conf);
        assigner.setSource(new PayloadFeederHelper(payloads));
        Record record = assigner.next().getRecord();
        assertEquals("The ID should match", "21", record.getId());
    }

    public void testPartialMeta() throws IOException {
        final String CONTENT = "<hello>\n<isPartOfNewspaper\n  foo:bar rdf:resource=\"info:fedora/plumbum\"";

        Configuration conf = Configuration.newMemoryBased();
        Configuration metaConf = conf.createSubConfigurations(RecordShaperFilter.CONF_META, 1).get(0);
        metaConf.set(RecordShaperFilter.CONF_META_SOURCE, RecordUtil.PART.content);
        metaConf.set(RecordShaperFilter.CONF_META_KEY, RecordUtil.PART.parentid);
        metaConf.set(RecordShaperFilter.CONF_META_REGEXP, "(?s)<isPartOfNewspaper[^>]*rdf:resource=\"info:fedora/([^\"]+)\"");
        metaConf.set(RecordShaperFilter.CONF_META_TEMPLATE, "id:$1");

        ObjectFilter feeder = new PayloadFeederHelper(Collections.singletonList(
                new Payload(new Record("only", "dummy", CONTENT.getBytes("utf-8")))));
        ObjectFilter shaper = new RecordShaperFilter(conf);
        shaper.setSource(feeder);
        Record record = shaper.next().getRecord();

        System.out.println(record);

        assertEquals("The first Payload should have the result assigned to parentid",
                      "id:plumbum", record.getParentIds().get(0));
    }

    /**
     * Test Multiline base use.
     */
    public void testMultilineBaseUse() {
        Pattern pattern = Pattern.compile(ID_REGEXP);
        Matcher matcher = pattern.matcher(CONTENT);
        assertTrue("The pattern " + ID_REGEXP
                   + " should match somewhere in the content",
                   matcher.find());
    }

    public void testLimit() throws IOException {
        Configuration shaperConf = Configuration.newMemoryBased();
        shaperConf.set(RecordShaperFilter.CONF_DISCARD_ON_ERRORS, true);
        shaperConf.set(RecordShaperFilter.CONF_META_REQUIREMENT, "none");
        shaperConf.set(RecordShaperFilter.CONF_COPY_META, true);
        List<Configuration> metaConfs = shaperConf.createSubConfigurations(
            RecordShaperFilter.CONF_META, 1);

        Configuration partConf = metaConfs.get(0);
        partConf.set(RecordShaperFilter.CONF_META_SOURCE,
                     RecordUtil.PART.content);
        partConf.set(RecordShaperFilter.CONF_META_KEY,
                     RecordUtil.PART.meta.toString() + "result");
        partConf.set(RecordShaperFilter.CONF_META_LIMIT, 10);
        partConf.set(RecordShaperFilter.CONF_META_REGEXP, ".*(foo.).*");
        partConf.set(RecordShaperFilter.CONF_META_TEMPLATE, "$1");

        Record small = new Record(
            "small", "bar", "zoofoo7maz".getBytes("utf-8"));
        Record large = new Record(
            "large", "bar", "1234567890zoofoo7maz".getBytes("utf-8"));
        ObjectFilter feeder = new PayloadFeederHelper(Arrays.asList(
            new Payload(small), new Payload(large)));

        ObjectFilter shaper = new RecordShaperFilter(shaperConf);
        shaper.setSource(feeder);

        assertNotNull("The first Payload should have the result assigned",
                      shaper.next().getRecord().getMeta("result"));
        assertNull("The second Payload should not have the result assigned",
                   shaper.next().getRecord().getMeta("result"));

    }

    public void testChildIDs() throws IOException {
        final String ID = "uuid:ff565ef7-ede7-4f25-8fb7-96bc9b4f04e0";

        Configuration shaperConf = Configuration.newMemoryBased();
        shaperConf.set(RecordShaperFilter.CONF_META_REQUIREMENT, "none");
        shaperConf.set(RecordShaperFilter.CONF_COPY_META, true);
        List<Configuration> metaConfs = shaperConf.createSubConfigurations(RecordShaperFilter.CONF_META, 1);

        Configuration partConf = metaConfs.get(0);
        partConf.set(RecordShaperFilter.CONF_META_SOURCE, RecordUtil.PART.content);
        partConf.set(RecordShaperFilter.CONF_META_KEY, RecordUtil.PART.childid);
        partConf.set(RecordShaperFilter.CONF_META_REGEXP,
                     "(?s).*<isPartOfNewspaper[^>]*rdf:resource=\"info:fedora/([^\"]+)\".*");
        partConf.set(RecordShaperFilter.CONF_META_TEMPLATE, "doms_newspaperAuthority:$1");

        Record small = new Record(
            "small", "bar", (
                "<alabast><isPartOfNewspaper xmlns=\"http://doms.statsbiblioteket.dk/relations/default/0/1/#\"" +
                " rdf:resource=\"info:fedora/" + ID + "\"/>\n</alabast>").
                getBytes("utf-8"));
        ObjectFilter feeder = new PayloadFeederHelper(Arrays.asList(new Payload(small)));

        ObjectFilter shaper = new RecordShaperFilter(shaperConf);
        shaper.setSource(feeder);

        Record first = shaper.next().getRecord();
        assertNotNull("The first Payload should have childIDs assigned",
                      first.getChildIds());
        assertEquals("The child-ID should be as expected",
                     "doms_newspaperAuthority:" + ID, first.getChildIds().get(0));

    }
}
