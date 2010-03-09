/**
 * Created: te 29-12-2009 14:33:57
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.common.util.bits;

import junit.framework.TestCase;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.io.StringWriter;

import dk.statsbiblioteket.summa.common.util.bits.test.BitsArrayPerformance;

public class BitsArrayPerformanceTest extends TestCase {

    public void disabledtestPerformanceSafe() throws Exception {
        new BitsArrayPerformance().testPerformance(false);
    }

    public void disabledtestPerformanceFast() throws Exception {
        new BitsArrayPerformance().testPerformance(true);
    }

    public void testDummy() {
        assertTrue(true);
    }
}

