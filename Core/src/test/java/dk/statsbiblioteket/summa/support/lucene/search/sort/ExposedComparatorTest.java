package dk.statsbiblioteket.summa.support.lucene.search.sort;

import com.ibm.icu.text.Collator;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.exposed.compare.NamedCollatorComparator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * ExposedSortComparator Tester.
 *
 * @author <Authors name>
 * @since <pre>05/25/2010</pre>
 * @version 1.0
 */
public class ExposedComparatorTest extends TestCase {
    private static Log log = LogFactory.getLog(ExposedComparatorTest.class);
    public ExposedComparatorTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ExposedComparatorTest.class);
    }

    public void testExposedSortBasic() throws Exception {
        List<String> actual = SortHelper.getSortResult(
            "all:all",
            SortHelper.BASIC_TERMS, new SortHelper.SortFactory() {
                ExposedComparator exposed = new ExposedComparator("da");
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

    public void testExposedSortEdgeCases() throws Exception {
        final String[] TERMS = SortHelper.TRICKY_TERMS;

        List<String> actual = SortHelper.getSortResult(
            "all:all",
            TERMS, new SortHelper.SortFactory() {
                ExposedComparator exposed = new ExposedComparator("da");
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
        String[] expected = Arrays.copyOf(TERMS, TERMS.length);
        final Collator standardCollator =
          new NamedCollatorComparator(new Locale("da")).getCollator();
        Arrays.sort(expected, new Comparator<String>(){
            @Override
            public int compare(String o1, String o2) {
                if (o1 == null) {
                    return o2 == null ? 0 : -1;
                } else if (o2 == null) {
                    return 1;
                }
                return standardCollator.compare(o1, o2);
            }
        });
        assertEquals("The returned order should be correct",
                     Strings.join(expected, ", "), Strings.join(actual, ", "));

    }
}
