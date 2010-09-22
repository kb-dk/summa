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

import dk.statsbiblioteket.util.reader.ReplaceFactory;
import dk.statsbiblioteket.util.reader.ReplaceReader;

import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * Unit tests for {@link SummaAnalyzer}.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
public class SummaAnalyzerTest extends TestCase {
    /** Summa annalyzer. */
    private SummaAnalyzer a;

    /**
     * Check tokens.
     * @param tokenizer The token stream.
     * @param tokens The token.
     * @throws Exception If error occurs.
     */
    static void assertTokens(TokenStream tokenizer, String... tokens)
                                                              throws Exception {
        TermAttribute term = tokenizer.getAttribute(TermAttribute.class);
        int count = 0;

        while (tokenizer.incrementToken()) {
            if (count >= tokens.length) {
                fail("Too many tokens from tokenizer, found " + (count + 1)
                     + ". Expected " + tokens.length + ".");
            }

            assertEquals("Mismatch in token number " + (count + 1) + ":",
                         tokens[count], term.term());
            count++;

        }

        assertEquals("To few tokens from tokenizer, found " + count
                     + ". Expected " + tokens.length + ".",
                     tokens.length, count);
    }

    /**
     * Return a token stream.
     * @param text Test which should be on the stream.
     * @return The token stream.
     */
    @SuppressWarnings("deprecation")
    static TokenStream getStream(String text) {
        return new LowerCaseFilter(
                new WhitespaceTokenizer(
                        new StringReader(text)));
    }

    /**
     * Test char to char array.
     */
    public void testCharToCharArray() {
        try {
            ReplaceFactory factory = new ReplaceFactory(
                    RuleParser.parse(RuleParser.sanitize(
                            "", true, Rules.ALL_TRANSLITERATIONS)));
            ReplaceReader replacer = factory.getReplacer();
            System.out.println("Got Replacer " + replacer);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Tset foo case fold.
     */
    public void testFooCaseFold() {
        try {
            a = new SummaAnalyzer(null, true, null, true, true);
            TokenStream t = a.reusableTokenStream("", new StringReader("Foo"));

            assertTokens(t, "foo");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test lower case only.
     */
    public void testLowerCaseOnly() {
        try {
            a = new SummaAnalyzer("", false, "", false, true);
            TokenStream t = a.reusableTokenStream("", new StringReader(
                "Foo-%4g 123/erW'd;_foo"));

            assertTokens(t, "foo-%4g", "123/erw'd;_foo");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test foo bar case fold.
     */
    public void testFooBarCaseFold() {
        try {
            a = new SummaAnalyzer(null, true, null, true, true);
            TokenStream t = a.reusableTokenStream("",
                                                  new StringReader("Foo baR"));
            assertTokens(t, "foo", "bar");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test foo bar no case fold.
     */
    public void testFooBarNoCaseFold() {
        try {
            a = new SummaAnalyzer(null, true, null, true, false);
            TokenStream t = a.reusableTokenStream("",
                                                  new StringReader("Foo baR"));
            assertTokens(t, "Foo", "baR");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test white space 1.
     */
    public void testWhiteSpace1() {
        try {
            a = new SummaAnalyzer(null, true, null, true, true);
            TokenStream t = a.reusableTokenStream("",
                                                  new StringReader(" white "));
            assertTokens(t, "white");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test long.
     */
    public void testLong() {
        try {
            a = new SummaAnalyzer(null, true, null, true, true);
            TokenStream t = a.reusableTokenStream("", new StringReader(
                    "Captain Planet once again saves the world. This time by "
                    + "getting rid of dandruff."));

            assertTokens(t, "captain", "planet", "once", "again", "saves",
                         "the", "world", "this", "time", "by", "getting", "rid",
                         "of", "dandruff");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test white space 2.
     */
    public void testWhiteSpace2() {
        try {
            a = new SummaAnalyzer(null, true, null, true, true);
            TokenStream t = a.reusableTokenStream("",
                                            new StringReader("barry   white "));

            assertTokens(t, "barry", "white");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test white space 3.
     */
    public void testWhiteSpace3() {
        try {
            // This test is case sensitive, just try that combo as well
            a = new SummaAnalyzer(null, true, null, true, false);
            TokenStream t = a.reusableTokenStream("",
                                        new StringReader(" I  P Jacobsen    "));

            assertTokens(t, "I", "P", "Jacobsen");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test punktucation.
     */
    public void testPunctuation() {
        try {
            a = new SummaAnalyzer("", true, null, true, true);
            TokenStream t = a.reusableTokenStream("",
                                            new StringReader("barry   white."));

            assertTokens(t, "barry", "white");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test token replacements.
     */
    public void testTokenReplacements() {
        try {
            a = new SummaAnalyzer(null, true, null, true, true);
            TokenStream t = a.reusableTokenStream(
                                   "", new StringReader(".Net vs C* Algebra?"));

            assertTokens(t, "dotnet", "vs", "cstaralgebra");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test reuse.
     */
    public void testReuse() {
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
            fail("Exception thrown");
        }
    }

    /**
     * Test Dashes.
     */
    public void testDashes() {
        try {
            a = new SummaAnalyzer(null, true, null, true, true);

            TokenStream t = a.reusableTokenStream(
                                    "", new StringReader("Hr. A. Binde-Streg"));

            assertTokens(t, "hr", "a", "binde", "streg");

            a = new SummaAnalyzer(null, true, null, false, false);
            t = a.reusableTokenStream("",
                                      new StringReader("Hr. A. Binde-Streg"));

            assertTokens(t, "Hr", "A", "Binde", "Streg");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     *Test trans literations.
     */
    public void testTransliterations() {
        try {
            a = new SummaAnalyzer(null, true, null, true, true);
            TokenStream t = a.reusableTokenStream(
                                       "", new StringReader("Über Ål!"));

            assertTokens(t, "yber", "aal");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }
}
