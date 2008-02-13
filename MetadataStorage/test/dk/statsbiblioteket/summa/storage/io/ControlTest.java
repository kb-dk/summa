package dk.statsbiblioteket.summa.storage.io;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

/**
 * This JUnit-test is written with ControlDerby testing in mind. However, it
 * is kept fairly generic so later evolvement to tests for other implementations
 * shouldn't be hard.
 */
public class ControlTest extends TestCase {
    public ControlTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ControlTest.class);
    }
}
