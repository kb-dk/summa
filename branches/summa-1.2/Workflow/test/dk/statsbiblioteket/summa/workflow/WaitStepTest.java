package dk.statsbiblioteket.summa.workflow;

import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.util.ThreadInterrupt;

/**
 *
 */
public class WaitStepTest extends TestCase {

    WaitStep step;

    public void testWait() throws Exception {
        step = new WaitStep(2);

        long delta = System.currentTimeMillis();
        step.run();
        delta = System.currentTimeMillis() - delta;

        assertTrue(delta >= 2000);
    }

    public void testInterrupt() throws Exception {
        step = new WaitStep(2);

        new ThreadInterrupt(Thread.currentThread(), 1000);

        long delta = System.currentTimeMillis();
        step.run();
        delta = System.currentTimeMillis() - delta;

        assertTrue("Should break out after a second. Break time was: "
                   + delta + "ms", delta > 995 && delta < 1005);
    }
}
