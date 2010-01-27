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
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.summa.common.util.StringTracker;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.CachedCollator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.IndexSearcher;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.text.Collator;

/**
 * Tests multiple sort-implementations for Lucene for correctness.
 */
public class MultipassSortComparatorTest extends TestCase {
    public MultipassSortComparatorTest(String name) {
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
        return new TestSuite(MultipassSortComparatorTest.class);
    }

    public void testBasicSort() throws Exception {
        List<String> actual = SortHelper.getSortResult(
                "all:all",
                SortHelper.BASIC_TERMS, new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) {
                return new Sort(SortHelper.SORT_FIELD);
            }
        });
        String[] expected = Arrays.copyOf(
                SortHelper.BASIC_TERMS, SortHelper.BASIC_TERMS.length);
        Arrays.sort(expected);
        assertEquals("The returned order should be correct",
                     Strings.join(expected, ", "), Strings.join(actual, ", "));
    }

    public void testBasicSortNull() throws Exception {
        List<String> actual = SortHelper.getSortResult(
                "all:all",
                SortHelper.TRICKY_TERMS, new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) {
                return new Sort(SortHelper.SORT_FIELD);
            }
        });
        String[] expected = Arrays.copyOf(
                SortHelper.TRICKY_TERMS, SortHelper.TRICKY_TERMS.length);
        SortHelper.nullSort(expected);
        assertEquals("The returned order should be correct",
                     Strings.join(expected, ", "), Strings.join(actual, ", "));
    }

    public void testBasicLocalStaticSortComparator() throws Exception {
        List<String> actual = SortHelper.getSortResult(
                "all:all",
                SortHelper.BASIC_TERMS, new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) throws IOException {
                return new Sort(new SortField(
                        SortHelper.SORT_FIELD,
                        new LocalStaticSortComparator("da")));
            }
        });
        String[] expected = Arrays.copyOf(
                SortHelper.BASIC_TERMS, SortHelper.BASIC_TERMS.length);
        Arrays.sort(expected);
        assertEquals("The returned order should be correct",
                     Strings.join(expected, ", "), Strings.join(actual, ", "));
    }

    public void testTrickyLocalStaticSortComparator() throws Exception {
        List<String> actual = SortHelper.getSortResult(
                "all:all",
                SortHelper.TRICKY_TERMS, new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) throws IOException {
                return new Sort(new SortField(
                        SortHelper.SORT_FIELD,
                        new LocalStaticSortComparator("da")));
            }
        });
        String[] EXPECTED = {
                "a aa", "a be", "abe", "bar", "foo", "moo moo", "z",
                "Ægir", "ægir", "Ødis", null, null, null};

        assertEquals("The returned order should be correct",
                     Strings.join(EXPECTED, ", "), Strings.join(actual, ", "));
    }

    public void testMultipassSingleElementOnHeap() throws Exception {
        testMultipassSpecificHeap(10); // 1 char
    }

    public void testMultipass2ElementsOnHeap() throws Exception {
        testMultipassSpecificHeap(StringTracker.
                SINGLE_ENTRY_OVERHEAD * 2 + 10); // ~2 chars
    }

    public void testMultipassAllElementsOnHeap() throws Exception {
        testMultipassSpecificHeap(Integer.MAX_VALUE);
    }

    public void testMultipassSpecificHeap(final int heap)
                                                              throws Exception {
        List<String> actual = SortHelper.getSortResult(
                "all:all",
                SortHelper.BASIC_TERMS, new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) throws IOException {
                return new Sort(new SortField(
                        SortHelper.SORT_FIELD,
                        new MultipassSortComparator("da", heap)));
            }
        });
        String[] expected = Arrays.copyOf(
                SortHelper.BASIC_TERMS, SortHelper.BASIC_TERMS.length);
        Arrays.sort(expected);
        assertEquals("The returned order should be correct with heap " + heap,
                     Strings.join(expected, ", "), Strings.join(actual, ", "));
    }

    public void testCompareMemoryConsumption() throws Exception {
        int TERM_COUNT = 10000;
        int TERM_MAX_LENGTH = 20;
        int RUNS = 3;
        final int SORT_BUFFER = 2 * 1024 * 1024;

        File index = SortHelper.createIndex(
                makeTerms(TERM_COUNT, TERM_MAX_LENGTH));
        System.gc();
        System.out.println(String.format(
                "Measuring memory usage for different sorters. Term count = %d,"
                + " max term length = %d, sort buffer (for multipass) = %d KB. "
                + "Initial memory usage: %d KB",
                TERM_COUNT, TERM_MAX_LENGTH, SORT_BUFFER / 1024,
                (Runtime.getRuntime().totalMemory()
                 - Runtime.getRuntime().freeMemory()) / 1024));
        for (int run = 0 ; run < RUNS ; run++) {
            long lucene = SortHelper.performSortedSearch(
                    index, "all:all", 10, getLuceneFactory(
                    SortHelper.SORT_FIELD));
            long multipass = SortHelper.performSortedSearch(
                    index, "all:all", 10, getMultipassFactory(
                            SortHelper.SORT_FIELD, SORT_BUFFER));
            long localstatic =SortHelper.performSortedSearch(
                    index, "all:all", 10, getStaticFactory(
                            SortHelper.SORT_FIELD));
            System.out.println(String.format(
                  "Run %d -> lucene = %d KB, multipass = %d KB, static = %d KB",
                  run, lucene / 1024, multipass / 1024, localstatic / 1024));
        }
    }

    // Manual test activation with tweaked Xmx
    public void testCreateIndex() throws Exception {
        int TERM_COUNT = 50000;
        int TERM_MAX_LENGTH = 20;
        Profiler profiler = new Profiler();
        SortHelper.createIndex(makeTerms(TERM_COUNT, TERM_MAX_LENGTH));
        System.out.println("Build index with " + TERM_COUNT + " documents in "
                           + profiler.getSpendTime());
    }

    public void testCollator() throws Exception {
        MultipassSortComparator sc = new MultipassSortComparator("da", 1000);
        assertTrue("Comparing a with b should work",
                   sc.compare("a", "b") < 0);
        assertTrue("Comparing bkNTeMbjØfUs with mB2 should work",
                   sc.compare("bkNTeMbjØfUs", "mB2") < 0);
        assertTrue("Comparing mB2 with mÆju12Q should work",
                   sc.compare("mB2", "mÆju12Q") < 0);
        assertTrue("Comparing m9Yi)o3Kq with mB2 should work",
                   sc.compare("m9Yi)o3Kq", "mB2") < 0);
    }

    // Manual test activation with tweaked Xmx
    public void testSortLucene() throws Exception {
        File index = new File(System.getProperty("java.io.tmpdir"));
        System.out.println("Performing standard Lucene sorted search");
        Profiler profiler = new Profiler();
        long lucene = SortHelper.performSortedSearch(
                index, "all:all", 10, getLuceneFactory(
                SortHelper.SORT_FIELD));
        System.out.println(
                "Lucene sort search performed, using " + lucene / 1024
                + " KB in " + profiler.getSpendTime());
    }

    // Manual test activation with tweaked Xmx
    public void testSortMultipass() throws Exception {
        File index = new File(System.getProperty("java.io.tmpdir"));
        System.out.println("Performing initial Multipass sorted search");
        Profiler profiler = new Profiler();
        long lucene = SortHelper.performSortedSearch(
                index, "all:all", 10, getMultipassFactory(
                SortHelper.SORT_FIELD,  1000 * 1024));
        System.out.println(
                "Multipass sort search performed, using " + lucene / 1024
                + " KB in " + profiler.getSpendTime());
    }

    public void testMultipassLoops() throws Exception {
        int TERM_COUNT = 45;
        int TERM_MAX_LENGTH = 20;
        int BUFFER = 800;
        String[] terms = makeTerms(TERM_COUNT, TERM_MAX_LENGTH);

        Profiler profiler = new Profiler();
        SortHelper.createIndex(terms);
        System.out.println("Build index with " + TERM_COUNT + " documents in "
                           + profiler.getSpendTime());

        File index = new File(System.getProperty("java.io.tmpdir"));
        profiler = new Profiler();
        SortHelper.SortFactory sf = getMultipassFactory(
                SortHelper.SORT_FIELD,  BUFFER);
        long lucene = SortHelper.performSortedSearch(index, "all:all", 10, sf);

        System.out.println(
                "Multipass sort search performed, using " + lucene / 1024
                + " KB in " + profiler.getSpendTime());
                                /*
        IndexSearcher searcher = new IndexSearcher(index.toString());
        Sort multipassSort = sf.getSort(searcher.getIndexReader());
        MultipassSortComparator multipass =
                (MultipassSortComparator)multipassSort.getSort()[0].
                        getFactory().newComparator(
                        searcher.getIndexReader(), SortHelper.SORT_FIELD);
        List<MultipassSortComparator.OrderedString> multiSorted =
               new ArrayList<MultipassSortComparator.OrderedString>(TERM_COUNT);
        int counter = 0;
        for (String term: terms) {
            multiSorted.add(new MultipassSortComparator.OrderedString(
                    term, counter++));
        }
        Collections.sort(multipassSort, multipass.))
        searcher.close();         */
    }

    public void testDualRepeat() throws Exception {
        testSortLuceneRepeat();
        testSortMultipassRepeat();
    }

    public void testSortLuceneRepeat() throws Exception {
        int REPEATS = 5;

        File index = new File(System.getProperty("java.io.tmpdir"));
        Profiler profiler = new Profiler();
        long lucene = SortHelper.performSortedSearch(
                index, "all:all", 10, getLuceneFactory(
                SortHelper.SORT_FIELD));
        System.out.println(
                "Lucene initial sort search performed, using " + lucene / 1024
                + " KB in " + profiler.getSpendTime());

        long searchTime = SortHelper.timeSortedSearch(
                index, "all:all", 10, getLuceneFactory(
                SortHelper.SORT_FIELD), REPEATS);
        System.out.println(
                "Lucene sort search #" + REPEATS + " took "
                + searchTime + " ms");
    }

    public void testSortMultipassRepeat() throws Exception {
        int REPEATS = 5;

        File index = new File(System.getProperty("java.io.tmpdir"));

        Profiler profiler = new Profiler();
        long multipass = SortHelper.performSortedSearch(
                index, "all:all", 10, getMultipassFactory(
                SortHelper.SORT_FIELD, 20 * 1024 * 1024));
        System.out.println(
                "Multipass initial sort search performed, using "
                + multipass / 1024 + " KB in " + profiler.getSpendTime());

        long searchTime = SortHelper.timeSortedSearch(
                index, "all:all", 10, getMultipassFactory(
                SortHelper.SORT_FIELD, 100 * 1024 * 1024), REPEATS);
        System.out.println(
                "Multipass sort search #" + REPEATS + " took "
                + searchTime + " ms");
    }



    // Manual test activation with tweaked Xmx
    public void testSortstatic() throws Exception {
        File index = new File(System.getProperty("java.io.tmpdir"));
        System.out.println("Performing initial Staticlocal sorted search");
        Profiler profiler = new Profiler();
        long lucene = SortHelper.performSortedSearch(
                index, "all:all", 10, getStaticFactory(SortHelper.SORT_FIELD));
        System.out.println(
                "Staticlocal sort search performed, using " + lucene / 1024
                + " KB in " + profiler.getSpendTime());
    }

    private static SortHelper.SortFactory getLuceneFactory(
                                                           final String field) {
        return new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) throws IOException {
                return new Sort(new SortField(field, new Locale("da")));
            }
        };
    }

    private static SortHelper.SortFactory getStaticFactory(final String field) {
        return new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) throws IOException {
                return new Sort(new SortField(
                        field,
                        new LocalStaticSortComparator("da")));
            }
        };
    }

    private static SortHelper.SortFactory getMultipassFactory(
            final String field, final int buffer) {
        return new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) throws IOException {
                return new Sort(new SortField(
                        field,
                        new MultipassSortComparator("da", buffer)));
            }
        };
    }

    private String[] makeTerms(int count, int maxLength) {
        final String BASE = "abcdefghijklmnopqrstuvwxyzæøå 1234567890 "
                            + "ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÅ !%/()";
        Random random = new Random(87);
        String[] result = new String[count];
        StringBuilder sb = new StringBuilder(maxLength);
        for (int i = 0 ; i < count ; i++) {
            sb.setLength(0);
            int length = random.nextInt(maxLength + 1);
            for (int j = 0 ; j < length ; j++) {
                sb.append(BASE.charAt(random.nextInt(BASE.length())));
            }
            result[i] = sb.toString();
        }
        return result;
    }
}

