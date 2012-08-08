package dk.statsbiblioteket.summa.search.tools;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;

public class QueryPhraserTest extends TestCase {
    public QueryPhraserTest(String name) {
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
        return new TestSuite(QueryPhraserTest.class);
    }

    public void testSimplePhrasing() throws ParseException {
        assertPhrase("foo bar", "(foo bar) OR \"foo bar\"~2");
        assertPhrase("foo bar zoo", "(foo bar zoo) OR \"foo bar zoo\"~2");
        assertPhrase("(foo bar)", "((foo bar)) OR \"foo bar\"~2");
        assertPhrase("foo^2 bar", "(foo^2 bar) OR \"foo bar\"~2");
        assertPhrase("((foo bar))", "(((foo bar))) OR \"foo bar\"~2"); // '((...))' is reduced by the QueryParser
    }

    public void testNoPhrasing() throws ParseException {
        assertPhrase("foo", "foo");
        assertPhrase("foo -bar", "foo -bar");
        assertPhrase("\"foo bar\"", "\"foo bar\"");
        assertPhrase("foo (bar moo)", "foo (bar moo)");
    }

    public void testFieldGrouping() throws Exception {
        assertPhrase("title:(php xml)", "title:(php xml)");
    }

    public void testQualified() throws Exception {
        assertPhrase("(foo:bar moo row)", "(foo:bar moo row)");
    }

    public void testConfigSlope() throws ParseException {
        final String QUERY = "foo bar";
        QueryPhraser qp = new QueryPhraser(Configuration.newMemoryBased(QueryPhraser.CONF_SLOP, 0));
        String actual = qp.rewrite(new Request(), QUERY);
        assertEquals("(foo bar) OR \"foo bar\"~0", actual);
    }

    public void testSearchSlope() throws ParseException {
        final String QUERY = "foo bar";
        QueryPhraser qp = new QueryPhraser(Configuration.newMemoryBased());
        String actual = qp.rewrite(new Request(QueryPhraser.SEARCH_SLOP, 1), QUERY);
        assertEquals("(foo bar) OR \"foo bar\"~1", actual);
    }

    public void testScoreTweak() throws ParseException {
        QueryRewriter qr = new QueryRewriter(Configuration.newMemoryBased(), null, null);
        QueryParser qp = QueryRewriter.createDefaultQueryParser();
        String parsed = qr.toString(qp.parse("\"foo bar\"~1^2"));
        assertEquals("The parsed phrase query should keep the boost", "\"foo bar\"~1^2.0", parsed);
    }

    public void assertPhrase(String query, String expected) throws ParseException {
        QueryPhraser qp = new QueryPhraser(Configuration.newMemoryBased());
        String actual = qp.rewrite(new Request(), query);
        assertEquals(expected, actual);
    }
}
