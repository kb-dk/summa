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

/**
 * Unit tests for {@link SummaAnalyzer}.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", "CallToPrintStackTrace"})
public class SummaAnalyzerTest extends AnalyzerTestCase {
    /** Summa analyzer. */
    private SummaAnalyzer a;




    /**
     * Test char to char array.
     */
    public void testCharToCharArray() {
        try {
            ReplaceFactory factory = new ReplaceFactory(
                    RuleParser.parse(RuleParser.sanitize("", true, Rules.ALL_TRANSLITERATIONS)));
            ReplaceReader replacer = factory.getReplacer();
            assertNotNull(replacer);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown");
        }
    }

    /**
     * Test foo case fold.
     */
    public void testFooCaseFold() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, true), "Foo",
                       "foo");
    }

    /**
     * Test lower case only.
     */
    public void testLowerCaseOnly() throws Exception {
        assertAnalyzer(new SummaAnalyzer("", false, "", false, true), "Foo-%4g 123/erW'd;_foo",
                       "foo-%4g", "123/erw'd;_foo");
    }

    public void testSpecialCharHandling() throws Exception {
        assertAnalyzer(new SummaAnalyzer("", true, "", true, true), "Foo-%4g 123/>erW'd;_=foo",
                       "foo", "%4g", "123", "erw", "d", "foo");
    }

    /**
     * Test foo bar case fold.
     */
    public void testFooBarCaseFold() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, true), "Foo baR", "foo", "bar");
    }

    /**
     * Test foo bar no case fold.
     */
    public void testFooBarNoCaseFold() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, false), "Foo baR", "Foo", "baR");
    }

    /**
     * Test white space 1.
     */
    public void testWhiteSpace1() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, true), " white ", "white");
    }

    /**
     * Test long.
     */
    public void testLong() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, true),
                       "Captain Planet once again saves the world. This time by getting rid of dandruff.",
                       "captain", "planet", "once", "again", "saves", "the", "world", "this", "time", "by", "getting",
                       "rid", "of", "dandruff");
    }

    /**
     * Test white space 2.
     */
    public void testWhiteSpace2() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, true), "barry   white ", "barry", "white");
    }

    /**
     * Test white space 3.
     */
    public void testWhiteSpace3() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, false), " I  P Jacobsen    ", "I", "P", "Jacobsen");
    }

    /**
     * Test punktucation.
     */
    public void testPunctuation() throws Exception {
        assertAnalyzer(new SummaAnalyzer("", true, null, true, true), "barry   white.", "barry", "white");
    }

    /**
     * Test token replacements.
     */
    public void testTokenReplacements() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, true), ".Net vs C* Algebra?",
                       "dotnet", "vs", "cstaralgebra");
    }

    /**
     * Test reuse.
     */
    public void testReuse() throws Exception {
        SummaAnalyzer a = new SummaAnalyzer(null, true, null, true, true);
        assertAnalyzer(a, "foo", "foo");
        assertAnalyzer(a, "Fast talking flip-flopper", "fast", "talking", "flip", "flopper");
    }

    public void testReuse2() throws Exception {
        SummaAnalyzer a = new SummaAnalyzer(null, true, null, true, true);
        { // run #1
            assertAnalyzer(a, "FOO BAR", "foo", "bar");
        }
        { // run #2
            assertAnalyzer(a, "FOO BAR", "foo", "bar");
        }
    }


    /**
     * Test Dashes.
     */
    public void testDashes() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, true), "Hr. A. Binde-Streg",
                       "hr", "a", "binde", "streg");
    }

    /**
     *Test trans literations.
     */
    public void testTransliterations() throws Exception {
        assertAnalyzer(new SummaAnalyzer(null, true, null, true, true), "Über Ål!",
                       "yber", "aal");
    }
}
