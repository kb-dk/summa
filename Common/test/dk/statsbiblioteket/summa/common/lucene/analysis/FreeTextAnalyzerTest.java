package dk.statsbiblioteket.summa.common.lucene.analysis;

import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.util.AttributeImpl;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FreeTextAnalyzerTest extends TestCase {
    public FreeTextAnalyzerTest(String name) {
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
        return new TestSuite(FreeTextAnalyzerTest.class);
    }

    /*public void testBasicTokenization() throws IOException {
        String INPUT = "foo bar";
        List<String> EXPECTED = Arrays.asList("foo", "bar");
        Analyzer fa = new WhitespaceAnalyzer();
        TokenStream stream = fa.tokenStream("dummy", new StringReader(INPUT));
        assertTerms(EXPECTED, stream);
    } */

    public void testFreeText() throws IOException {
        String[][] TESTS = new String[][]{
            {"foo bar", "foo bar"},
            {"foo bar", "foo-bar"},
            {"foo", "foo"},
            {"areni", "árënì"},
            {"hæns", "häns"},
            {"føo", "föó"},
            {"bog", "bog"},
            {"sundhed", "sundhed"},
            {"foo & bar", "foo og bar"},
            {"foo & bar", "foo und bar"},
            {"foo & bar", "foo and bar"}
        };
        for (String[] test: TESTS) {
            List<String> expected = Arrays.asList(test[0].split(" "));
            String input = test[1];
            testFreetext(input, expected);
        }
    }

    public void testFreetext(String input, List<String> expected)
                                                            throws IOException {
        FreeTextAnalyzer fa = new FreeTextAnalyzer();
        TokenStream stream = fa.tokenStream("dummy", new StringReader(input));
        assertTerms(expected, stream);
    }

    private void assertTerms(List<String> expected, TokenStream actual)
                                                            throws IOException {
        List<String> terms = getTerms(actual);
        String es = Strings.join(expected, "', '");
        String as = Strings.join(terms, "', '");
        assertEquals("There should be the same number of terms\nExpected: '"
                     + es + "'\nActual:   '" + as + "'\n",
                     expected.size(), terms.size());
        for (int i = 0 ; i < expected.size() ; i++) {
            assertEquals("The Strings ap position " + i + " should be equal\n"
                         + "Expected: '" + es + "'\nActual:   '" + as + "'\n",
                         expected.get(i), terms.get(i));
        }
    }

    private List<String> getTerms(TokenStream tokens) throws IOException {
        List<String> result = new ArrayList<String>();
        while (tokens.incrementToken()) {
            Iterator<AttributeImpl> ai = tokens.getAttributeImplsIterator();
            while (ai.hasNext()) {
                AttributeImpl a = ai.next();
                if (a instanceof CharTermAttributeImpl) {
                    result.add(a.toString());
                }
            }
        }
        return result;
    }
}
