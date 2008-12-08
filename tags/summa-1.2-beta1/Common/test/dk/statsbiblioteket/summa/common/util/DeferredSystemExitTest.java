package dk.statsbiblioteket.summa.common.util;

import junit.framework.TestCase;

/**
 * Unit tests for {@link DeferredSystemExit}.
 * <p></p>
 * These unit tests are not suited for being run in a batched manner since
 * they need to call {@link System#exit} and Java does not support forking
 * and trapping of sub processes 
 */
public class DeferredSystemExitTest extends TestCase {


    /**
     * Schedule a system exit 0 and wait for it to happen. If it
     * doesn't exit with code 1. This test should exit with code 0.
     * @throws Exception on frobnication
     */
    public void testExit () throws Exception {
        DeferredSystemExit exit = new DeferredSystemExit(0, 1000);

        Thread.sleep (2000);
        System.exit (1);
    }

    /**
     * Schedule a system exit with code 1 and abort it. This test should exit
     * with exit code 0.
     * @throws Exception if the wizzlebizzle has been twizzled
     */
    public void testAbortExit () throws Exception {
        DeferredSystemExit exit = new DeferredSystemExit(1, 1000);

        Thread.sleep (500);
        exit.abortExit();
        Thread.sleep (1500);
        System.exit (0);
    }

}



