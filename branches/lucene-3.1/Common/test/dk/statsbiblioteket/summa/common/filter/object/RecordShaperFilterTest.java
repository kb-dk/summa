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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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

        List<Payload> payloads = new ArrayList<Payload>(1);

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

        List<Payload> payloads = new ArrayList<Payload>(1);

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
            "(?s)<foo.*>.*<bar .*class=\"id\".*>(.+)</bar>";
            //"(?m)<foo.*>.*<bar .*class=\"id\".*>(.+)</bar>";

    /**
     * Test multiline.
     */
    public void testMultiline() {
        try {
            Configuration conf = Configuration.newMemoryBased(
                    RecordShaperFilter.CONF_ID_REGEXP, ID_REGEXP,
                    RecordShaperFilter.CONF_ID_TEMPLATE, "$2/$1");

            List<Payload> payloads = new ArrayList<Payload>(1);
            payloads.add(new Payload(new Record("id1", "base1",
                                                CONTENT.getBytes("utf-8"))));
            RecordShaperFilter assigner = new RecordShaperFilter(conf);
            assigner.setSource(new PayloadFeederHelper(payloads));
            Record record = assigner.next().getRecord();
            assertEquals("The ID should match", "21", record.getId());
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception is expected here");
        }
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
}
