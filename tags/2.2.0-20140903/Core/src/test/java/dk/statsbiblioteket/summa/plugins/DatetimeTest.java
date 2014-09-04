package dk.statsbiblioteket.summa.plugins;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DatetimeTest extends TestCase {
    public static final String tOK = "2010-01-30T15:58:45+0200";
    public static final String tOK2 = "2010-12-06T15:02:45";
    public static final String tDate = "2010-01-30";
    public static final String tDefect = "200-01-30T15:58:45+0200";

    public DatetimeTest(String name) {
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
        return new TestSuite(DatetimeTest.class);
    }

    public void testDateExpand() throws Exception {
        assertEquals("Locale en",
                     "'2010-01-30 30/01-2010 01/30-2010 30/01/2010 20100130 "
                     + "30/01 01/30 01-30 30/1 1/30 1-30 January Jan'",
                     "'" + Datetime.dateExpand(tOK, "en") + "'");
        assertEquals("Locale da",
                     "'2010-01-30 30/01-2010 01/30-2010 30/01/2010 20100130 " 
                     + "30/01 01/30 01-30 30/1 1/30 1-30 januar jan'",
                     "'" + Datetime.dateExpand(tOK, "da") + "'");
        assertEquals("Locale en part",
                     "'2010-12-06 06/12-2010 12/06-2010 06/12/2010 20101206 "
                     + "06/12 12/06 12-06 6/12 12/6 12-6 December Dec'",
                     "'" + Datetime.dateExpand(tOK2, "en") + "'");
        assertEquals("Locale en date only",
                     "'2010-01-30 30/01-2010 01/30-2010 30/01/2010 20100130 "
                     + "30/01 01/30 01-30 30/1 1/30 1-30 January Jan'",
                     "'" + Datetime.dateExpand(tDate, "en") + "'");
        assertEquals("Defect",
                     tDefect, Datetime.dateExpand(tDefect, ""));
    }

    public void testTimeExpand() throws Exception {
        assertEquals("Full", "'15:58 15.58 1558 15h58m 15:58:15 15h58m15s'",
                     "'" + Datetime.timeExpand(tOK, "") + "'");
        assertEquals("Part", "'15:02 15.02 1502 15h02m 15:02:15 15h02m15s'",
                     "'" + Datetime.timeExpand(tOK2, "") + "'");
        assertEquals("Defect", tDefect,
                     Datetime.timeExpand(tDefect, ""));
    }

    public void testDivide() throws Exception {
        assertEquals("2010/01/30/15/58/45", Datetime.divide(tOK, 1, 100));
        assertEquals("2010/01/30/15", Datetime.divide(tOK, 1, 4));
        assertEquals("2010", Datetime.divide(tOK, 1, 1));
        assertEquals("15/58/45", Datetime.divide(tOK, 4, 6));

        assertEquals("Date", "2010/01/30", Datetime.divide(tDate, 1, 100));
        assertEquals("Defect", "", Datetime.divide(tDefect, 1, 100));
    }

    public void testSolrDateTimeYear() {
        String EXPECTED = "2013-01-01T00:00:00Z";
        String[] TESTS = new String[]{"2013", "mydate 2013done", "20130a"};
        assertSolrDates(EXPECTED, TESTS);
    }

    public void testSolrDateTimeMonth() {
        String EXPECTED = "2013-02-01T00:00:00Z";
        String[] TESTS = new String[]{"2013-02", "mydate 2013-02done", "2013-020b", "201302", "2013/02"};
        assertSolrDates(EXPECTED, TESTS);
    }

    public void testSolrDateTimeDay() {
        String EXPECTED = "2013-02-03T00:00:00Z";
        String[] TESTS = new String[]{"2013-02-03", "mydate 2013-02-03done", "2013-02-03f", "20130203"};
        assertSolrDates(EXPECTED, TESTS);
    }

    private void assertSolrDates(String expected, String[] tests) {
        for (String test: tests) {
            assertEquals("Input is '" + test + "'", expected, Datetime.solrDateTime(test));
        }
    }
}
