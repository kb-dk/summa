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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.Logs;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.StringWriter;
import java.text.Collator;
import java.util.*;

/**
 * ListSorter Tester.
 *
 * @author <Authors name>
 * @since <pre>08/22/2008</pre>
 * @version 1.0
 */
public class ListSorterTest extends TestCase implements Comparator<String> {
    public ListSorterTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ListSorterTest.class);
    }

    public void testCollator() throws Exception {
        ListSorter ls = new ListSorter();
        Collator myCollator = new CachedCollator(
            new Locale("da"), CachedCollator.COMMON_SUMMA_EXTRACTED, true);
        List<String> test = Arrays.asList("Foo", "bar");
        ls.sort(test, myCollator);
        assertTrue("bar should be the first element", 
                   "bar".equals(test.get(0)));
    }

    public void testSpecific() throws Exception {
        String[][] tests = {
                {""},
                {"a"},
                {"", "a"},
                {"a", ""},
                {"a", "b"},
                {"b", "a"},
                {"a", "a"},
                {"a", "b", "c"},
                {"a", "c", "b"},
                {"c", "a", "b"},
                {"c", "b", "a"},
                {"b", "a", "c"},
                {"b", "c", "a"},
                {"b", "a", "a"},
                {"a", "b", "a"},
                {"a", "b", "a"},
                {"a", "a", "b"}
        };
        for (String[] test: tests) {
            List<String> expected = Arrays.asList(test.clone());
            Collections.sort(expected, this);
            List<String> testData = Arrays.asList(test);
            assertSort("Specific " + Logs.expand(testData, 20),
                       expected, testData);
        }
    }

    public void testSpeed() throws Exception {
        int NUM_WORDS = 100000;
        int MAX_WORD_LENGTH = 10;
        int WARMUP = 5;
        int RUNS = 10;
        List<String> randomWords = getRandomWords(NUM_WORDS, MAX_WORD_LENGTH);
        Collator collator = new CachedCollator(
            new Locale("da"), CachedCollator.COMMON_SUMMA_EXTRACTED, true);
        ListSorter ls = new ListSorter();
        System.out.println("Created " + NUM_WORDS + " words. Warming up");
        for (int i = 0 ; i < WARMUP ; i++) {
            System.gc();
            List<String> copy = new ArrayList<String>(randomWords.size());
            copy.addAll(randomWords);
            Collections.sort(copy, collator);
            System.gc();
            copy = new ArrayList<String>(randomWords.size());
            copy.addAll(randomWords);
            ls.sort(copy, collator);
        }
        System.out.println("Warming finished, doing real runs");

        long classicTime = 0;
        long newTime = 0;
        for (int i = 0 ; i < RUNS ; i++) {
            System.gc();
            List<String> copy = new ArrayList<String>(randomWords.size());
            copy.addAll(randomWords);
            long startTime = System.nanoTime();
            Collections.sort(copy, collator);
            classicTime += System.nanoTime() - startTime;

            System.gc();
            copy = new ArrayList<String>(randomWords.size());
            copy.addAll(randomWords);
            startTime = System.nanoTime();
            ls.sort(copy, collator);
            newTime += System.nanoTime() - startTime;
        }
        System.out.println("Classic: " + classicTime / 1000000.0 + "ms, new: "
                           + newTime / 1000000.0 + "ms, classic/new = "
                           + (1.0 * classicTime / newTime));
    }

    public void testRandom() {
        List<String> random = getRandomWords(20000, 20);
        List<String> copy = new ArrayList<String>(random.size());
        copy.addAll(random);

        Collator collator = new CachedCollator(
            new Locale("da"), CachedCollator.COMMON_SUMMA_EXTRACTED, true);
        ListSorter ls = new ListSorter();
        Collections.sort(random, collator);
        ls.sort(copy, collator);
        assertEquals("random words", random, copy);
    }

    private List<String> getRandomWords(int wordCount, int maxLength) {
        char[] LETTERS = ("abcdefghijklmnopqrstuvwxyzæøå"
                         + "ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ1234567890 ").
                toCharArray();
        Random random = new Random();
        List<String> randomWords = new ArrayList<String>(wordCount);
        for (int i = 0; i < wordCount ; i++) {
            StringWriter sw = new StringWriter();
            int length = random.nextInt(maxLength);
            for (int l = 0 ; l < length ; l++) {
                sw.append(LETTERS[random.nextInt(LETTERS.length)]);
            }
            randomWords.add(sw.toString());
        }
        return randomWords;
    }

    public void assertSort(String message,
                           List<String> expected, List<String> unsorted) {
        ListSorter ls = new ListSorter();
        ls.sort(unsorted, this);
        assertEquals(message, expected, unsorted);
    }
    public void assertEquals(String message,
                             List<String> expected, List<String> unsorted) {
        if (expected.size() != unsorted.size()) {
            fail(message + ": expected size " + expected.size()
                 + " differ from actual size: " + unsorted);
        }
        for (int i = 0 ; i < expected.size() ; i++) {
//            System.out.println(expected.get(i) + " - " + unsorted.get(i));
            assertEquals(message + ": index " + i + " expected "
                         + expected.get(i) + ", got " + unsorted.get(i),
                         expected.get(i), unsorted.get(i));
        }
    }

    public int compare(String o1, String o2) {
        return o1.compareTo(o2);
    }
}




