package dk.statsbiblioteket.summa.storage.database;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 *
 */
public class UniqueTimestampGeneratorTest extends TestCase {

    UniqueTimestampGenerator gen;
    final long maxSalt = UniqueTimestampGenerator.MAX_SALT;
    final long maxTime = UniqueTimestampGenerator.MAX_TIME;

    public void setUp() throws Exception {
        gen = new UniqueTimestampGenerator();
    }

    public void testFixedNow() {
        long now = System.currentTimeMillis();
        long lastStamp = 0;

        for (int i = 0; i < UniqueTimestampGenerator.MAX_SALT; i++) {
            long next = gen.next(now);
            assertFalse(lastStamp == next);
            assertEquals("System time mismatch on iteration " + i
                         +". Delta was: " + (now - gen.systemTime(next)),
                         now, gen.systemTime(next));

            lastStamp = next;
        }
    }

    public void testExtendSalt () {
        long now = System.currentTimeMillis();
        long lastStamp = 0;

        for (long i = 0; i < 2*UniqueTimestampGenerator.MAX_SALT; i++) {
            if (i == UniqueTimestampGenerator.MAX_SALT) {
                now++;
            }

            long next = gen.next(now);
            assertFalse(lastStamp == next);
            assertEquals("System time mismatch on iteration " + i
                         +". Delta was: " + (now - gen.systemTime(next)),
                         now, gen.systemTime(next));

            lastStamp = next;
        }
    }

    public void testIncSalt () {
        long now =System.currentTimeMillis();

        for (long i = 0; i < UniqueTimestampGenerator.MAX_SALT; i++) {
            long next = gen.next(now);
            assertEquals("Salt not incremented as expected",
                         i, gen.salt(next));
            assertEquals("System time mismatch on iteration " + i
                         +". Delta was: " + (now - gen.systemTime(next)),
                         now, gen.systemTime(next));
        }
    }

    public void testSort() {
        int numSalts = 1000;
        int numTimeSteps = 10;

        long[] stamps = new long[numSalts*numTimeSteps];

        for (int timeStep = 0; timeStep < numTimeSteps; timeStep++) {
            for (int salt = 0; salt < numSalts; salt++) {
                long stamp = gen.next(timeStep);
                assertEquals("System time should match timeStep",
                             timeStep, gen.systemTime(stamp));
                assertEquals("Salt should match precomputed value",
                             salt, gen.salt(stamp));

                stamps[timeStep*numSalts + salt] = stamp;
            }
        }

        long[] stampsSorted = Arrays.copyOf(stamps, stamps.length);
        Arrays.sort(stampsSorted);
        
        assertTrue("Generated timestamps should come out sorted",
                   Arrays.equals(stamps, stampsSorted));
    }

    static double log2(double d) {
        return Math.log10(d)/ Math.log10(2);
    }

}
