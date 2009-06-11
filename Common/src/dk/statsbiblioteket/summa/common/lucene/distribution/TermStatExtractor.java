/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.lucene.distribution;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Creates and merges dumps of the term-statistics for Lucene-indexes.
 * </p><p>
 * The format of the output-file is {@code field:text\tfrequency\n} where
 * line-breaks in text are escaped with \n and frequency is written with
 * {@code Integer.toString()}.
 * </p><p>
 * When configuring the TermStatExtractor, consider specifying
 * {@link TermStat#CONF_MEMORYBASED}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatExtractor {
    private static Log log = LogFactory.getLog(TermStatExtractor.class);

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
        IndexReader ir = IndexReader.open(
                FSDirectory.getDirectory(index, null));
        Profiler profiler = new Profiler();
        TermStat stats = new TermStat(termStatConf);
        stats.create(destination);
        TermEnum terms = ir.terms();
        while (terms.next()) {
            stats.dirtyAdd(new TermEntry(
                    terms.term().field() + ":" + terms.term().text(),
                    terms.docFreq()));
            profiler.beat();
        }
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
                source.open(fileSource, true);
                if (source.size() == 0) {
                    log.debug("the source " + fileSource + " did not contain "
                              + "any terms. Skipping source");
                    source.close();
                    continue;
                }
                sources.add(source);
            } catch (Exception e) {
                log.warn(String.format(
                        "Unable to open TermStat with source '%s'. "
                        + "Skipping source", fileSource), e);
            }
        }
        log.debug(fileSources.size() + " sources opened. Beginning merge");
        while(sources.size() > 0) {
            // Find the first term in Unicode-order
            TermStat first = null;
            for (TermStat candidate : sources) {
                if (first == null ||
                    candidate.get(candidate.position).getTerm().compareTo(
                            first.get(first.position).getTerm()) < 0) {
                    first = candidate;
                }
            }

            // Sum the term counts and remove empty
            TermEntry firstEntry = first.get(first.position);
            int sum = 0; //firstEntry.getCount();
            String term = firstEntry.getTerm();
            for (int i = sources.size() - 1 ; i >= 0 ; i--) {
                TermStat current = sources.get(i);
                String currentTerm = current.get(current.position).getTerm();
                if (currentTerm.equals(term)) {
                    sum += current.get(current.position).getCount();
                    current.position++;
                    // Remove depleted
                    if (current.position == current.size()) {
                        current.close();
                        sources.remove(i);
                    }
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("Summed docFreq for '" + term + "' is " + sum);
            }
            destination.dirtyAdd(new TermEntry(term, sum));
            profiler.beat();
        }
        log.debug("Finished merging. Cleaning up...");
        destination.cleanup();
        log.debug("Cleanup finished. Storing...");
        destination.store();
        log.debug("Finished storing");
        destination.close();
        log.debug(String.format(
                "Finished merging %d sources to '%s' in %s",
                fileSources.size(), destination, profiler.getSpendTime()));
    }
}
