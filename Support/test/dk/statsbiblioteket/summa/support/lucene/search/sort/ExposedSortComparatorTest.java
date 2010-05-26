package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * ExposedSortComparator Tester.
 *
 * @author <Authors name>
 * @since <pre>05/25/2010</pre>
 * @version 1.0
 */
public class ExposedSortComparatorTest extends TestCase {
    public ExposedSortComparatorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ExposedSortComparatorTest.class);
    }

    public void testExposedSortBasic() throws Exception {
        List<String> actual = SortHelper.getSortResult(
            "all:all",
            SortHelper.BASIC_TERMS, new SortHelper.SortFactory() {
                ExposedSortComparator exposed = new ExposedSortComparator("da");
                @Override
                Sort getSort(IndexReader reader) throws IOException {
                    exposed.indexChanged(reader);
                    return new Sort(new SortField(
                        SortHelper.SORT_FIELD, exposed));
                }

                @Override
                void indexChanged(IndexReader reader) throws IOException {
                    exposed.indexChanged(reader);
                }
            });
        String[] expected = Arrays.copyOf(
            SortHelper.BASIC_TERMS, SortHelper.BASIC_TERMS.length);
        Arrays.sort(expected);
        assertEquals("The returned order should be correct",
                     Strings.join(expected, ", "), Strings.join(actual, ", "));

    }
}
