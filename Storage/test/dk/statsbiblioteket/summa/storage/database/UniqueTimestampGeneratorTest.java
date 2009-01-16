package dk.statsbiblioteket.summa.storage.database;

import junit.framework.TestCase;

/**
 *
 */
public class UniqueTimestampGeneratorTest extends TestCase {

    UniqueTimestampGenerator gen;

    public void setUp() throws Exception {
        gen = new UniqueTimestampGenerator();
    }

    public void testFixedNow() {
        long now = System.currentTimeMillis();
        long lastStamp = 0;

        for (int i = 0; i < UniqueTimestampGenerator.SALT_MAX; i++) {
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

        for (long i = 0; i < 2*UniqueTimestampGenerator.SALT_MAX; i++) {
            if (i == UniqueTimestampGenerator.SALT_MAX) {
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

        for (long i = 0; i < UniqueTimestampGenerator.SALT_MAX; i++) {
            long next = gen.next(now);
            assertEquals("Salt not incremented as expected",
                         i, gen.salt(next));
            assertEquals("System time mismatch on iteration " + i
                         +". Delta was: " + (now - gen.systemTime(next)),
                         now, gen.systemTime(next));
        }
    }



}
