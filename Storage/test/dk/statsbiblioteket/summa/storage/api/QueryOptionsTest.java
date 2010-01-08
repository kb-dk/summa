package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

/**
 * Test cases for {@link QueryOptions}
 *
 * @author mke
 * @since Jan 8, 2010
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class QueryOptionsTest extends TestCase {

    public void testAllowsRecord() {
        Record standard = new Record("1", "b", new byte[0]);
        Record deleted = new Record("1", "b", new byte[0]);
        deleted.setDeleted(true);
        Record nonIndexable = new Record("1", "b", new byte[0]);
        nonIndexable.setIndexable(false);

        QueryOptions opt = new QueryOptions();
        assertTrue(opt.allowsRecord(standard));
        assertTrue(opt.allowsRecord(deleted));
        assertTrue(opt.allowsRecord(nonIndexable));

        opt = new QueryOptions(true, null, 0, 0);
        assertFalse(opt.allowsRecord(standard));
        assertTrue(opt.allowsRecord(deleted));
        assertFalse(opt.allowsRecord(nonIndexable));

        opt = new QueryOptions(false, null, 0, 0);
        assertTrue(opt.allowsRecord(standard));
        assertFalse(opt.allowsRecord(deleted));
        assertTrue(opt.allowsRecord(nonIndexable));

        opt = new QueryOptions(null, true, 0, 0);
        assertTrue(opt.allowsRecord(standard));
        assertTrue(opt.allowsRecord(deleted));
        assertFalse(opt.allowsRecord(nonIndexable));

        opt = new QueryOptions(null, false, 0, 0);
        assertFalse(opt.allowsRecord(standard));
        assertFalse(opt.allowsRecord(deleted));
        assertTrue(opt.allowsRecord(nonIndexable));
    }

    public void testMeta() {
        QueryOptions opt = new QueryOptions();

        assertFalse(opt.hasMeta());
        assertNull(opt.meta("foo"));

        opt.meta("foo", "bar");
        assertEquals("bar", opt.meta("foo"));
        assertTrue(opt.hasMeta());
    }

}
