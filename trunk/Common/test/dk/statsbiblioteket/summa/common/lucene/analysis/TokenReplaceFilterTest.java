package dk.statsbiblioteket.summa.common.lucene.analysis;

import junit.framework.TestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.Tokenizer;

import java.io.StringReader;

/**
 * Unit tests for {@link TokenReplaceFilter}
 */
public class TokenReplaceFilterTest extends TestCase {

    TokenReplaceFilter filter;
    TokenStream stream;
    Token tok;

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
        return new WhitespaceTokenizer(new StringReader(text));
    }

    public void testDummy() throws Exception {
        stream = getStream("foo");
        assertTokens(stream, "foo");
    }

    public void testSingleTokenNoReplace() throws Exception {
        filter = new TokenReplaceFilter(getStream("foo"));
        assertTokens(filter, "foo");
    }

    public void testSingleTokenCPlusPlusReplace() throws Exception {
        filter = new TokenReplaceFilter(getStream("c++"));
        assertTokens(filter, "cplusplus");
    }

    public void testTwoTokenTricksReplace() throws Exception {
        filter = new TokenReplaceFilter(getStream("c+ c++"));
        assertTokens(filter, "c+", "cplusplus");
    }

    public void testOneDanishTokenWithDefaultRulesAlt1() throws Exception {
        // Test an alternative constructor
        filter = new TokenReplaceFilter(getStream("Åkjær"), "", false);
        assertTokens(filter, "Åkjær");
    }

}
