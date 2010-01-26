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
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.summa.common.util.StringTracker;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.*;

public class WindowQueueTest extends TestCase {
    public WindowQueueTest(String name) {
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
        return new TestSuite(WindowQueueTest.class);
    }

    public void testBounds() throws Exception {
        WindowQueue<String> queue = new WindowQueue<String>(
                null, "b", "k", new StringTracker(200, 500));

        queue.insert("a");
        assertEquals("a should be outside of bounds", 0, queue.getSize());
        queue.insert("b");
        assertEquals("b should be outside of bounds", 0, queue.getSize());
        queue.insert("c");
        assertEquals("c should be inside bounds", 1, queue.getSize());
        queue.insert("j");
        assertEquals("j should be inside bounds", 2, queue.getSize());
        queue.insert("k");
        assertEquals("k should be outside bounds", 2, queue.getSize());
    }

    public void testMonkey() throws Exception {
        int TERMS = 50000;
        String ALPHABET =
                "abcdefghijklmnopqrstuvexyzæøå ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ "
                + "1234567890 !#¤%&/()=@£$";
        int MAX_TERM_LENGTH = 40;

        WindowQueue<String> queue = new WindowQueue<String>(
                null, null, null, new StringTracker(100, 500));
        Random random = new Random(87);
        List<String> raw = new ArrayList<String>(TERMS);
        for (int i = 0 ; i < TERMS ; i++) {
            String term = getString(ALPHABET, MAX_TERM_LENGTH, random);
            queue.insert(term);
            raw.add(term);
        }
        assertTrue("There should be more than 2 terms collected", 
                   queue.getSize() > 2);

        Collections.sort(raw, queue.getComparator());
        ArrayList<String> ql = new ArrayList<String>(queue.getSize());
        while (queue.getSize() > 0) {
            ql.add(queue.removeMin());
        }
        Collections.reverse(ql);
        raw = raw.subList(0, ql.size());
        assertEquals("The queue should sort correctly",
                     Strings.join(raw, ", "),
                     Strings.join(ql, ", "));
    }

    private String getString(String alphabet, int maxLength, Random random) {
        int length = random.nextInt(maxLength)+1;
        char[] chars = new char[length];
        for (int i = 0 ; i < length ; i++) {
            chars[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return new String(chars);
    }

    public void testLimits() throws Exception {
        WindowQueue<String> queue = new WindowQueue<String>(
                null, "b", "k", new StringTracker(2, 500));
        String[] ENTRIES = new String[]{
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"};
        for (String entry: ENTRIES) {
            queue.insert(entry);
        }
        assertEquals("There should be the right number of elements",
                     2, queue.getSize());
        String big = "c123456789123456";
        for (int i = 0 ; i < 5 ; i++) {
            big += big;
        }
        queue.insert(big);
        assertEquals("Big element should push other elements out",
                     1, queue.getSize());
    }

    public void testBasicOrder() throws Exception {
        WindowQueue<String> queue = new WindowQueue<String>(
                null, null, null, new StringTracker(200, 5000));
        String[] ENTRIES = new String[]{
                "b", "a", "c", "d"};
        for (String entry: ENTRIES) {
            queue.insert(entry);
        }
        String[] EXPECTED = new String[] {"d", "c", "b", "a"};
        assertEquals("The sorted result should be correct",
                     Strings.join(EXPECTED, ", "),
                     Strings.join(getElements(queue), ", "));
    }

    public void testBoundsOrder() throws Exception {
        WindowQueue<String> queue = new WindowQueue<String>(
                null, "b", "k", new StringTracker(2, 500));
        String[] ENTRIES = new String[]{
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"};
        for (String entry: ENTRIES) {
            queue.insert(entry);
        }
        String[] EXPECTED = new String[] {"d", "c"};
        assertEquals("The sorted result should be correct",
                     Strings.join(EXPECTED, ", "),
                     Strings.join(getElements(queue), ", "));
    }

    private List<String> getElements(WindowQueue<String> queue) {
        List<String> elements = new ArrayList<String>(queue.getSize());
        while (queue.getSize() > 0) {
            elements.add(queue.removeMin());
        }
        return elements;
    }

}

