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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.text.Collator;
import java.util.*;

public class CollatorFactoryTest extends TestCase {
    public CollatorFactoryTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    public static Test suite() {
        return new TestSuite(CollatorFactoryTest.class);
    }

    public void testModifiedCollator() throws Exception {
        List<String> EXPECTED = Arrays.asList("ko", "kost er bedst", "koste");
        List<String> input = Arrays.asList("koste", "kost er bedst", "ko");

        Collator daCollator = CollatorFactory.createCollator(new Locale("da"));
        testCompare("Spaces hould be sorted before anything else by rule "
                    + "collator", daCollator, EXPECTED, input);
    }

    public void testUnModifiedCollator() throws Exception {
        List<String> EXPECTED = Arrays.asList("ko", "koste", "kost er bedst");
        List<String> input = Arrays.asList("koste", "kost er bedst", "ko");

        Collator daCollator = Collator.getInstance(new Locale("da"));
        testCompare("Spaces hould be sorted after everything else by standard "
                    + "collator", daCollator, EXPECTED, input);
    }

/*    public void testUnModifiedCollatorAA() throws Exception {
        List<String> EXPECTED = Arrays.asList("aa", "a a");
        List<String> input = Arrays.asList("a a", "aa");

        Collator daCollator = Collator.getInstance(new Locale("da"));
        testCompare("Spaces hould be sorted after everything else by standard "
                    + "collator", daCollator, EXPECTED, input);
    }
  */
    // TODO: check why "a a" vs. "aa" does not behave as below
    
    public void testUnModifiedCollatorAB() throws Exception {
        List<String> EXPECTED = Arrays.asList("ab", "a b");
        List<String> input = Arrays.asList("ab", "a b");

        Collator daCollator = Collator.getInstance(new Locale("da"));
        testCompare("Spaces hould be sorted after everything else by standard "
                    + "collator", daCollator, EXPECTED, input);
    }


    private void testCompare(String message, Comparator comparator,
                            List<String> expected, List<String> actual)
                                                              throws Exception {
        List<String> sorted = new ArrayList<String>(actual);
        //noinspection unchecked
        Collections.sort(sorted, comparator);
        assertEquals(message,
                     Strings.join(expected, ", "), Strings.join(sorted, ", "));
    }

    public void testSpaceSort() throws Exception {
        assertTrue("Standard compareTo should sort space first",
                   "a b".compareTo("aa") < 0);

        Collator sansSpaceStandard = Collator.getInstance(new Locale("da"));
        assertTrue("Standard Collator should sort space last",
                   sansSpaceStandard.compare("a b", "ab") > 0);
        testCompare("Standard Collator should sort space last", 
                    sansSpaceStandard, Arrays.asList("ab", "a b"),
                    Arrays.asList("a b", "ab"));

        Collator sansSpace = new CachedCollator(new Locale("da"), "");
        assertTrue("None-space-modified Collator should sort space last",
                   sansSpace.compare("a b", "ab") > 0);
    }
}

