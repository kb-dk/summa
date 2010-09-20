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
import junit.framework.TestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.*;

import java.io.StringReader;

/**
 * Unit tests for {@link SummaAnalyzer}
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class SummaAnalyzerTest extends TestCase {

    SummaAnalyzer a;

    static void assertTokens(TokenStream tokenizer, String... tokens)
                                                               throws Exception{
        TermAttribute term = tokenizer.getAttribute(TermAttribute.class);
        int count = 0;

        while (tokenizer.incrementToken()) {
            if (count >= tokens.length) {
                fail("Too many tokens from tokenizer, found " + (count+1)
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

    static TokenStream getStream(String text) {
        return new LowerCaseFilter(
                new WhitespaceTokenizer(
                        new StringReader(text)));
    }

    public void testCharToCharArray() throws Exception {
        ReplaceFactory factory = new ReplaceFactory(
                RuleParser.parse(RuleParser.sanitize(
                        "", true, Rules.ALL_TRANSLITERATIONS)));
        ReplaceReader replacer = factory.getReplacer();
        System.out.println("Got Replacer " + replacer);
    }

    public void testFooCaseFold() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream("", new StringReader("Foo"));

        assertTokens(t, "foo");
    }

    public void testLowerCaseOnly() throws Exception {
        a = new SummaAnalyzer("", false, "", false, true);
        TokenStream t = a.reusableTokenStream("", new StringReader(
            "Foo-%4g 123/erW'd;_foo"));

        assertTokens(t, "foo-%4g", "123/erw'd;_foo");
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

    public void testLong() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream("", new StringReader(
                "Captain Planet once again saves the world. This time by "
                + "getting rid of dandruff."));

        assertTokens(t, "captain", "planet", "once", "again", "saves", "the", 
                     "world", "this", "time", "by", "getting", "rid", "of",
                     "dandruff");
    }



    public void testWhiteSpace2() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream("", new StringReader("barry   white "));

        assertTokens(t, "barry", "white");
    }

    public void testWhiteSpace3() throws Exception {
        // This test is case sensitive, just try that combo as well
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
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream(
                                   "", new StringReader("Hr. A. Binde-Streg"));

        assertTokens(t, "hr", "a", "binde", "streg");

        a = new SummaAnalyzer(null, true, null, false, false);
        t = a.reusableTokenStream("", new StringReader("Hr. A. Binde-Streg"));

        assertTokens(t, "Hr", "A", "Binde", "Streg");
    }

    public void testTransliterations() throws Exception {
        a = new SummaAnalyzer(null, true, null, true, true);
        TokenStream t = a.reusableTokenStream(
                                   "", new StringReader("Über Ål!"));

        assertTokens(t, "yber", "aal");

        
    }
}

