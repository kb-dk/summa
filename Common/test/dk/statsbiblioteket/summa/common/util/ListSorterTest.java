package dk.statsbiblioteket.summa.common.util;

import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.summa.common.util.ListSorter;

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

    public void testSpecific() throws Exception {
        String[][] tests = {
                {""},
                {"a"},
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

    public void assertSort(String message,
                           List<String> expected, List<String> unsorted) {
        ListSorter ls = new ListSorter();
        ls.sort(unsorted, this);
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
