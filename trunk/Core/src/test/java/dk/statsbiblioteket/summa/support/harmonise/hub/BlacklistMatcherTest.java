package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;

import java.util.Arrays;
import java.util.HashSet;

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
        blacklistMatcher = new BlacklistMatcher(new HashSet<String>(Arrays.asList("myfield:", "foo:bar", ":myterm")));
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
}
