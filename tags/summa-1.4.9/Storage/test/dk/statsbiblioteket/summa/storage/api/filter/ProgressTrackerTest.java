package dk.statsbiblioteket.summa.storage.api.filter;

import junit.framework.TestCase;

import java.io.File;

import dk.statsbiblioteket.util.Files;

/**
 * Unit tests for {@link ProgressTracker}
 */
public class ProgressTrackerTest extends TestCase {


    File testDir = new File(System.getProperty("java.io.tmpdir"),
                            "summa-progresstracker-test");
    File progressFile = new File(testDir, "test-progress.xml");
    ProgressTracker p;

    public void setUp() throws Exception {
        testDir.mkdirs();
        Files.delete(testDir);
        testDir.mkdirs();
    }

    public void testStart() throws Exception {
        p = new ProgressTracker(progressFile, 1, 10);

        assertEquals(0, p.getLastUpdate());

        p.loadProgress();
        assertEquals(0, p.getLastUpdate());
    }

    public void testUpdateBatch1() throws Exception {
        p = new ProgressTracker(progressFile, 1, 10000);

        assertEquals(0, p.getLastUpdate());

        // This works because the batch size is exactly 1
        p.updated(System.currentTimeMillis());
        assertTrue(p.getLastUpdate() > 0);
    }

    public void testUpdateBatch2() throws Exception {
        p = new ProgressTracker(progressFile, 2, 10000);

        assertEquals(0, p.getLastUpdate());

        // Update is reflected in the progress tracker but not stored
        p.updated(System.currentTimeMillis());
        assertFalse(progressFile.exists());
        long last = p.getLastUpdate();
        assertTrue(p.getLastUpdate() > 0);

        assertEquals(last, p.getLastUpdate());
    }
}
