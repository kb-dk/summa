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
package dk.statsbiblioteket.summa.common;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import dk.statsbiblioteket.util.Zips;

/**
 * Unit tests for the {@link Record} class
 */
public class RecordTest extends TestCase {

    Record r;
    byte[] emptyContent;

    public void setUp () {
        r = new Record();
        emptyContent = new byte[0];
    }

    public void testSetChildIds () {
        assertNull (r.getChildren());
        assertNull (r.getChildIds());

        r.setChildIds(Arrays.asList("foo", "bar"));

        List<String> childIds = r.getChildIds();
        assertEquals(Arrays.asList("foo", "bar"), childIds);

        // We should not ahve any resolved children
        assertNull (r.getChildren());
    }

    public void testSetParentIds () {
        assertNull (r.getChildren());
        assertNull (r.getChildIds());

        r.setParentIds(Arrays.asList("foo", "bar"));

        List<String> parentIds = r.getParentIds();
        assertEquals(Arrays.asList("foo", "bar"), parentIds);

        // We should not ahve any resolved children
        assertNull (r.getParents());
    }

    public void testSetChildren () {
        assertNull (r.getChildren());
        assertNull (r.getChildIds());

        List<Record> children = new ArrayList<Record>(2);
        children.add(new Record("foo", "base", emptyContent));
        children.add(new Record("bar", "base", emptyContent));

        r.setChildren(children);

        assertEquals(children, r.getChildren());

        // We should updated the child ids accordingly
        assertEquals (Arrays.asList("foo", "bar"), r.getChildIds());
    }

    public void testSetParents () {
        assertNull (r.getParents());
        assertNull (r.getParentIds());

        List<Record> parents = new ArrayList<Record>(2);
        parents.add(new Record("foo", "base", emptyContent));
        parents.add(new Record("bar", "base", emptyContent));

        r.setParents(parents);

        assertEquals(parents, r.getParents());

        // We should updated the child ids accordingly
        assertEquals (Arrays.asList("foo", "bar"), r.getParentIds());
    }

    public void testSimpleContent () throws Exception {
        String orig = "Summa - the choice of GNU generation";
        r.setRawContent(orig.getBytes());

        assertEquals(orig, r.getContentAsUTF8());
        assertTrue(Arrays.equals(orig.getBytes(), r.getContent()));

        /* Run the tests twice to make sure that we don't have some
         * flip-flopping of the contentCompressed flag */
        assertEquals(orig, r.getContentAsUTF8());
        assertTrue(Arrays.equals(orig.getBytes(), r.getContent()));
    }

    public void testCompressedContent() throws Exception {
        String orig = "Summa rocks my socks";
        byte[] data = Zips.gzipBuffer(orig.getBytes());

        r.setRawContent(data, true);

        assertEquals(orig, r.getContentAsUTF8());
        assertTrue(Arrays.equals(orig.getBytes("utf-8"), r.getContent()));

        /* Run the tests twice to make sure that we don't have some
         * flip-flopping of the contentCompressed flag */
        assertEquals(orig, r.getContentAsUTF8());
        assertTrue(Arrays.equals(orig.getBytes(), r.getContent()));
    }

    public void testCompressedContentII() throws Exception {
        String orig = "Summa pops my socks";
        r.setContent(orig.getBytes("utf-8"), true);
        assertEquals(orig, r.getContentAsUTF8());
        r.setContent(orig.getBytes("utf-8"), false);
        assertEquals(orig, r.getContentAsUTF8());

        byte[] data = Zips.gzipBuffer(orig.getBytes());
        r.setRawContent(data, true);
        assertEquals(orig, r.getContentAsUTF8());
    }

    public void testEquals () throws Exception {
        Record r1_1 = new Record("id1", "base1", "".getBytes());
        Record r1_2 = new Record("id1", "base1", "".getBytes());
        Record r2 = new Record("id2", "base1", "".getBytes());

        r1_1.setContent("Hello world".getBytes(), false);
        r1_2.setContent("Hello world".getBytes(), true);
        assertEquals(r1_1, r1_2);

        r1_1.setParentIds(Arrays.asList("foo", "bar"));
        r1_2.setParentIds(Arrays.asList("foo", "bar"));
        assertEquals(r1_1, r1_2);

        r1_1.setChildIds(Arrays.asList("quiz", "show"));
        r1_2.setChildIds(Arrays.asList("quiz", "show"));
        assertEquals(r1_1, r1_2);

        assertFalse(r1_1.equals(r2));
        assertFalse(r1_2.equals(r2));
    }

    public void testConstructorsWithEmptyContent() {
        Record r = new Record();
        assertNotNull(r);
        r = new Record("test", "test-base", true, false, null, 0, 0, null, null,
                       null, false);
        assertNotNull(r);
    }
}
