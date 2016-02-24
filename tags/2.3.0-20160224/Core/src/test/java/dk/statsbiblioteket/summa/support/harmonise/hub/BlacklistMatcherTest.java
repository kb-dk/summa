package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;

import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hsm")
public class BlacklistMatcherTest extends TestCase {

    private static Log log = LogFactory.getLog(BlacklistMatcherTest.class);

    private BlacklistMatcher blacklistMatcher;

    public BlacklistMatcherTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        blacklistMatcher = new BlacklistMatcher(Arrays.asList("myfield:", "foo:bar", ":myterm"));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        blacklistMatcher = null;
    }

    public void testNonBlacklistedFieldNoText() {
        Term term = new Term("author", "");
        assertFalse(blacklistMatcher.blacklistContains(term));
    }

    public void testNonBlacklistedTextNoField() {
        Term term = new Term("", "text");
        assertFalse(blacklistMatcher.blacklistContains(term));
    }

    public void testBlacklistedFieldNoText() {
        Term term = new Term("myfield", "");
        assertTrue(blacklistMatcher.blacklistContains(term));
    }

    public void testBlacklistedFieldWithText() {
        Term term = new Term("myfield", "anything");
        assertTrue(blacklistMatcher.blacklistContains(term));
    }

    public void testBlacklistedTextNoField() {
        Term term = new Term("", "myterm");
        assertTrue(blacklistMatcher.blacklistContains(term));
    }

    public void testBlacklistedTextWithField() {
        Term term = new Term("anything", "myterm");
        assertTrue(blacklistMatcher.blacklistContains(term));
    }

    public void testBlacklistedFullySpecifiedTerm() {
        Term term = new Term("foo", "bar");
        assertTrue(blacklistMatcher.blacklistContains(term));
    }

    public void testThatFullySpecifiedTermDoesNotPartiallyMatchField() {
        Term term = new Term("foo", "");
        assertFalse(blacklistMatcher.blacklistContains(term));
    }

    public void testThatFullySpecifiedTermDoesNotPartiallyMatchText() {
        Term term = new Term("", "bar");
        assertFalse(blacklistMatcher.blacklistContains(term));
    }

    public void testNonBlacklistedFullySpecifiedTerm() {
        Term term = new Term("baz", "qux");
        assertFalse(blacklistMatcher.blacklistContains(term));
    }

    public void testBlacklistedTermQuery() {
        TermQuery termQuery = new TermQuery(new Term("foo", "bar"));
        assertTrue(blacklistMatcher.blacklistContains(termQuery));
    }

    public void testNonBlacklistedTermQuery() {
        TermQuery termQuery = new TermQuery(new Term("fred", "barney"));
        assertFalse(blacklistMatcher.blacklistContains(termQuery));
    }

    public void testBlacklistedPhraseQuery1() {
        PhraseQuery phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("foo", "bar"));
        assertTrue(blacklistMatcher.blacklistContains(phraseQuery));
    }

    public void testBlacklistedPhraseQuery2() {
        PhraseQuery phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("myfield", "bar"));
        phraseQuery.add(new Term("myfield", "baz"));
        assertTrue(blacklistMatcher.blacklistContains(phraseQuery));
    }

    public void testNonBlacklistedPhraseQuery() {
        PhraseQuery phraseQuery = new PhraseQuery();
        phraseQuery.add(new Term("ok", "bar"));
        phraseQuery.add(new Term("ok", "baz"));
        assertFalse(blacklistMatcher.blacklistContains(phraseQuery));
    }

    public void testBlacklistedTermRangeQuery() {
        TermRangeQuery termRangeQuery =
                new TermRangeQuery("myfield", new BytesRef("bar"), new BytesRef("baz"), true, true);
        assertTrue(blacklistMatcher.blacklistContains(termRangeQuery));
    }

    public void testNonBlacklistedTermRangeQuery() {
        TermRangeQuery termRangeQuery =
                new TermRangeQuery("foo", new BytesRef("bar"), new BytesRef("baz"), true, true);
        assertFalse(blacklistMatcher.blacklistContains(termRangeQuery));
    }

    public void testBlacklistedPrefixQuery() {
        PrefixQuery prefixQuery = new PrefixQuery(new Term("myfield", "bar"));
        assertTrue(blacklistMatcher.blacklistContains(prefixQuery));
    }

    public void testNonBlacklistedPrefixQuery() {
        PrefixQuery prefixQuery = new PrefixQuery(new Term("foo", "bar"));
        assertFalse(blacklistMatcher.blacklistContains(prefixQuery));
    }

    public void testBlacklistedFuzzyQuery() {
        FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term("myfield", "bar"));
        assertTrue(blacklistMatcher.blacklistContains(fuzzyQuery));
    }

    public void testNonBlacklistedFuzzyQuery() {
        FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term("foo", "bar"));
        assertFalse(blacklistMatcher.blacklistContains(fuzzyQuery));
    }
}
