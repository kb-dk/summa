package dk.statsbiblioteket.summa.common.lucene.analysis;

import dk.statsbiblioteket.util.Strings;

import java.util.List;
import java.util.Arrays;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import java.io.Reader;
import java.io.StringReader;

/**
 * Load test data into memory so we have it around. This is done to
 * prevent any IO during performance test of the analyzers
 */
public class SampleDataLoader {

    static final List<String> data = Arrays.asList(
            Strings.flushLocal(getSystemResourceAsStream(
                                                    "data/lgpl-2.1-german.txt"))
            // Add more sample data sources here
    );

    public static String getDataString(int i) {
        return data.get(i);
    }

    public static Reader getDataReader(int i) {
        return new StringReader(data.get(i));
    }

}
