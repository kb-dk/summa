/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.lucene.analysis;

import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 *
 */
public class SummaStandardAnalyzerTest extends SummaAnalyzerTest {
    /** The summa analyzer. */
    private SummaAnalyzer a;

    /**
     * Checks tokens.
     * @param tokenizer The tokenizer.
     * @param tokens The tokens.
     * @throws Exception If error occur.
     */
    static void assertTokens(TokenStream tokenizer, String... tokens)
                                                               throws Exception{
        CharTermAttribute term = tokenizer.getAttribute(CharTermAttribute.class);
        int count = 0;

        while (tokenizer.incrementToken()) {
            if (count >= tokens.length) {
                fail("Too many tokens from tokenizer, found " + (count + 1)
                     + ". Expected " + tokens.length + ".");
            }
            assertEquals("Mismatch in token number " + (count + 1) + ":",
                         tokens[count], term.toString());
            count++;

        }
        assertEquals("To few tokens from tokenizer, found " + count
                     + ". Expected " + tokens.length + ".",
                     tokens.length, count);
    }

    @Override
    public final void testFooCaseFold() {
        try {
            a = new SummaStandardAnalyzer();
            TokenStream t = a.reusableTokenStream("", new StringReader("Foo"));

            assertTokens(t, "foo");
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    @Override
    public final void testFooBarCaseFold() {
        try {
            a = new SummaStandardAnalyzer();
            TokenStream t = a.reusableTokenStream("",
                                                  new StringReader("Foo baR"));

            assertTokens(t, "foo", "bar");
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    @Override
    public final void testWhiteSpace1() {
        try {
            a = new SummaStandardAnalyzer();
            TokenStream t = a.reusableTokenStream("",
                                                  new StringReader(" white "));

            assertTokens(t, "white");
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    @Override
    public final void testWhiteSpace2() {
        try {
            a = new SummaStandardAnalyzer();
            TokenStream t = a.reusableTokenStream("",
                                            new StringReader("barry   white "));

            assertTokens(t, "barry", "white");
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    @Override
    public final void testWhiteSpace3() {
        try {
            // This tes is case sensitive, just try that combo as well
            a = new SummaStandardAnalyzer();
            TokenStream t = a.reusableTokenStream("",
                                        new StringReader(" I  P Jacobsen    "));

            assertTokens(t, "i", "p", "jacobsen");
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    @Override
    public final void testPunctuation() {
        try {
            a = new SummaStandardAnalyzer();
            TokenStream t = a.reusableTokenStream("",
                                            new StringReader("barry   white."));

            assertTokens(t, "barry", "white");
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    @Override
    public final void testTokenReplacements() {
        try {
            a = new SummaStandardAnalyzer();
            TokenStream t = a.reusableTokenStream(
                                   "", new StringReader(".Net vs C* Algebra?"));

            assertTokens(t, "dotnet", "vs", "cstaralgebra");
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    @Override
    public final void testReuse() {
        try {
            a = new SummaAnalyzer(null, true, null, true, true);

            TokenStream t = a.reusableTokenStream("", new StringReader("foo"));
            assertTokens(t, "foo");

            t = a.reusableTokenStream("", new StringReader("bar"));
            assertTokens(t, "bar");

            t = a.reusableTokenStream("",
                                 new StringReader("Fast talking flip-flopper"));
            assertTokens(t, "fast", "talking", "flip", "flopper");
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }

    @Override
    public final void testDashes() {
        try {
            a = new SummaStandardAnalyzer();
            //new SummaStandardAnalyzer(null, true, null, true, true);
            TokenStream t = a.reusableTokenStream(
                                    "", new StringReader("Hr. A. Binde-Streg"));
            assertTokens(t, "hr", "a", "binde", "streg");

            t = a.reusableTokenStream(
                                "", new StringReader("Jan-Hendrik S. Hofmeyr"));
            assertTokens(t, "jan", "hendrik", "s", "hofmeyr");
        } catch (Exception e) {
            e.printStackTrace();
            fail("No exception expected here");
        }
    }
}
