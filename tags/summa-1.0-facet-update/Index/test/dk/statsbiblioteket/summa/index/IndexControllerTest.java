package dk.statsbiblioteket.summa.index;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.index.IndexCommon;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexControllerTest extends TestCase {
    public IndexControllerTest(String name) {
        super(name);
    }
    File root = new File(System.getProperty("java.io.tmpdir"), "subdirtest");

    public void setUp() throws Exception {
        super.setUp();
        root.mkdirs();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (root.exists()) {
            Files.delete(root);
        }
    }

    public static Test suite() {
        return new TestSuite(IndexControllerTest.class);
    }

    public void testLocateFiles() throws Exception {
        new File(root, "20080417-212800").mkdir();
        File lastValid = new File(root, "20080417-213400");
        lastValid.mkdir();
        new File(root, "20080417-212900").mkdir();
        new File(root, "20080417-212900").setWritable(false);
        new File(root, "20080417-213200").createNewFile();
        new File(root, "0080417-212800").mkdir();
        new File(root, "20080417-212500").mkdir();
        new File(root, "20080417-213000-foo").mkdir();

        File[] subs = root.listFiles(IndexCommon.SUBFOLDER_FILTER_WRITE);
        Arrays.sort(subs);
        assertEquals("The number of valid folders should match. Got "
                     + Arrays.toString(subs),
                     3, subs.length);
        assertEquals("The last folder should be the right one",
                     lastValid, subs[subs.length-1]);
    }

    public void testTimestampFormatting() throws Exception {
        Calendar t = new GregorianCalendar(2008, 3, 17, 21, 50, 54);
        assertEquals("The timestamp should be properly formatted",
                     "20080417-215054", 
                     String.format(IndexCommon.TIMESTAMP_FORMAT, t));
    }
}



