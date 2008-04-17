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

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexUpdaterImplTest extends TestCase {
    public IndexUpdaterImplTest(String name) {
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
        return new TestSuite(IndexUpdaterImplTest.class);
    }

    public void testLocateFiles() throws Exception {
        new File(root, "20080417-2128").mkdir();
        File lastValid = new File(root, "20080417-2134");
        lastValid.mkdir();
        new File(root, "20080417-2129").mkdir();
        new File(root, "20080417-2129").setWritable(false);
        new File(root, "20080417-2132").createNewFile();
        new File(root, "0080417-2128").mkdir();
        new File(root, "20080417-2125").mkdir();
        new File(root, "20080417-2130-foo").mkdir();

        File[] subs = root.listFiles(IndexUpdaterImpl.SUBFOLDER_FILTER);
        Arrays.sort(subs);
        assertEquals("The number of valid folders should match",
                     3, subs.length);
        assertEquals("The last folder should be the right one",
                     lastValid, subs[subs.length-1]);
    }

    public void testTimestampFormatting() throws Exception {
        Calendar t = new GregorianCalendar(2008, 3, 17, 21, 50);
        assertEquals("The timestamp should be properly formatted",
                     "20080417-2150", 
                     String.format(IndexUpdaterImpl.TIMESTAMP_FORMAT, t));
    }
}
