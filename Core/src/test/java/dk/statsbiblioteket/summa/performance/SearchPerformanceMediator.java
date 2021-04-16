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
package dk.statsbiblioteket.summa.performance;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.text.ParseException;

/**
 * handles data-transfer and communication among SearchPerformance-classes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchPerformanceMediator {
    private static Log log = LogFactory.getLog(SearchPerformanceMediator.class);

    /**
     * If true, searches are skipped and only query-parsing is performed.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_SIMULATE = "summa.performance.simulate";
    public static final boolean DEFAULT_SIMULATE = false;

    /**
     * The maximum number of hits to extract from the result set.
     * Note that fields are also extracted as part of this.
     * </p><p>
     * Optional. Default is 20.
     */
    public static final String CONF_MAX_HITS = "summa.performance.maxhits";
    public static final int DEFAULT_MAX_HITS = 20;

    /**
     * The maximum number of queries to use.
     * </p><p>
     * Optional. Default is Integer.MAX_VALUE.
     */
    public static final String CONF_MAX_QUERIES =
            "summa.performance.maxqueries";
    public static final int DEFAULT_MAX_QUERIES = Integer.MAX_VALUE;

    /**
     * This will be prepended to the filename of the generated log.
     * </p><p>
     * Optional. Default is "t" + #threads + (shared ? "" : "u").
     */
    public static final String CONF_FEEDBACK_PREFIX =
            "summa.performance.feedback.prefix";
    public static final String DEFAULT_FEEDBACK_PREFIX = "";

    /**
     * This will be appended to the filename of the generated log.
     * </p><p>
     * Optional. Default is ".log".
     */
    public static final String CONF_FEEDBACK_POSTFIX =
            "summa.performance.feedback.postfix";
    public static final String DEFAULT_FEEDBACK_POSTFIX = ".log";

    /**
     * The locations of the queries to use. One query/line.
     * </p><p>
     * Optional. Default is "queries.dat".
     */
    public static final String CONF_QUERIES_FILE =
            "summa.performance.queries.file";
    public static final String DEFAULT_QUERIES_FILE =
            "integration/performance/queries.dat";

    /**
     * A list of the fields to extract for each hit.
     * </p><p>
     * Optional. Default is "shortformat".
     */
    public static final String CONF_FIELDS = "summa.performance.fields";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String[] DEFAULT_FIELDS = {"shortformat"};

    /**
     * The location of the index descriptor. The descriptor is used by the
     * query parser.
     * </p><p>
     * Optional. Default is "index/IndexDescriptor.xml".
     */
    public static final String CONF_INDEX_DESCRIPTOR =
            "summa.performance.index.descriptor";
    public static final String DEFAULT_INDEX_DESCRIPTOR =
            "performance/index/IndexDescriptor.xml";

    public int maxHits;
    public boolean simulate;
    public SummaQueryParser parser;
    public String[] fields;

    private AtomicLong hitCount = new AtomicLong();
    private AtomicInteger queryCount = new AtomicInteger();
    private String[] queries;
    private int maxQueries;
    private Profiler profiler;
    private int feedback;
    private int logFeedback;
    private long startTime;
    private boolean shared;
    private int threads;
    private Writer output;

    public SearchPerformanceMediator(Configuration conf, int threads,
                                     boolean sharedSearcher) throws IOException{
        log.trace("Creating mediator");
        queries = getQueries(conf);
        maxQueries = conf.getInt(CONF_MAX_QUERIES, DEFAULT_MAX_QUERIES);
        feedback = Math.min(Math.max(10, Math.min(maxQueries, queries.length)
                                         / 100), 100);
        logFeedback = Math.min(Math.max(10, Math.min(maxQueries, queries.length)
                                            / 100), 10000);
        simulate = conf.getBoolean(CONF_SIMULATE, DEFAULT_SIMULATE);
        maxHits = conf.getInt(CONF_MAX_HITS, DEFAULT_MAX_HITS);
        shared = sharedSearcher;
        this.threads = threads;
        fields = conf.getStrings(CONF_FIELDS, DEFAULT_FIELDS);

        File outputFile = new File(
                conf.getString(CONF_FEEDBACK_PREFIX,
                               DEFAULT_FEEDBACK_PREFIX)
                + getDefaultLogName()
                + conf.getString(CONF_FEEDBACK_POSTFIX,
                                 DEFAULT_FEEDBACK_POSTFIX));
        log.debug("Logging performance data to " + outputFile);
        output = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile, false)));
        try {
            File location = Resolver.getFile(conf.getString(
                    CONF_INDEX_DESCRIPTOR, DEFAULT_INDEX_DESCRIPTOR));
            if (location == null) {
                throw new IllegalArgumentException(String.format(Locale.ROOT,
                        "Unable to resolve the location '%s' to a file",
                        conf.getString(CONF_INDEX_DESCRIPTOR,
                                       DEFAULT_INDEX_DESCRIPTOR)));
            }
            parser = new SummaQueryParser(new LuceneIndexDescriptor(
                    Files.loadString(location)));
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Could not create SummaQueryParser", e);
        }
    }

    private String getDefaultLogName() {
        return "t" + threads + (shared ? "" : "u");
    }

    /*
     * Start time-taking et al.
     */
    public void start() {
        profiler = new Profiler();
        profiler.setBpsSpan(500);
        profiler.setExpectedTotal(queries.length);
        startTime = System.currentTimeMillis();
    }

    public void ping(long currentHitCount) {
        hitCount.addAndGet(currentHitCount);
        profiler.beat();
        int count = queryCount.get();
        if (count < 100 || count % feedback == 0) {
            if (count < 10 || count % 10 == 0) {
                write((System.currentTimeMillis() - startTime) / 1000
                      + " sec. " + queryCount.get() + "/"
                      + profiler.getExpectedTotal()
                      + ". Hits: " + hitCount.get()
                      + ". Q/sec: " + round(profiler.getBps(true))
                      + " (" + round(profiler.getBps(false)) 
                      + " total). ETA: " + profiler.getETAAsString(true));
            }
        }
        if (count % logFeedback == 0) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Processed " + count / feedback + "%");
        }
    }

    public void stop() throws IOException {
        String end = String.format(Locale.ROOT,
                "Tested %d queries (%d hits). In %d seconds. Average "
                + "queries/second: %.1f. Total time used: %s. Threads: %d. "
                + "One searcher/thread: %b",
                                   queries.length, hitCount.get(),
                (System.currentTimeMillis() - startTime) / 1000,
                                   profiler.getBps(false), profiler.getSpendTime(),
                                   threads, !shared);
        write(end);
        output.close();
        System.out.println(end);
    }

    private void write(String s) {
        try {
            output.write(s);
            output.write("\n");
        } catch (IOException e) {
            log.warn("IOException writing '" + s + "'");
        }
    }

    public String round(double v) {
        return Double.toString(Math.round(v * 10) / 10.0);
    }

    /**
     * @return the next query or null, if no more queryes are available.
     */
    public String getNextQuery() {
        int queryPos = queryCount.getAndAdd(1);
        if (queryPos >= queries.length || queryPos >= maxQueries) {
            log.debug("No more queries");
            return null;
        }
        return queries[queryPos];
    }

    /**
     * Loads queries from {@link #CONF_QUERIES_FILE} and returns them.
     * Singleton.
     * @param conf Configuration containing the location of the queries.
     * @return an array of queries.
     * @throws IOException if the queries could not be loaded.
     */

    private String[] getQueries(Configuration conf) throws IOException {
        if (queries != null) {
            return queries;
        }
        String location = conf.getString(CONF_QUERIES_FILE,
                                         DEFAULT_QUERIES_FILE);
        log.trace("Loading queries from '" + location + "'");
        try {
            queries = Files.loadString(Resolver.getFile(location)).split("\n");
        } catch (IOException e) {
            throw new IOException("Unable to load queries from '" + location
                                  + "'", e);
        } catch (NullPointerException e) {
            throw new IOException("Unable to resolve '" + location
                                  + "' to a File", e);
        }
        log.debug(String.format(Locale.ROOT, "Got %d queries from '%s'",
                                queries.length, location));
        return queries;
    }

}

