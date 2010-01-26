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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.storage.BaseStats;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.util.List;
import java.util.Arrays;
import java.io.StringWriter;

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
        BaseStats b = new BaseStats("base", 27, 30, 1, 2, 4, 8);
        assertEquals("base", b.getBaseName());
        assertEquals(27, b.getModificationTime());
        assertEquals(1+4, b.getDeletedCount());
        assertEquals(1+2, b.getIndexableCount());
        assertEquals(2, b.getLiveCount());
        assertEquals(1+2+4+8, b.getTotalCount());
    }

    public void testMeta() {
        BaseStats b = new BaseStats("base", 27, 30, 1, 2, 4, 8);
        assertFalse(b.hasMeta());
        assertNull(b.meta("foo"));
        assertSame(b, b.meta("foo", "bar"));
        assertEquals("bar", b.meta("foo"));
        assertTrue(b.hasMeta());
    }

    public void testXML() {
        BaseStats b1 = new BaseStats("base1", 27, 30, 1, 2, 4, 8);
        BaseStats b2 = new BaseStats("base2", 28, 31, 1, 2, 4, 8).meta("foo", "bar");
        List<BaseStats> stats = Arrays.asList(b1, b2);
        StringWriter w = new StringWriter();
        BaseStats.toXML(stats, w);
        String xml = w.toString();
        String expected =
        "<holdings date=\"1970-01-01T01:00:00.031\" mtime=\"1970-01-01T01:00:00.028\">\n"+
        "  <base name=\"base1\" deleted=\"5\" indexable=\"3\" live=\"2\" total=\"15\" mtime=\"1970-01-01T01:00:00.027\"/>\n"+
        "  <base name=\"base2\" deleted=\"5\" indexable=\"3\" live=\"2\" total=\"15\" mtime=\"1970-01-01T01:00:00.028\">\n"+
        "    <meta key=\"foo\" value=\"bar\"/>\n"+
        "  </base>\n"+
        "</holdings>";
        assertEquals(expected, xml);
        System.out.println(xml);
    }
}

