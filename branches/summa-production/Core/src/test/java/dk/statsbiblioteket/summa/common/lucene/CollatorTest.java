/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.lucene;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Random;

/**
 * Experiments with Collators, especially with regard to speed.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CollatorTest extends TestCase {
    private static Log log = LogFactory.getLog(CollatorTest.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(CollatorTest.class);
    }

    public static final String ALPHABET =
        "abcdefghijklmnopqrstuvwxyzæøåABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ1"
        + "234567890ëöäïüËÜÏÖÄ@£$%&{[]}+?ôêîûâÂÊŶÛÔÎ          ";

    public void testCollatorSpeed() {
        int[] TERMS = new int[]{1000, 10000, 100000};
        int MIN = 5;
        int MAX = 30;
        Locale DA = new Locale("da");

        Collator javaCollator = Collator.getInstance(DA);
        com.ibm.icu.text.Collator icuCollator =
            com.ibm.icu.text.Collator.getInstance(DA);
        for (int termC: TERMS) {
            testRaw("Java", wrap(javaCollator), termC, MIN, MAX);
            testRaw("ICU ", wrapO(icuCollator), termC, MIN, MAX);
        }
    }

    public void testKeys() {
        Locale DA = new Locale("da");
        com.ibm.icu.text.Collator javaC =
            com.ibm.icu.text.Collator.getInstance(DA);
        com.ibm.icu.text.Collator icuC =
            com.ibm.icu.text.Collator.getInstance(DA);
    }

    private Comparator<String> wrapO(
        final com.ibm.icu.text.Collator icuCollator) {
        return new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return icuCollator.compare(o1, o2);
            }
        };
    }

    private Comparator<String> wrap(final Collator collator) {
        return new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return collator.compare(o1, o2);
            }
        };
    }

    public void testRaw(String id, Comparator<String> comparator,
                        int termCount, int minLength, int maxLength) {
        final int WARMUP = 1;
        final int RUNS = 3;
        final String[] terms = generateTerms(termCount, minLength, maxLength);

        for (int i = 0 ; i < WARMUP ; i++) {
            String[] sort = copy(terms);
            Arrays.sort(sort, comparator);
        }

        long bestTime = Long.MAX_VALUE;
        for (int i = 0 ; i < RUNS ; i++) {
            String[] sort = copy(terms);
            long sortTime = -System.currentTimeMillis();
            Arrays.sort(sort, comparator);
            sortTime += System.currentTimeMillis();
            bestTime = Math.min(bestTime, sortTime);
        }
        System.out.println(
            id + ": " + termCount + " terms sorted fastest in "
            + bestTime + " ms @ "
            + (bestTime == 0 ? "N/A" : termCount/bestTime) + " terms/ms");
    }

    private String[] copy(String[] terms) {
        String[] result = new String[terms.length];
        System.arraycopy(terms, 0, result, 0, terms.length);
        return result;
    }

    private String[] generateTerms(
        int termCount, int minLength, int maxLength) {
        Random random = new Random();
        String[] result = new String[termCount];
        for (int i = 0 ; i < termCount ; i++) {
            final char[] term =
                new char[random.nextInt(maxLength-minLength)+minLength];
            for (int c = 0 ; c < term.length ; c++) {
                term[c] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
            }
            result[i] = new String(term);
        }
        return result;
    }
}

