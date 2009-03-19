package dk.statsbiblioteket.summa.common.lucene.analysis;

import static dk.statsbiblioteket.summa.common.lucene.analysis.SampleDataLoader.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;

import java.io.Writer;
import java.io.CharArrayWriter;

/**
 * Performance tests for the analyzers
 */
public class AnalyzerPerformance {

    final int NUM_TESTS = 1000;

    Analyzer a;
    TokenStream t;

    public AnalyzerPerformance() throws Exception {
        //a = new SummaStandardAnalyzer();
        a = new SummaAnalyzer(null, true, null, true, true);

        System.out.println("Running " + NUM_TESTS + " tests on "
                           + a.getClass().getSimpleName());

        CharArrayWriter out = new CharArrayWriter();
        long testTime = 0;
        long testStart;
        long dummyInspection = 0; // Prevent sneaky JIT compiling
        for (int i = 0; i < NUM_TESTS; i++) {
            testStart = System.nanoTime();
            t = a.reusableTokenStream("testField", getDataReader(0));
            dumpTokens(t, out);
            dummyInspection += out.size();
            out.reset();
            testTime += (System.nanoTime() - testStart);

            if (i % (NUM_TESTS/5) == 0 && i != 0) {
                System.out.println("At " + i + ", avg. test time (ms) : "
                           + ((testTime/1000000D)/i));
            }
        }

        System.out.println("Total avg. test time (ms) : "
                           + ((testTime/1000000D)/NUM_TESTS));

        // Prevent sneaky JIT compiling... Hopefully :-)
        System.out.println("Garbage data: " + dummyInspection);
    }

    public static void dumpTokens(TokenStream t, Appendable out) throws Exception {
        Token tok = new Token();

        while ((tok = t.next(tok)) != null) {
            out.append("TOKEN:"+tok.term());
            out.append('\n');
        }
    }

    public static void main(String[] args) {
        try {
            new AnalyzerPerformance();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
