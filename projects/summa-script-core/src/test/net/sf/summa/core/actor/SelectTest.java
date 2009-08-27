package net.sf.summa.core.actor;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.*;

import java.util.List;
import java.util.LinkedList;

/**
 *
 */
public class SelectTest {

    /*
     * Return an array of (select, expected_continuation) pairs
     */
    @DataProvider(name = "SelectAny")
    public Object[][] createAnySelections() {
        return new Object[][]{
            new Object[]{Select.any(), null},
            new Object[]{Select.any(Long.MAX_VALUE), null},
            new Object[]{Select.any(Object.class), Object.class},
            new Object[]{Select.any(true), true},
            new Object[]{Select.any("hello"), "hello"}
        };
    }

    @DataProvider(name = "SelectAnyMessage")
    public Object[][] createMessageSelections() {
        return new Object[][]{
            new Object[]{Select.message(Long.MAX_VALUE, null, Object.class), null},
            new Object[]{Select.message(String.class, Object.class), null},
            new Object[]{Select.message(Object.class), null},    
        };
    }

    @Test(dataProvider = "SelectAny")
    public void testAny(Select s, Object continuation) {
        tryLiveAcceptAll(s, continuation);
    }

    @Test(dataProvider = "SelectAnyMessage")
    public void testAnyMessage(Select s, Object continuation) {
        tryLiveAcceptAll(s, continuation);
    }

    public void tryLiveAcceptAll(Select s, Object continuation) {
        assertFalse(s.isTimedOut());
        assertFalse(s.accepts(null));
        assertTrue(s.accepts(Object.class));
        assertTrue(s.accepts(1));
        assertTrue(s.accepts(false));
    }

    @Test(dataProvider = "SelectAny")
    public void testAnyContinuations(Select s, Object continuation) {
        assertEquals(s.getContinuation(), continuation);
    }

    @Test(dataProvider = "SelectAnyMessage")
    public void testMessageContinuations(Select s, Object continuation) {
        assertEquals(s.getContinuation(), continuation);
    }
}
