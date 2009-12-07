package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.summa.common.util.StringTracker;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.List;

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
