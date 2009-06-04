/* $Id:$
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

import java.io.*;
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

    public TermStatExtractor(Configuration conf) {
        maxOpenSources = conf.getInt(CONF_MAXOPENSOURCES, maxOpenSources);
    }

    /**
     * Iterates through all terms in the IndexReader and dumps the docFreq of
     * the terms to the destination.
     * @param ir          an IndexReader for a Lucene index.
     * @param destination where to store the Term-statistics.
     * @throws IOException if extraction failed due to an I/O error.
     */
    public void dumpStats(IndexReader ir, File destination) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug("dumpStats(..., " + destination + ") called");
        Profiler profiler = new Profiler();
        Writer out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(destination, false), "utf-8"));
        TermEnum terms = ir.terms();
        while (terms.next()) {
            out.write(terms.term().field());
            out.write(":");
            out.write(terms.term().text().replace("\n", "\\n"));
            out.write("\t");
            out.write(Integer.toString(terms.docFreq()));
            out.write("\n");
            profiler.beat();
        }
        out.close();
        log.info(String.format("Finished extracting %d  docFreq's in %s. "
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
        if (sources.size() <= maxOpenSources) {
            doMerge(sources, destination);
        } else {
            File tempDest = File.createTempFile(
                    "merge_", null, destination.getParentFile());
            doMerge(sources.subList(0, maxOpenSources), tempDest);
            List<File> pending = new ArrayList<File>(sources.size());
            pending.addAll(sources.subList(maxOpenSources, sources.size()));
            pending.add(tempDest);
            mergeStats(pending, destination);
            tempDest.delete();
        }
    }

    // TODO: Make the merger more robust with regard to errors in the sources
    private void doMerge(List<File> fileSources, File destination) throws
                                                                   IOException {
        Profiler profiler = new Profiler();
        List<PeekReader> sources =
                new ArrayList<PeekReader>(fileSources.size());
        for (File source: fileSources) {
            try {
                log.trace("Opening the source '" + source + "'");
                PeekReader in = new PeekReader(source);
                sources.add(in);
            } catch (IOException e) {
                log.error(String.format(
                        "Error: Unable to open the source-file '%s'. The file"
                        + " will be ignored for merging", source), e);
            }
        }
        Writer out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(destination, false), "utf-8"));
        while(fileSources.size() > 0) {
            // Find the first term in Unicode-order
            PeekReader first = null;
            for (PeekReader source: sources) {
                if ("".equals(source.peek())) { // Skip blanks
                    source.readLine();
                    continue;
                }
                if (source.peek() != null
                    && (first == null
                        || source.peekTerm().compareTo(first.peekTerm()) < 0)) {
                     first = source;
                }
            }
            if (first == null) { // No more terms
                break;
            }
            // Sum it up and remove empty
            int sum = 0;
            String term = first.peekTerm();
            for (int i = sources.size() - 1 ; i >= 0 ; i++) {
                PeekReader source = sources.get(i);
                String sourceTerm = source.peekTerm();
                if (sourceTerm != null && sourceTerm.equals(term)) {
                    sum += source.peekDocFreq();
                    source.readLine();
                }
                // Remove depleted
                if (source.peek() == null) {
                    sources.remove(sources.size()-1);
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("Summed docFreq for '" + term + "' is " + sum);
            }
            out.write(term);
            out.write("\t");
            out.write(Integer.toString(sum));
            out.write("\n");
            profiler.beat();
        }
        out.close();
        log.debug(String.format(
                "Finished merging %d sources to '%s' in %s",
                sources.size(), destination, profiler.getSpendTime()));
    }

    /**
     * Simple Reader-wrapper that auto-closes on EOF and allows for peeking the
     * next term without advancing in the file. Do not use this outside of
     * TermStatsextractor as it is not a full implementation - only the
     * overridden methods are guaranteed to work!
     */
    private class PeekReader extends BufferedReader {
        private File source;
        private String nextLine = null;
        private boolean eofReached = false;

        public PeekReader(File source) throws IOException {
            super(new InputStreamReader(new FileInputStream(source), "utf-8"));
            this.source = source;
        }

        @Override
        public String readLine() throws IOException {
            if (nextLine != null) {
                String result = nextLine;
                nextLine = null;
                return result;
            }
            if (eofReached) {
                return null;
            }
            String result = super.readLine();
            if (result == null) {
                close();
            }
            return result;
        }

        /**
         * Extracts the next line without advancing in the file.
         * @return the next line in the file or null if EOF has been reached.
         * @throws IOException if the line could not be read.
         */
        public String peek() throws IOException {
            if (nextLine != null) {
                return nextLine;
            }
            if (eofReached) {
                return null;
            }
            nextLine = readLine();
            return nextLine;
        }

        /**
         * @return the next term (without docFreq), null if EOF.
         * @throws IOException if the term could not be extracted.
         */
        public String peekTerm() throws IOException {
            if (peek() == null) {
                return null;
            }
            if ("".equals(nextLine)) {
                return "";
            }
            int pos = peek().lastIndexOf("\t");
            if (pos == -1) {
                throw new IOException("Unable to locate docFreq-divider for '"
                                      + nextLine + "'");
            }
            return nextLine.substring(0, pos);
        }

        /**
         * @return the docFreq for the current term, null if EOF and 0 if the
         *         term is the empty String.
         * @throws IOException if the docFreq could not be extracted.
         */
        public Integer peekDocFreq() throws IOException {
            if (peek() == null) {
                return null;
            }
            if ("".equals(nextLine)) {
                return 0; // Is this acceptable?
            }
            int pos = peek().lastIndexOf("\t");
            if (pos == -1) {
                throw new IOException("Unable to determine docFreq for '"
                                      + nextLine + "'");
            }
            try {
                return Integer.parseInt(nextLine.substring(pos + 1));
            } catch (Exception e) {
                throw new IOException("Unable to parse docFreq for '"
                                      + nextLine + "'", e);
            }
        }

        @Override
        public void close() throws IOException {
            if (eofReached) {
                return;
            }
            log.trace(String.format("Closing '%s'", source));
            eofReached = true;
            super.close();
        }
    }
}
