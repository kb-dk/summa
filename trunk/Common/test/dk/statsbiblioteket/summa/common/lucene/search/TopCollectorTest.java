package dk.statsbiblioteket.summa.common.lucene.search;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TopCollector Tester.
 *
 * @author <Authors name>
 * @since <pre>12/13/2007</pre>
 * @version 1.0
 */
public class TopCollectorTest extends TestCase {
    public TopCollectorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBasicUsage() throws Exception {
        float[] input = new float[]{0.1f, 0.5f, 0.2f, 0.4f, 0.6f};
        float[] expectedValues = new float[]{0.6f, 0.5f, 0.4f, 0.2f, 0.1f};
        int[] expectedPositions = new int[]{4, 1, 3, 2, 0};
        int[] collectorSizes = new int[]{1, 2, 4, input.length, 10000};

        for (int size: collectorSizes) {
            TopCollector topCollector = new TopCollector(size);
            BinaryCollector binaryCollector = new BinaryCollector(size);
            collectorTestHelper(input, expectedValues, expectedPositions, size,
                                topCollector, "top");
            collectorTestHelper(input, expectedValues, expectedPositions, size,
                                binaryCollector, "top");
        }
    }

    private void collectorTestHelper(float[] input, float[] expectedValues,
                                     int[] expectedPositions, int size,
                                     TopCollector topCollector,
                                     String collectorName) {
        int position = 0;
        for (float value: input) {
            topCollector.collect(position++, value);
        }
//            System.out.println("Size " + size + ": " + topCollector);
        for (int i = 0 ; i < size && i < input.length ; i++) {
            assertEquals("The element " + i + " at " + collectorName + "("
                         + size + ") should have the correct position ",
                         expectedPositions[i], topCollector.getPosition(i));
            assertEquals("The element " + i + " at \" + collectorName + \"("
                         + size + ") should have the correct value ",
                         expectedValues[i], topCollector.getValue(i));
        }
    }

    // TODO: Collapse this with above
    private void collectorTestHelper(float[] input, float[] expectedValues,
                                     int[] expectedPositions, int size,
                                     BinaryCollector binaryCollector,
                                     String collectorName) {
        int position = 0;
        for (float value: input) {
            binaryCollector.collect(position++, value);
        }
//            System.out.println("Size " + size + ": " + binaryCollector);
        for (int i = 0 ; i < size && i < input.length ; i++) {
            assertEquals("The element " + i + " at " + collectorName + "("
                         + size + ") should have the correct position ",
                         expectedPositions[i], binaryCollector.getPosition(i));
            assertEquals("The element " + i + " at \" + collectorName + \"("
                         + size + ") should have the correct value ",
                         expectedValues[i], binaryCollector.getValue(i));
        }
    }

    public static Test suite() {
        return new TestSuite(TopCollectorTest.class);
    }
}



