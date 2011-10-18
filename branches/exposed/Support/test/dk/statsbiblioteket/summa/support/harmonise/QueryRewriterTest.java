package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryRewriterTest extends TestCase {
    public QueryRewriterTest(String name) {
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
        return new TestSuite(QueryRewriterTest.class);
    }

    public void testNoAdjustment() throws ParseException {
        assertIdentity("\"foo\"", "foo");
        assertIdentity("(+\"foo\" +\"-\" +\"bar\")", "foo - bar");
        assertIdentity("(+\"foo\" +\"bar\")", "foo AND bar");
    }

    public void testScoreAdjustmentPlain() throws ParseException {
        assertIdentity("\"foo\"^1.2", "foo^1.2");
    }

    public void testScoreAdjustmentPlainConcat() throws ParseException {
        assertIdentity("\"foo-bar\"^1.2", "foo-bar^1.2");
    }

    public void testScoreAssignmentAndAdjustmentPlain() throws ParseException {
        String userInput = "foo bar";
        String rewritten = assignWeight(userInput, 1.2f);
        // So far so good. Now to eat our own dog food
        assertIdentity("(+\"foo\"^1.2 +\"bar\"^1.2)", rewritten);
    }

    public void testScoreAssignmentAndAdjustmentDivider() throws ParseException {
        String userInput = "foo - bar";
        String rewritten = assignWeight(userInput, 1.2f);
        // So far so good. Now to eat our own dog food
        assertIdentity("(+\"foo\"^1.2 +\"-\"^1.2 +\"bar\"^1.2)", rewritten);
    }

    private String assignWeight(String query, final float weight) throws ParseException {
        QueryRewriter.Event event = new QueryRewriter.Event() {
            @Override
            public Query onQuery(TermQuery query) {
                query.setBoost(weight);
                return query;
            }
        };
        return new QueryRewriter(event).rewrite(query);
    }

    public void testScoreAdjustmentDividerQuoted() throws ParseException {
        assertIdentity("(+\"foo\" +\"-\"^1.2 +\"bar\")", "foo \"-\"^1.2 bar");
    }

    public void testQuoting() throws ParseException {
        String query = "- ";
        assertIdentity("\"-\"^1.2", assignWeight(query, 1.2f));
    }

    public void testEscapedWhitespace1() throws ParseException {
        String query = "new:umat\\ 11*";
        assertIdentity("new:umat\\ 11*", query);
    }

    public void testEscapedWhitespace2() throws ParseException {
        String query = "new:umat\\ 11";
        assertIdentity("(+new:\"umat\" +new:\"11\")", query); // We accept this because one would normally use quotes instead of escaping
    }

    public void testEscapedWhitespace3() throws ParseException {
        String query = "new:umat\\ 11?";
        assertIdentity("new:umat\\ 11?", query);
    }

    public void testBooleanQueryInBooleanQuery() throws ParseException {
        String query = "foo AND (bar OR - )";
        assertIdentity("(+\"foo\" +(\"bar\" OR \"-\"))", query);
    }

    private void assertIdentity(String expected, String input) throws ParseException {
        assertEquals("Rewrite should be correct",
                     expected, new QueryRewriter(new QueryRewriter.Event()).rewrite(input));
    }
}
