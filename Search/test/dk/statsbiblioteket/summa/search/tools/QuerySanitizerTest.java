package dk.statsbiblioteket.summa.search.tools;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.Arrays;

public class QuerySanitizerTest extends TestCase {
    private QuerySanitizer sanitizer = new QuerySanitizer(Configuration.newMemoryBased());

    private QuerySanitizer.SanitizedQuery.CHANGE ERROR = QuerySanitizer.SanitizedQuery.CHANGE.error;
    private QuerySanitizer.SanitizedQuery.CHANGE SYNTAX = QuerySanitizer.SanitizedQuery.CHANGE.summasyntax;

    public QuerySanitizerTest(String name) {
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
        return new TestSuite(QuerySanitizerTest.class);
    }

    public void testUnbalancedQuotes() {
        assertSanitize("Unbalanced quotes",              "foo bar",         "foo bar\"",         ERROR);
        assertSanitize("Unbalanced quotes",              "foo bar",         "\"foo bar",         ERROR);
        assertSanitize("Unbalanced quotes",              "foo bar zoo",     "foo \"bar zoo",     ERROR);
        assertSanitize("Balanced quotes",                "foo \"bar zoo\"", "foo \"bar zoo\"");
        assertSanitize("Escaped quotes",                 "foo \\\"bar zoo", "foo \\\"bar zoo");
        assertSanitize("Unbalanced quotes with escaped", "foo \\\"bar zoo", "\"foo \\\"bar zoo", ERROR);
    }

    public void testUnbalancedParentheses() {
        assertSanitize("Quoted parentheses",          "foo \"(bar zoo\"",    "foo \"(bar zoo\"");
        assertSanitize("Unbalanced parentheses",      "foo bar",             "foo bar)",     ERROR);
        assertSanitize("Unbalanced parentheses",      "foo bar",             "(foo bar",     ERROR);
        assertSanitize("Unbalanced parentheses",      "foo bar zoo",         "foo (bar zoo", ERROR);
        assertSanitize("Balanced parentheses",        "foo (bar zoo)",       "foo (bar zoo)");
        assertSanitize("Double balanced parentheses", "foo (bar (baz goo))", "foo (bar (baz goo))");
    }

    public void testColonWithMissingTerms() {
        assertSanitize("Bad colon",     "foo bar",      "foo: bar", SYNTAX);
        assertSanitize("Bad colon",     "foo bar",      "foo bar:", SYNTAX);
        assertSanitize("OK colon",      "foo:bar",      "foo:bar");
        assertSanitize("Quoted colon",  "\"foo: bar\"",  "\"foo: bar\"");
        assertSanitize("Escaped colon", "foo\\: bar",   "foo\\: bar");
    }

    public void testTrailingExclamationMark() {
        assertSanitize("Bad trailing exclamation mark",     "foo bar",     "foo bar!", SYNTAX);
        assertSanitize("Bad trailing exclamation mark",     "foo bar",     "foo! bar", SYNTAX);
        assertSanitize("Bad trailing exclamation mark",     "foo!bar",      "foo!bar");
        assertSanitize("Quoted trailing exclamation mark",  "\"foo bar!\"", "\"foo bar!\"");
        assertSanitize("Escaped trailing exclamation mark", "foo bar\\!",   "foo bar\\!");
    }

    public void testMixedProblems() {
        assertSanitize("Gallimaufry", "foo bar",        "\"foo (bar!",     ERROR, ERROR, SYNTAX);
        assertSanitize("Hodgepodge",  "foo \"(\"bar",   "foo \"(\"bar!",   SYNTAX);
        assertSanitize("Potpourri",   "foo (bar\"!\")", "foo (bar\"!\")");
        assertSanitize("Salmagundi",  "foo\\!\\\" bar",     "foo\\!\\\" bar)", ERROR);
    }

    private void assertSanitize(
        String message, String expected, String query, QuerySanitizer.SanitizedQuery.CHANGE... expectedChanges) {
        assertEquals(message + ": '" + query + "'", expected, sanitizer.sanitize(query).getLastQuery());

        String expectedC = Strings.join(Arrays.asList(expectedChanges), ", ");
        String actualC = Strings.join(sanitizer.sanitize(query).getChanges(), ", ");
        assertEquals(message + ": '" + query + "'. Change type", expectedC, actualC);
    }
}
