package dk.statsbiblioteket.summa.storage.database;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

/**
 * Test suite for {@link BaseStats}
 *
 * @author mke
 * @since Dec 15, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BaseStatsTest extends TestCase {

    public void testBasicProps() {
        // We use powers of two as counts to avoid collisions in the tests
        BaseStats b = new BaseStats("base", 27, 1, 2, 4, 8);
        assertEquals("base", b.getBaseName());
        assertEquals(27, b.getModificationTime());
        assertEquals(1+4, b.getDeletedCount());
        assertEquals(1+2, b.getIndexableCount());
        assertEquals(2, b.getLiveCount());
        assertEquals(1+2+4+8, b.getTotalCount());
    }

    public void testMeta() {
        BaseStats b = new BaseStats("base", 27, 1, 2, 4, 8);
        assertFalse(b.hasMeta());
        assertNull(b.meta("foo"));
        assertSame(b, b.meta("foo", "bar"));
        assertEquals("bar", b.meta("foo"));
        assertTrue(b.hasMeta());
    }
}
