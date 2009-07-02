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
 * AssignMetaFilter Tester.
 *
 * @author <Authors name>
 * @since <pre>06/27/2009</pre>
 * @version 1.0
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class AssignMetaFilterTest extends TestCase {
    public AssignMetaFilterTest(String name) {
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
        return new TestSuite(AssignMetaFilterTest.class);
    }

    public void testDirectAssignments() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(AssignMetaFilter.CONF_ID_REGEXP, "id_.+di");
        conf.set(AssignMetaFilter.CONF_BASE_REGEXP, "base.+esab");
        conf.set(AssignMetaFilter.CONF_CONTENT_REGEXP, "<real>.+</real>");
        Configuration subMeta = conf.createSubConfigurations(
                AssignMetaFilter.CONF_METAS, 1).get(0);
        subMeta.set(AssignMetaFilter.CONF_META_KEY, "mymeta");
        subMeta.set(AssignMetaFilter.CONF_META_REGEXP, "meta.+etam");

        List<Payload> payloads = new ArrayList<Payload>(1);

        String contentInner =
                "<real><identity>id_foo_di</identity>"
                + "<orig>base_bar_esab</orig>meta_zoo_etam</real>";
        String content = "<outer>" + contentInner + "</outer>";
        payloads.add(new Payload(new Record("dummyid", "dummybase",
                                            content.getBytes("utf-8"))));

        AssignMetaFilter assigner = new AssignMetaFilter(conf);
        assigner.setSource(new PayloadFeederHelper(payloads));
        Record record = assigner.next().getRecord();
        assertEquals("The ID should match", "id_foo_di", record.getId());
        assertEquals("The base should match",
                     "base_bar_esab", record.getBase());
        assertEquals("The content should match",
                     contentInner, record.getContentAsUTF8());
        assertEquals("The correct meta for mymeta should match",
                     "meta_zoo_etam", record.getMeta().get("mymeta"));
    }

    public void testGroupAssignments() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(AssignMetaFilter.CONF_ID_REGEXP, "id_(.+)/(.+)_di");
        conf.set(AssignMetaFilter.CONF_ID_TEMPLATE, "$2/$1");

        List<Payload> payloads = new ArrayList<Payload>(1);

        String contentInner =
                "<real><identity>id_foo/bar_di</identity>"
                + "<orig>base_bar_esab</orig>meta_zoo_etam</real>";
        String content = "<outer>" + contentInner + "</outer>";
        payloads.add(new Payload(new Record("dummyid", "dummybase",
                                            content.getBytes("utf-8"))));

        AssignMetaFilter assigner = new AssignMetaFilter(conf);
        assigner.setSource(new PayloadFeederHelper(payloads));
        Record record = assigner.next().getRecord();
        assertEquals("The ID should match", "bar/foo", record.getId());
    }
}
