package dk.statsbiblioteket.summa.common.lucene.analysis;

import junit.framework.TestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.WhitespaceTokenizer;

import java.io.StringReader;

/**
 * Unit tests for {@link SummaAnalyzer}
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class SummaAnalyzerTest extends TestCase {

    SummaAnalyzer a;

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

    public void testFooCaseFold() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream("", new StringReader("Foo"));

        assertTokens(t, "foo");
    }

    public void testFooBarCaseFold() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream("", new StringReader("Foo baR"));

        assertTokens(t, "foo", "bar");
    }

    public void testFooBarNoCaseFold() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, false);
        TokenStream t = a.reusableTokenStream("", new StringReader("Foo baR"));

        assertTokens(t, "Foo", "baR");
    }

    public void testWhiteSpace1() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream("", new StringReader(" white "));

        assertTokens(t, "white");
    }

    public void testWhiteSpace2() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream("", new StringReader("barry   white "));

        assertTokens(t, "barry", "white");
    }

    public void testWhiteSpace3() throws Exception {
        // This tes is case sensitive, just try that combo as well
        a = new SummaAnalyzer(null, true, null, true, false);
        TokenStream t = a.reusableTokenStream("", new StringReader(" I  P Jacobsen    "));

        assertTokens(t, "I", "P", "Jacobsen");
    }

    public void testPunctuation() throws Exception {
        a = new SummaAnalyzer("", true, null, true, true);
        TokenStream t = a.reusableTokenStream("", new StringReader("barry   white."));

        assertTokens(t, "barry", "white");
    }

    public void testTokenReplacements() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream(
                                   "", new StringReader(".Net vs C* Algebra?"));

        assertTokens(t, "dotnet", "vs", "cstaralgebra");
    }
}
