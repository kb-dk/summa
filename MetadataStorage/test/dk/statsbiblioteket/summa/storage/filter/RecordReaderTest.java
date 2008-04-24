package dk.statsbiblioteket.summa.storage.filter;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;
import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordReaderTest extends TestCase {
    public RecordReaderTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSetSource() throws Exception {
        //TODO: Test goes here...
    }

    public static Test suite() {
        return new TestSuite(RecordReaderTest.class);
    }

    public void testTimestampFormatting() throws Exception {
        Calendar t = new GregorianCalendar(2008, 3, 17, 21, 50, 57);
        assertEquals("The timestamp should be properly formatted",
                     expected,
                     String.format(RecordReader.TIMESTAMP_FORMAT, t));
    }
    public void testTimestampExtraction() throws Exception {
        Calendar t = new GregorianCalendar(2008, 3, 17, 21, 50, 57);
        long expectedMS = t.getTimeInMillis();
        String formatted = String.format(RecordReader.TIMESTAMP_FORMAT, t);
        assertEquals("Parsing the formatted timestamp-containing text should "
                     + "match the expected point in time",
                     expectedMS, RecordReader.getTimestamp(new File("foo"), 
                                                           formatted));
    }

    private static final String expected =
            "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
            + "<lastRecordTimestamp>20080417-215057</lastRecordTimestamp>\n";


    public void testPatternMatching() throws Exception {
        Pattern p = Pattern.compile("([0-9]{4})([0-9]{2})([0-9]{2})-"
                                    + "([0-9]{2})([0-9]{2})([0-9]{2})");
        assertTrue("Pattern should match simple case",
                   p.matcher("20080417-215057").matches());

        //String TAG = "lastRecordTimestamp";
        Pattern pfull;/* = Pattern.compile(".*<" + TAG + ">"
                    + "([0-9]{4})([0-9]{2})([0-9]{2})-"
                    + "([0-9]{2})([0-9]{2})([0-9]{2})"
                    + "</" + TAG + ">", Pattern.DOTALL);*/
        pfull = RecordReader.TIMESTAMP_PATTERN;
        assertTrue("Pattern should match extended case",
                   pfull.matcher("<lastRecordTimestamp>20080417-215057"
                                 + "</lastRecordTimestamp>").matches());
        assertTrue("Pattern should match full case",
                   pfull.matcher(expected).matches());
    }
}
