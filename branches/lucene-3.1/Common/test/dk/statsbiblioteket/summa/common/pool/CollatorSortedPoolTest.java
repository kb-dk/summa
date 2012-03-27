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
package dk.statsbiblioteket.summa.common.pool;

import dk.statsbiblioteket.summa.common.util.CollatorFactory;
import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CollatorSortedPoolTest extends TestCase {
    public CollatorSortedPoolTest(String name) {
        super(name);
    }
    public void setUp() throws Exception {
        super.setUp();
    }
    public void tearDown() throws Exception {
        super.tearDown();
    }

    Collator defaultCollator =
        CollatorFactory.createCollator(new Locale("da"), true);

    private void testSorting(CollatorSortedPool pool) {
        pool.clear();
        String[] terms = new String[]{"a", "å", "", "ø", "æ", "/"};
        pool.addAll(Arrays.asList(terms));
        Arrays.sort(terms, defaultCollator);
        assertEquals(terms, pool);
    }
    public void testMemoryBasedSorting() throws Exception {
        MemoryStringPool pool = new MemoryStringPool(defaultCollator);
        pool.open(new File("Common/tmp/", "facetTestMem"),
                  "testpool", false, true);
        testSorting(pool);
    }
    public void testDiskBasedSorting() throws Exception {
        DiskStringPool pool = new DiskStringPool(defaultCollator);
        pool.open(new File("Common/tmp/", "facetTestDisk"),
                  "testpool", false, true);
        testSorting(pool);
    }

    public void assertEquals(String[] expectedTerms, SortedPool pool) {
        assertEquals("The number of terms hould match",
                     expectedTerms.length, pool.size());
        for (int i = 0 ; i < pool.size() ; i++) {
            assertEquals("Terms at position " + i + " should match",
                         expectedTerms[i], pool.get(i));
        }
    }

    public void testMutation(CollatorSortedPool pool) throws IOException {
        pool.open(StringPoolSuper.poolDir, "foo", false, true);
        pool.clear();
        pool.setCollator(null);
        assertEquals(new String[0], pool);
        pool.add("b");
        pool.add("c");
        pool.add("a");
        assertEquals(new String[]{"a", "b", "c"}, pool);
        pool.remove(0);
        assertEquals(new String[]{"b", "c"}, pool);
        pool.remove(1);
        assertEquals(new String[]{"b"}, pool);
        pool.add("b");
        assertEquals(new String[]{"b"}, pool);
        pool.add("b");
        pool.add("c");
        assertEquals(new String[]{"b", "c"}, pool);
        pool.remove(1);
        assertEquals(new String[]{"b"}, pool);
        pool.remove(0);
        assertEquals(new String[0], pool);
        pool.add("b");
        pool.add("c");
        pool.add("a");
        try {
            pool.remove(10);
            fail("Removing a non-existing entry should throw an exception");
        } catch(IndexOutOfBoundsException e) {
            // Expected
        }
        pool.clear();
        assertEquals(new String[0], pool);
        pool.close();
    }
    public void testMemoryBasedMutation() throws Exception {
        testMutation(new MemoryStringPool(defaultCollator));
    }
    public void testDiskBasedMutation() throws Exception {
        testMutation(new DiskStringPool(defaultCollator));
    }

    // TODO: Save with disk, open with mem - check order
}




