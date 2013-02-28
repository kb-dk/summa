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

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.util.AttributeImpl;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AnalyzerTestCase extends TestCase {

    public AnalyzerTestCase() {
    }

    public AnalyzerTestCase(String name) {
        super(name);
    }

    public void testDummy() {
        assertTrue(true); // To avoid warnings (this TestCase is a helper and has no standalone functionality)
    }

    protected void assertTokens(String message, Analyzer analyzer, String input, String... tokens) throws Exception{
        TokenStream tokenStream = analyzer.tokenStream("dummy", new StringReader(input));
        assertTokens(message, tokenStream, tokens);
    }

    /**
     * Check tokens.
     * @param tokenizer The token stream.
     * @param tokens The token.
     * @throws Exception If error occurs.
     */
    protected void assertTokens(TokenStream tokenizer, String... tokens) throws Exception {
        assertTokens(null, tokenizer, tokens);
    }
    protected void assertTokens(String message, TokenStream tokenizer, String... tokens) throws Exception {
        tokenizer.reset();
        CharTermAttribute term = tokenizer.getAttribute(CharTermAttribute.class);
        int count = 0;
        String prefix = message == null ? "" : message + ". ";

        while (tokenizer.incrementToken()) {
            if (count >= tokens.length) {
                fail(prefix + "Too many tokens from tokenizer, found " + (count + 1)
                        + ". Expected " + tokens.length + ".");
            }

            assertEquals(prefix + "Mismatch in token number " + (count + 1),
                         tokens[count], term.toString());
            count++;
        }

        assertEquals(prefix + "To few tokens from tokenizer, found " + count + ". Expected " + tokens.length + ".",
                     tokens.length, count);
    }

    protected void assertTerms(List<String> expected, TokenStream actual) throws IOException {
        actual.reset();
        List<String> terms = getTerms(actual);
        String es = Strings.join(expected, "', '");
        String as = Strings.join(terms, "', '");
        assertEquals("There should be the same number of terms\nExpected: '" + es + "'\nActual:   '" + as + "'\n",
                     expected.size(), terms.size());
        for (int i = 0 ; i < expected.size() ; i++) {
            assertEquals("The Strings ap position " + i + " should be equal\nExpected: '" + es + "'\nActual:   '"
                         + as + "'\n",
                         expected.get(i), terms.get(i));
        }
    }

    @SuppressWarnings("ObjectToString")
    protected List<String> getTerms(TokenStream tokens) throws IOException {
        List<String> result = new ArrayList<String>();
        while (tokens.incrementToken()) {
            Iterator<AttributeImpl> ai = tokens.getAttributeImplsIterator();
            while (ai.hasNext()) {
                AttributeImpl a = ai.next();
                if (a instanceof CharTermAttributeImpl) {
                    result.add(a.toString());
                }
            }
        }
        return result;
    }

    protected void disabledAssertAnalyzer(SummaAnalyzer analyzer, String input, String... expected) throws Exception {
        { // run #1
            Analyzer.TokenStreamComponents components = analyzer.createComponents("sample", new StringReader(input));
            assertTokens(components.getTokenStream(), expected);
        }
        { // run #2
            Analyzer.TokenStreamComponents components = analyzer.createComponents("sample", new StringReader(input));
            assertTokens(components.getTokenStream(), expected);
        }
    }
    protected void assertAnalyzer(Analyzer analyzer, String input, String... expected) throws Exception {
        { // run #1
            assertTokens("run 1", analyzer, input, expected);
        }
        { // run #2
            assertTokens("run 2", analyzer, input, expected);
        }
    }
}
