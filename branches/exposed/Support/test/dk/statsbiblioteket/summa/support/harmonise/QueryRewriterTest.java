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
        assertIdentity("foo", "foo");
        assertIdentity("(+foo +- +bar)", "foo - bar");
        assertIdentity("(+foo +bar)", "foo AND bar");
    }

    public void testScoreAdjustmentPlain() throws ParseException {
        assertIdentity("foo^1.2", "foo^1.2");
    }

    public void testScoreAdjustmentPlainConcat() throws ParseException {
        assertIdentity("foo-bar^1.2", "foo-bar^1.2");
    }

    public void testScoreAssignmentAndAdjustmentPlain() throws ParseException {
        final String USER_INPUT = "foo bar";
        String rewritten = assignWeight(USER_INPUT, 1.2f);
        // So far so good. Now to eat our own dog food
        assertIdentity("(+foo^1.2 +bar^1.2)", rewritten);
        // Hint: Maybe quoting of - works, but does that change it into a phrase-query?
    }

    public void testScoreAssignmentAndAdjustmentDivider() throws ParseException {
        final String USER_INPUT = "foo - bar";
        String rewritten = assignWeight(USER_INPUT, 1.2f);
        // So far so good. Now to eat our own dog food
        assertIdentity("(+foo^1.2 +-^1.2 +bar^1.2)", rewritten);
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

    public void testScoreAdjustmentDivider() throws ParseException {
        assertIdentity("(+foo +-^1.2 +bar)", "foo -^1.2 bar");
    }

    public void testScoreAdjustmentDividerQuoted() throws ParseException {
        assertIdentity("(+foo +-^1.2 +bar)", "foo \"-\"^1.2 bar"); // Shouldn't - be in quotes
    }

    private void assertIdentity(String expected, String input) throws ParseException {
        assertEquals("Rewrite should be correct",
                     expected, new QueryRewriter(new QueryRewriter.Event()).rewrite(input));
    }
}
