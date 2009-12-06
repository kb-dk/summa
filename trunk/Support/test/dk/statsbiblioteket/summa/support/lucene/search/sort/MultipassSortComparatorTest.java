package dk.statsbiblioteket.summa.support.lucene.search.sort;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.List;
import java.util.Arrays;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import dk.statsbiblioteket.util.Strings;

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

    public static void testBasicSort() throws Exception {
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

    public static void testBasicSortNull() throws Exception {
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

    public static void testBasicLocalStaticSortComparator() throws Exception {
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

    public static void testTrickyLocalStaticSortComparator() throws Exception {
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

    public static void testBasicMultipass() throws Exception {
        List<String> actual = SortHelper.getSortResult(
                "all:all",
                SortHelper.BASIC_TERMS, new SortHelper.SortFactory() {
            @Override
            Sort getSort(IndexReader reader) throws IOException {
                return new Sort(new SortField(
                        SortHelper.SORT_FIELD,
                        new MultipassSortComparator("da", 10)));
            }
        });
        String[] expected = Arrays.copyOf(
                SortHelper.BASIC_TERMS, SortHelper.BASIC_TERMS.length);
        Arrays.sort(expected);
        assertEquals("The returned order should be correct",
                     Strings.join(expected, ", "), Strings.join(actual, ", "));
    }
}
