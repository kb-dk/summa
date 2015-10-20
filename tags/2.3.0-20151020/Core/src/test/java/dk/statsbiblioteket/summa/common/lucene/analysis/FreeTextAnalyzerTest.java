package dk.statsbiblioteket.summa.common.lucene.analysis;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

public class FreeTextAnalyzerTest extends AnalyzerTestCase {
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

    public void testFreetext(String input, List<String> expected) throws IOException {
        FreeTextAnalyzer fa = new FreeTextAnalyzer();
        TokenStream stream = fa.tokenStream("dummy", new StringReader(input));
        assertTerms(expected, stream);
    }

}
