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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.Record;

import java.util.List;
import java.util.ArrayList;

/**
 * RecordShaperFilter Tester.
 *
 * @author <Authors name>
 * @since <pre>06/27/2009</pre>
 * @version 1.0
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class RecordShaperFilterTest extends TestCase {
    public RecordShaperFilterTest(String name) {
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
        return new TestSuite(RecordShaperFilterTest.class);
    }

    public void testDirectAssignments() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(RecordShaperFilter.CONF_ID_REGEXP, "id_.+di");
        conf.set(RecordShaperFilter.CONF_BASE_REGEXP, "base.+esab");
        conf.set(RecordShaperFilter.CONF_CONTENT_REGEXP, "<real>.+</real>");
        Configuration subMeta = conf.createSubConfigurations(
                RecordShaperFilter.CONF_META, 1).get(0);
        subMeta.set(RecordShaperFilter.CONF_META_KEY, "mymeta");
        subMeta.set(RecordShaperFilter.CONF_META_REGEXP, "meta_(.+)_etam");
        subMeta.set(RecordShaperFilter.CONF_META_TEMPLATE, "$1_party");

        List<Payload> payloads = new ArrayList<Payload>(1);

        String contentInner =
                "<real><identity>id_foo_di</identity>"
                + "<orig>base_bar_esab</orig>meta_zoo_etam</real>";
        String content = "<outer>" + contentInner + "</outer>";
        payloads.add(new Payload(new Record("dummyid", "dummybase",
                                            content.getBytes("utf-8"))));

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

    public void testGroupAssignments() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(RecordShaperFilter.CONF_ID_REGEXP, "id_(.+)/(.+)_di");
        conf.set(RecordShaperFilter.CONF_ID_TEMPLATE, "$2/$1");

        List<Payload> payloads = new ArrayList<Payload>(1);

        String contentInner =
                "<real><identity>id_foo/bar_di</identity>"
                + "<orig>base_bar_esab</orig>meta_zoo_etam</real>";
        String content = "<outer>" + contentInner + "</outer>";
        payloads.add(new Payload(new Record("dummyid", "dummybase",
                                            content.getBytes("utf-8"))));

        RecordShaperFilter assigner = new RecordShaperFilter(conf);
        assigner.setSource(new PayloadFeederHelper(payloads));
        Record record = assigner.next().getRecord();
        assertEquals("The ID should match", "bar/foo", record.getId());
    }
}

