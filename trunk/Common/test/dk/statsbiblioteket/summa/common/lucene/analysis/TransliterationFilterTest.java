package dk.statsbiblioteket.summa.common.lucene.analysis;

import junit.framework.TestCase;
import org.apache.lucene.analysis.*;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import dk.statsbiblioteket.util.reader.StringReplacer;
import dk.statsbiblioteket.util.reader.ReplaceReader;
import dk.statsbiblioteket.util.reader.ReplaceFactory;
import dk.statsbiblioteket.util.Strings;

/**
 * Tests for {@link TransliterationFilter}
 */
public class TransliterationFilterTest extends TestCase {

    TransliterationFilter t;
    Token tok;

    public void setUp() {
        tok = new Token();
    }

    static void assertTokens(TokenStream tokenizer, String... tokens)
                                                               throws Exception{
        Token tok = new Token();
        int count = 0;

        while ((tok = tokenizer.next(tok)) != null) {
            if (count >= tokens.length) {
                fail("Too many tokens from tokenizer, found " + (count+1)
                     + ". Expected " + tokens.length + ".");
            }

            assertEquals("Mismatch in token number " + (count + 1) + ":",
                         tokens[count], tok.term());
            count++;
        }

        assertEquals("To few tokens from tokenizer, found " + count
                     + ". Expected " + tokens.length + ".",
                     tokens.length, count);
    }

    static TokenStream getStream(String text) {
        return new LowerCaseFilter(
                new WhitespaceTokenizer(
                        new StringReader(text)));
    }

    public void testOneTokenWithDefaultRules() throws Exception {
        t = new TransliterationFilter(getStream("foo"),
                                        null, false);
        assertTokens(t, "foo");
    }

    public void testOneAlternativeTokenWithDefaultRules() throws Exception {
        t = new TransliterationFilter(getStream("baz"),
                                        null, false);
        assertTokens(t, "baz");
    }

    public void testTwoTokensWithDefaultRules() throws Exception {
        t = new TransliterationFilter(getStream("foo bar"),
                                        null, false);
        assertTokens(t, "foo", "bar");
    }

    public void testThreeTokensWithDefaultRules() throws Exception {
        t = new TransliterationFilter(getStream("foo bar baz"),
                                        null, false);
        assertTokens(t, "foo", "bar", "baz");
    }

    public void testThreeAlternativeTokensWithDefaultRules() throws Exception {
        t = new TransliterationFilter(getStream("fookbarkbaz"),
                                        null, false);
        assertTokens(t, "fookbarkbaz");
    }

    public void testOneDanishTokensWithDefaultRules() throws Exception {
        t = new TransliterationFilter(getStream("Åkjær"),
                                        null, false);
        assertTokens(t, "aakjær");
    }

    /*public void testFoo() throws Exception {
        Map<String,String> rules = new HashMap<String,String>();
        rules.put("Å", "aa");
        ReplaceReader r = ReplaceFactory.getReplacer(new StringReader("Åkjær"),
                                                     rules);
        assertEquals("aa");
    }*/
}
