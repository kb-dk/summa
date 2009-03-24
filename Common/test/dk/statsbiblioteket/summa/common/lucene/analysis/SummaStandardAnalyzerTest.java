package dk.statsbiblioteket.summa.common.lucene.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.WhitespaceTokenizer;

import java.io.StringReader;

/**
 *
 */
public class SummaStandardAnalyzerTest extends SummaAnalyzerTest {

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

    public void testFooCaseFold() throws Exception {
        a = new SummaStandardAnalyzer();
        TokenStream t = a.reusableTokenStream("", new StringReader("Foo"));

        assertTokens(t, "foo");
    }

    public void testFooBarCaseFold() throws Exception {
        a = new SummaStandardAnalyzer();
        TokenStream t = a.reusableTokenStream("", new StringReader("Foo baR"));

        assertTokens(t, "foo", "bar");
    }

    public void testWhiteSpace1() throws Exception {
        a = new SummaStandardAnalyzer();
        TokenStream t = a.reusableTokenStream("", new StringReader(" white "));

        assertTokens(t, "white");
    }

    public void testWhiteSpace2() throws Exception {
        a = new SummaStandardAnalyzer();
        TokenStream t = a.reusableTokenStream("", new StringReader("barry   white "));

        assertTokens(t, "barry", "white");
    }

    public void testWhiteSpace3() throws Exception {
        // This tes is case sensitive, just try that combo as well
        a = new SummaStandardAnalyzer();
        TokenStream t = a.reusableTokenStream("", new StringReader(" I  P Jacobsen    "));

        assertTokens(t, "i", "p", "jacobsen");
    }

    public void testPunctuation() throws Exception {
        a = new SummaStandardAnalyzer();
        TokenStream t = a.reusableTokenStream("", new StringReader("barry   white."));

        assertTokens(t, "barry", "white");
    }

    public void testTokenReplacements() throws Exception {
        a = new SummaStandardAnalyzer();
        TokenStream t = a.reusableTokenStream(
                                   "", new StringReader(".Net vs C* Algebra?"));

        assertTokens(t, "dotnet", "vs", "cstaralgebra");
    }

    public void testReuse() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);

        TokenStream t = a.reusableTokenStream("", new StringReader("foo"));
        assertTokens(t, "foo");

        t = a.reusableTokenStream("", new StringReader("bar"));
        assertTokens(t, "bar");

        t = a.reusableTokenStream("",
                                  new StringReader("Fast talking flip-flopper"));
        assertTokens(t, "fast", "talking", "flip", "flopper");
    }

    public void testDashes() throws Exception {
        a = new SummaStandardAnalyzer();//new SummaStandardAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream(
                                   "", new StringReader("Hr. A. Binde-Streg"));
        assertTokens(t, "hr", "a", "binde", "streg");

        t = a.reusableTokenStream(
                                "", new StringReader("Jan-Hendrik S. Hofmeyr"));
        assertTokens(t, "jan", "hendrik", "s", "hofmeyr");
    }

}
