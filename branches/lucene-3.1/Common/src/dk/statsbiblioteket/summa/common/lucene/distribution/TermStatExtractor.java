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
package dk.statsbiblioteket.summa.common.lucene.distribution;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;

/**
 * Creates and merges dumps of the term-statistics for Lucene-indexes.
 * </p><p>
 * The format of the output-file is {@code field:text\tfrequency\n} where
 * line-breaks in text are escaped with \n and frequency is written with
 * {@code Integer.toString()}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatExtractor {
    private static Log log = LogFactory.getLog(TermStatExtractor.class);

    // TODO: Optimize space by discarding all terms with count 1 in the merger
    // When the count for a term is requested, "not found" should be taken as 1

    /**
     * The maximum number of sources to open at the same time in
     * {@link #mergeStats}. If the number of sources exceeds this, the merging
     * will be done using multiple steps, which slows down the merging speed.
     * </p><p>
     * Warning: Do not set this value without knowledge of the limitations on
     * the number of open file handles in the JVM and the operating system.
     * </p><p>
     * Optional. Default is 100.
     */
    public static final String CONF_MAXOPENSOURCES =
            "summa.common.termstatextractor.maxopensources";
    public static final int DEFAULT_MAXOPENSOURCES = 100;

    private int maxOpenSources = DEFAULT_MAXOPENSOURCES;
    private Configuration termStatConf;

    public TermStatExtractor(Configuration conf) {
        maxOpenSources = conf.getInt(CONF_MAXOPENSOURCES, maxOpenSources);
        termStatConf = conf;
    }
    private TermStatExtractor() {
        this(Configuration.newMemoryBased());
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            usage();
            return;
        }
        if ("-e".equals((args[0]))) {
            if (args.length != 3) {
                System.err.println("Extraction takes only 2 arguments");
                usage();
            } else {
                System.out.println(String.format(
                        "Extracting term stats fron index at '%s' to '%s'",
                        args[1], args[2]));
                long startTime = System.currentTimeMillis();
                new TermStatExtractor().dumpStats(
                        new File(args[1]), new File(args[2]));
                System.out.println(String.format(
                        "Finished term stat extraction in %d seconds",
                        (System.currentTimeMillis() - startTime) / 1000));
            }
        } else if ("-m".equals((args[0]))) {
            List<File> sources = new ArrayList<File>(args.length-2);
            for (int i = 1 ; i < args.length - 1 ; i++) {
                sources.add(new File(args[i]));
            }
            File dest = new File(args[args.length-1]);
            System.out.println(String.format("Merging %d sources into '%s'",
                                             sources.size(), dest));
            long startTime = System.currentTimeMillis();
            new TermStatExtractor().mergeStats(sources, dest);
            System.out.println(String.format(
                    "Finished term stat merging in %d seconds",
                    (System.currentTimeMillis() - startTime) / 1000));
        } else {
            usage();
        }
    }

    public static void usage() {
        System.out.println(
                "Usage:\n"
                + "TermStatExtractor [-e index destination]\n"
                + "TermStatExtractor [-m termstats* destination]\n\n"
                + "-e: Extract termstats from index and store the stats at "
                + "destination\n"
                + "-m: Merge termstats into destination");
    }

    /**
     * Iterates through all terms in the IndexReader and dumps the docFreq of
     * the terms to the destination.
     * @param index       the location of a Lucene index.
     * @param destination where to store the Term-statistics.
     * @throws IOException if extraction failed due to an I/O error.
     */
    public void dumpStats(File index, File destination) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format("dumpStats(%s, %s) called",
                                index, destination));
        IndexReader ir = IndexReader.open(new NIOFSDirectory(index));
        Profiler profiler = new Profiler();
        TermStat stats = new TermStat(termStatConf);
        stats.create(destination);
        TermEnum terms = ir.terms();
        while (terms.next()) {
            stats.add(new TermEntry(
                    terms.term().field() + ":" + terms.term().text(),
                    terms.docFreq()));
            profiler.beat();
        }
        stats.setDocCount(ir.maxDoc());
        stats.setSource(index.toString());
        stats.store();
        stats.close();
        ir.close();
        log.info(String.format(
                "Finished extracting %d terms with docFreqs in %s. "
                + "Average extraction speed: %s docFreq's/sec",
                profiler.getBeats(), profiler.getSpendTime(),
                profiler.getBps(false)));
    }

    /**
     * Merges multiple stats created by {@link #dumpStats} into a single file.
     * Merging simply adds the docFreq's for the different Terms. In order for
     * the merger to work, the term-statics must be ordered (dumpStats does
     * this automatically).
     * @param sources     any number of term-statistics.
     * @param destination where to store the result of the merge.
     * @throws IOException if the destination could not be updated.
     */
    public void mergeStats(List<File> sources, File destination) throws
                                                                   IOException {

        // TODO: Split on maxOpenSources or the merger will die on ~500 sources
        doMerge(sources, destination);
    }

    // TODO: Make the merger more robust with regard to errors in the sources
    private void doMerge(List<File> fileSources, File destinationFile)
                                                            throws IOException {
        Profiler profiler = new Profiler();
        TermStat destination = new TermStat(termStatConf);
        destination.create(destinationFile);

        List<TermStat> sources = new ArrayList<TermStat>(fileSources.size());
        for (File fileSource: fileSources) {
            log.trace("Opening the source '" + fileSource + "'");
            try {
                TermStat source = new TermStat(termStatConf);
                source.open(fileSource);
                source.reset();
                sources.add(source);
            } catch (Exception e) {
                log.warn(String.format(
                        "Unable to open TermStat with source '%s'. "
                        + "Skipping source", fileSource), e);
            }
        }
        log.debug(fileSources.size() + " sources opened. Calculating stats");

        updateMergeStats(sources, destination);

        log.debug("Beginning merge");
        while(sources.size() > 0) {
            // Find the first term in Unicode-order
            TermStat first = null;
            for (TermStat candidate : sources) {
                TermEntry candidateEntry = candidate.peek();
                if (candidateEntry == null) {
                    continue;
                }
                if (first == null ||
                    candidate.peek().getTerm().compareTo(
                            first.peek().getTerm()) < 0) {
                    first = candidate;
                }
            }

            // Sum the term counts and remove empty
            TermEntry firstEntry = first.peek();
            int sum = 0; //firstEntry.getCount();
            String term = firstEntry.getTerm();
            for (int i = sources.size() - 1 ; i >= 0 ; i--) {
                TermStat current = sources.get(i);
                TermEntry currentTerm = current.peek();
                if (currentTerm == null) {
                    current.close();
                    sources.remove(i);
                    continue;
                }
                if (currentTerm.getTerm().equals(term)) {
                    sum += currentTerm.getCount();
                    current.get();
                    // Remove depleted
                    if (!current.hasNext()) {
                        current.close();
                        sources.remove(i);
                    }
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("Summed docFreq for '" + term + "' is " + sum);
            }
            destination.add(new TermEntry(term, sum));
            profiler.beat();
        }
        log.debug("Finished merging. Storing...");
        destination.store();
        log.debug("Finished storing");
        destination.close();
        log.debug(String.format(
                "Finished merging %d sources to '%s' in %s",
                fileSources.size(), destination, profiler.getSpendTime()));
    }

    private void updateMergeStats(List<TermStat> sources, TermStat destination) {
        long docCount = 0;
        StringWriter sw = new StringWriter(sources.size() * 50);
        sw.append("merge(");
        boolean firstS = true;
        for (TermStat source: sources) {
            docCount += source.getDocCount();
            sw.append(firstS ? "" : ", ").append(source.getSource());
            firstS = false;
        }
        sw.append(")");
        destination.setDocCount(docCount);
        destination.setSource(sw.toString());
    }
}

