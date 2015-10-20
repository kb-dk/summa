package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RangeNumberTest extends TestCase {
    public RangeNumberTest(String name) {
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
        return new TestSuite(RangeNumberTest.class);
    }

    public void testAdd() throws Exception {

        RangeNumber r1 = new RangeNumber(4, 4, 4);
        RangeNumber r2 = new RangeNumber(4, 4, 4);
    }

    public void testEdge() throws Exception {
        //TODO: Test goes here...
    }

    public void testSubtract() throws Exception {
        //TODO: Test goes here...
    }

    public void testMin() throws Exception {
        //TODO: Test goes here...
    }

    public void testMax() throws Exception {
        //TODO: Test goes here...
    }

}
