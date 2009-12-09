package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.summa.common.util.StringTracker;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import java.io.IOException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Locale;

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

    public void testMemoryConsumption() throws Exception {
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
        int TERM_COUNT = 2000;
        int TERM_MAX_LENGTH = 20;
        SortHelper.createIndex(makeTerms(TERM_COUNT, TERM_MAX_LENGTH));
    }

    // Manual test activation with tweaked Xmx
    public void testSortLucene() throws Exception {
        File index = new File(System.getProperty("java.io.tmpdir"));
        long lucene = SortHelper.performSortedSearch(
                index, "all:all", 10, getLuceneFactory(
                SortHelper.SORT_FIELD));
        System.out.println("Lucene mem: " + lucene / 1024 + " KB");
    }

    // Manual test activation with tweaked Xmx
    public void testSortMultipass() throws Exception {
        File index = new File(System.getProperty("java.io.tmpdir"));
        long lucene = SortHelper.performSortedSearch(
                index, "all:all", 10, getMultipassFactory(
                SortHelper.SORT_FIELD, 1024 * 1024));
        System.out.println("Multipass mem: " + lucene / 1024 + " KB");
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
