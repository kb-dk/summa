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

/**
 *
 */
public class SummaStandardAnalyzerTest extends AnalyzerTestCase {
    /**
     * The summa analyzer.
     */
    private SummaAnalyzer a;

    public final void testFooCaseFold() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), "Foo", "foo");
    }

    public final void testFooBarCaseFold() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), "Foo baR", "foo", "bar");
    }

    public final void testWhiteSpace1() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), " white ", "white");
    }

    public final void testWhiteSpace2() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), "barry   white ", "barry", "white");
    }

    public final void testWhiteSpace3() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), " I  P Jacobsen    ", "i", "p", "jacobsen");
    }

    public final void testPunctuation() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), "barry   white.", "barry", "white");
    }

    public final void testPunctuation2() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), ".foo bar.", "foo", "bar");
    }

    public final void testTokenReplacements() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), ".Net vs C* Algebra?", "dotnet", "vs", "cstaralgebra");
    }

    public final void testDashes() throws Exception {
        assertAnalyzer(new SummaStandardAnalyzer(), "Hr. A. Binde-Streg", "hr", "a", "binde", "streg");
        assertAnalyzer(new SummaStandardAnalyzer(), "Jan-Hendrik S. Hofmeyr", "jan", "hendrik", "s", "hofmeyr");
    }
}
