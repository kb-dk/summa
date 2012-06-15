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

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

import java.io.StringReader;

/**
 * Test cases for {@link SummaKeywordAnalyzer}
 */
public class SummaKeywordAnalyzerTest extends AnalyzerTestCase {

    SummaKeywordAnalyzer a;
    TokenStream t;

    static TokenStream getStream(String text) {
        return new LowerCaseFilter(org.apache.lucene.util.Version.LUCENE_30,
                new WhitespaceTokenizer(org.apache.lucene.util.Version.LUCENE_30, new StringReader(text)));
    }

    
    
    // This is actually what the keyword rule does
    public void testUnderscore() throws Exception {
        a = new SummaKeywordAnalyzer();
        t = a.tokenStream("testField", new StringReader("bar_foo"));
        assertTokens(t, "bar foo");

        t = a.tokenStream("testField", new StringReader("foo"));
        assertTokens(t, "foo");
    }

    public void testFooBar() throws Exception {
        a = new SummaKeywordAnalyzer();
        t = a.tokenStream("testField", new StringReader("foo bar"));
        assertTokens(t, "foo bar");

        t = a.tokenStream("testField", new StringReader("foo bar"));
        assertTokens(t, "foo bar");
    }

    public void testFooBarExtraSpaces() throws Exception {
        a = new SummaKeywordAnalyzer();
        t = a.tokenStream("testField", new StringReader(" foo  bar   "));
        assertTokens(t, "foo bar");

        t = a.tokenStream("testField", new StringReader(" foo  bar   "));
        assertTokens(t, "foo bar");
    }

    public void testPunctuation() throws Exception {
        assertAnalyzer(new SummaKeywordAnalyzer(), ".foo  bar.", "foo bar");
/*        a = new SummaKeywordAnalyzer();
        t = a.tokenStream("testField", new StringReader(".foo  bar."));
        assertTokens(t, "foo bar");
  */
        assertAnalyzer(new SummaKeywordAnalyzer(), "moo.bar", "moo bar");
//        t = a.tokenStream("testField", new StringReader("foo.bar   "));
//        assertTokens(t, "foo bar");
    }

}

