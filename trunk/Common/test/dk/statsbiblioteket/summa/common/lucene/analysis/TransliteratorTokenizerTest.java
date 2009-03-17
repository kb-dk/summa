package dk.statsbiblioteket.summa.common.lucene.analysis;

import junit.framework.TestCase;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;

import java.io.StringReader;

/**
 * Tests for {@link TransliteratorTokenizer}
 */
public class TransliteratorTokenizerTest extends TestCase {

    TransliteratorTokenizer t;
    Token tok;

    public void setUp() {
        tok = new Token();
    }

    static void assertTokens(Tokenizer tokenizer, String... tokens)
                                                               throws Exception{
        Token tok = new Token();
        int count = 0;

        while ((tok = tokenizer.next(tok)) != null) {
            if (count >= tokens.length) {
                fail("Too many tokens from tokenizer, " + (count+1)
                     + ". Expected " + tokens.length);
            }

            assertEquals("Mismatch in token number " + (count + 1),
                         tokens[count], tok.term());
            count++;
        }

        assertEquals("To few tokens from tokenizer, " + count
                     + ". Expected " + tokens.length, tokens.length, count);
    }

    public void testOneTokenWithDefaultRules() throws Exception {
        t = new TransliteratorTokenizer(new StringReader("foo"),
                                        null, false);
        assertTokens(t, "foo");
    }

    public void testOneAlternativeTokenWithDefaultRules() throws Exception {
        t = new TransliteratorTokenizer(new StringReader("baz"),
                                        null, false);
        assertTokens(t, "baz");
    }

    public void testTwoTokensWithDefaultRules() throws Exception {
        t = new TransliteratorTokenizer(new StringReader("foo bar"),
                                        null, false);
        assertTokens(t, "foo", "bar");
    }

    public void testThreeTokensWithDefaultRules() throws Exception {
        t = new TransliteratorTokenizer(new StringReader("foo bar baz"),
                                        null, false);
        assertTokens(t, "foo", "bar", "baz");
    }

    public void testThreeAlternativeTokensWithDefaultRules() throws Exception {
        t = new TransliteratorTokenizer(new StringReader("fookbarkbaz"),
                                        null, false);
        assertTokens(t, "fookbarkbaz");
    }

    public void testOneDanishTokensWithDefaultRules() throws Exception {
        t = new TransliteratorTokenizer(new StringReader("Åkjær"),
                                        null, false);
        assertTokens(t, "aakjær");
    }
}
