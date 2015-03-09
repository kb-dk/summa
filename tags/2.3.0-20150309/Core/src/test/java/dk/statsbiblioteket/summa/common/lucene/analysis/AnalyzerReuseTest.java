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

public class AnalyzerReuseTest extends AnalyzerTestCase {

    public void testSummaAnalyzer() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, true), "FOO BAR", "foo", "bar");
    }

    public void testSummaKeywordAnalyzer() throws Exception {
        assertAnalyzer(new SummaKeywordAnalyzer(), "FOO BAR", "foo bar");
    }

    public void testSummaStandardAnalyzer() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), "FOO BAR", "foo", "bar");
    }

    public void testSummaFreetextAnalyzer() throws Exception {
        assertAnalyzer(new FreeTextAnalyzer(), "FOO BAR", "foo", "bar");
    }

    public void testSummaFieldSeparatingAnalyzer() throws Exception {
        assertAnalyzer(new SummaFieldSeparatingAnalyzer(new SummaKeywordAnalyzer()), "FOO BAR", "foo bar");
    }

    public void testSummaLowercaseAnalyzer() throws Exception {
        assertAnalyzer(new SummaLowercaseAnalyzer(), "FOO BAR", "foo", "bar");
    }

    public void testSummaNumberAnalyzer() throws Exception {
        assertAnalyzer(new SummaNumberAnalyzer(), "123 45-67", "123 4567");
    }

    public void testSummaSymbolRemovingAnalyzer() throws Exception {
        assertAnalyzer(new SummaSymbolRemovingAnalyzer(), "foo bar", "foo bar");
    }
}
