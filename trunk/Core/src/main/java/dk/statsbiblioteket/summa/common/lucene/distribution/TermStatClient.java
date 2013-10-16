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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.Triple;
import dk.statsbiblioteket.summa.support.lucene.LuceneUtil;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.exposed.ExposedFactory;
import org.apache.lucene.search.exposed.ExposedTuple;
import org.apache.lucene.search.exposed.TermProvider;
import org.apache.lucene.search.exposed.compare.NamedNaturalComparator;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Creates and merges dumps of the term-statistics for Lucene-indexes.
 * </p><p>
 * The format of the output-file is {@code field:text\tfrequency\n} where line-breaks in text are escaped with \n and
 * frequency is written with {@code Integer.toString()}.
 */
// TODO: Add Java String order to BytesRef order sorter
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatClient implements Configurable {
    private static Log log = LogFactory.getLog(TermStatClient.class);

    private Configuration termStatConf;

    public TermStatClient(Configuration conf) {
        termStatConf = conf;
    }
    private TermStatClient() {
        this(Configuration.newMemoryBased());
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            usage();
            return;
        }
        List<String> arguments = new ArrayList<String>(Arrays.asList(args));
        String first = arguments.remove(0);
        if ("-e".equals(first)) {
            extract(arguments);
        } else if ("-m".equals(first)) {
            merge(arguments);
        } else if ("-u".equals(first)) {
            unique(arguments);
        } else {
            System.err.println("First argument must be -e or -m but was " + first);
            usage();
        }
    }

    private static void extract(List<String> arguments) throws IOException {
        int mdf = 1;
        int mtl = -1;
        boolean skipSpace = false;
        File index = null;
        String columnname = null;
        String destination = null;
        List<String> fields = new ArrayList<String>(10);
        boolean fieldsActive = false;
        while (!arguments.isEmpty()) {
            String argument = arguments.remove(0);
            if ("-i".equals(argument)) {
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-i must be followed by a path");
                }
                index = new File(arguments.remove(0));
            } else if ("-d".equals(argument)) {
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-d must be followed by a path");
                }
                destination = arguments.remove(0);
            } else if ("-c".equals(argument)) {
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-c must be followed by a column name");
                }
                columnname = arguments.remove(0);
            } else if ("-f".equals(argument)) {
                fieldsActive = true;
                continue;
            } else if ("-s".equals(argument)) {
                skipSpace = true;
            } else if ("-mdf".equals(argument)) {
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-mdf must be followed by an integer");
                }
                mdf = Integer.parseInt(arguments.remove(0));
            } else if ("-mtl".equals(argument)) {
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-mtl must be followed by an integer");
                }
                mtl = Integer.parseInt(arguments.remove(0));
            } else if (fieldsActive) {
                fields.add(argument);
                continue;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + argument);
            }
            fieldsActive = false;
        }
        new TermStatClient().extract(index, destination, columnname, fields, mdf, mtl, skipSpace);
    }

    private enum MODE {none, input, column}
    private static void merge(List<String> arguments) {
        List<File> inputs = new ArrayList<File>(10);
        List<String> columns = new ArrayList<String>(10);
        int mdf = 1;
        String destination = null;
        MODE mode = MODE.none;
        while (!arguments.isEmpty()) {
            String argument = arguments.remove(0);
            if ("-d".equals(argument)) {
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-d must be followed by a path");
                }
                destination = arguments.remove(0);
            } else if ("-c".equals(argument)) {
                mode = MODE.column;
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-f must be followed by one or more fields");
                }
                continue;
            } else if ("-i".equals(argument)) {
                mode = MODE.input;
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-i must be followed by one or more fields");
                }
                continue;
            } else if ("-mdf".equals(argument)) {
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-mdf must be followed by an integer");
                }
                mdf = Integer.parseInt(arguments.remove(0));
            } else if (mode == MODE.column) {
                columns.add(argument);
                continue;
            } else if (mode == MODE.input) {
                inputs.add(new File(argument));
                continue;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + argument);
            }
            mode = MODE.none;
        }
        new TermStatClient().merge(inputs, columns, destination, mdf);
    }

    public static void unique(List<String> arguments) throws IOException {
        File index = null;
        List<String> fields = new ArrayList<String>(10);
        boolean fieldsActive = false;
        boolean countOnly = false;
        while (!arguments.isEmpty()) {
            String argument = arguments.remove(0);
            if ("-i".equals(argument)) {
                if (arguments.isEmpty()) {
                    throw new IllegalArgumentException("-i must be followed by a path");
                }
                index = new File(arguments.remove(0));
            } else if ("-f".equals(argument)) {
                fieldsActive = true;
                continue;
            } else if ("-c".equals(argument)) {
                countOnly = true;
            } else if (fieldsActive) {
                fields.add(argument);
                continue;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + argument);
            }
            fieldsActive = false;
        }
        new TermStatClient().unique(index, fields, countOnly);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void usage() {
        System.out.println(
                "Usage:\n"
                + "TermStatClient -u -i index [-f field*]"
                + "TermStatClient -e [-mdf minimumdocumentfrequency] -i index -d destination [-f field*] "
                +                    "-c columnprefix [-s] [-mtl maxtermlength]\n"
                + "TermStatClient -m [-mdf minimumdocumentfrequency] -i termstat* -d destination -c columnregexp*\n"
                + "-u: Return the number of unique terms\n"
                + " -i: Lucene index\n"
                + " -c: Count only, no term listing\n"
                + " -f: Lucene fields as regexp\n"
                + "\n"
                + "-e: Extract termstats from index and store the stats at destination\n"
                + " -i: Lucene index\n"
                + " -f: Lucene fields as regexp\n"
                + " -c: Destination column prefix\n"
                + " -d: Destination for the term stats\n"
                + "\n"
                + "-m: Merge termstats into destination\n"
                + " -i: Input termstats\n"
                + " -c: column names as regexp\n"
                + " -d: Destination for the merged term stats\n"
                + "\n"
                + "-mdf: Only store terms where the document frequency is the given number or above\n"
                + "-mtl: Only store terms when the length is <= this\n"
                + " -s: Skip terms containing space\n"
        );
        // TODO: Add skipSpace + mtl to merge
    }

    public void extract(File index, String destination, String columnPrefix,
                        List<String> fieldRegexps, int mdf, int mtl, boolean skipSpace)
        throws IOException {
        if (mtl == -1) {
            mtl = Integer.MAX_VALUE;
        }
        if (index == null) {
            throw new IllegalArgumentException("No index specified");
        }
        if (destination == null) {
            throw new IllegalArgumentException("No destination specified");
        }
        if (fieldRegexps.isEmpty()) {
            log.info("No fields specified. Extracting all fields");
            fieldRegexps.add(".*");
        }
        log.info(String.format("Extracting fields %s from %s with mdf=%d to %s",
                               Strings.join(fieldRegexps, ", "), index, mdf, destination));
        Profiler profiler = new Profiler();

        IndexReader ir = IndexReader.open(new NIOFSDirectory(index));
        Set<String> fields = getFields(ir, fieldRegexps, index);

        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        TermStat termStat = new TermStat(Configuration.newMemoryBased());
        // TODO: Add timestamp for extraction
        String header = String.format("TermStats created from %s at %2$tF %2$tT",
                                      index, System.currentTimeMillis());
        final String[] columns = new String[]{ "term", "tf_" + columnPrefix, "df_" + columnPrefix};
        termStat.create(new File(destination), header, columns);
        termStat.setDocCount(getDocCount(ir));
        termStat.setSource(index.toString());
        termStat.setMinDocumentFrequency(mdf);

        TermStatSource factory = new TermStatSource(index);
        // TODO: docCount
        Iterator<Triple<BytesRef, Long, Long>> source = factory.getTerms(fields);
        final long[] cache = new long[2];
        while (source.hasNext()) {
            Triple<BytesRef, Long, Long> triple = source.next();
            cache[0] =  triple.getValue2(); // tf
            cache[1] =  triple.getValue3(); // df

            if (triple.getValue3() >= mdf) {
                if (log.isTraceEnabled()) {
                    log.trace("Adding " + triple);
                }
                String term = triple.getValue1().utf8ToString();
                if ("".equals(term)) {
                    log.warn("Received empty term, which should not happen. Skipping " + triple);
                    continue;
                }
                if (skipSpace && term.contains(" ")) {
                    log.trace("Skipping '" + term + "' as it contains a space");
                    continue;
                }
                if (term.length() > mtl) {
                    log.trace("Skipping '" + term + "' as it is longer than " + mtl);
                    continue;
                }
                termStat.add(new TermEntry(term, cache, columns));
            } else if (log.isTraceEnabled()) {
                log.trace("Skipping " + triple + " as mdf=" + mdf);
            }
        }
        termStat.store();
        termStat.close();
        log.info("Finished extraction in " + profiler.getSpendTime());
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void unique(File index, List<String> fieldRegexps, boolean countOnly) throws IOException {
        if (index == null) {
            throw new IllegalArgumentException("No index specified");
        }
        if (fieldRegexps.isEmpty()) {
            log.info("No fields specified. Extracting unique terms for all fields");
            fieldRegexps.add(".*");
        }
        log.info(String.format("Extracting unique terms for fields %s from %s", 
                               Strings.join(fieldRegexps, ", "), index));

        Profiler profiler = new Profiler();
        IndexReader ir = DirectoryReader.open(new NIOFSDirectory(index));
        Set<String> fields = getFields(ir, fieldRegexps, index);

        TermProvider termProvider = ExposedFactory.createProvider(
                ir, "all", new ArrayList<String>(fields), new NamedNaturalComparator());
        if (countOnly) {
            System.out.println(termProvider.getUniqueTermCount());
            return;
        }

        Iterator<ExposedTuple> terms = termProvider.getIterator(false);
        int termCount = 0;
        while (terms.hasNext()) {
            System.out.println(terms.next().term.utf8ToString());
            termCount++;
        }
        ir.close();
        log.info("Finished extraction in " + profiler.getSpendTime() + " with " + termCount
                 + " unique terms from fields " + Strings.join(fields));
    }

    private Set<String> getFields(IndexReader ir, List<String> fieldRegexps, File index) throws IOException {
        Set<String> fields = new HashSet<String>(20);

        List<AtomicReader> readers = LuceneUtil.gatherSubReaders(ir);
        for (AtomicReader ar: readers) {
            for (String fieldName : ar.fields()) {
                for (String regexp: fieldRegexps) {
                    if (Pattern.compile(regexp).matcher(fieldName).matches()) {
                        fields.add(fieldName);
                    }
                }
            }
        }
        if (fields.isEmpty()) {
            log.warn("Unable to expand " + Strings.join(fieldRegexps, ", ") + " to any fields in " + index);
        }
        return fields;
    }

    private long getDocCount(IndexReader ir) {
        if (ir instanceof AtomicReader) {
            Bits liveDocs = ((AtomicReader)ir).getLiveDocs();
            if (liveDocs == null) {
                return ir.maxDoc();
            }
            long count = ir.maxDoc();
            for (int i = 0 ; i < liveDocs.length() ; i++) {
                if (!liveDocs.get(i)) {
                    count--;
                }
            }
            return count;
        }
        long count = 0;
        List<AtomicReader> readers = LuceneUtil.gatherSubReaders(ir);
        for (AtomicReader i: readers) {
            count += getDocCount(i);
        }
        return count;
    }

    public void merge(List<File> inputs, List<String> columns, String destination, int mdf) {
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("One or more inputs must be specified");
        }
        if (destination == null) {
            throw new IllegalArgumentException("No destination specified");
        }
        if (columns.isEmpty()) {
            log.info("No columns specified. Merging all columns");
            columns.add(".*");
        }
        log.info(String.format("Merging columns %s from inputs %s with mdf %d to %s",
                               Strings.join(columns, ", "), Strings.join(inputs, ", "), mdf, destination));
        Profiler profiler = new Profiler();

        // TODO: Implement this. Remember docCount

        log.info("Finished merging in " + profiler.getSpendTime());
    }

    /**
     * Iterates through all terms in the index and dumps the tf and df stats of
     * the terms to the destination.
     * </p><p>
     * This is a shorthand for the method
     * {@link #extract(java.io.File, String, String, java.util.List, int, int, boolean)}
     * called as {@code extract(index, destination, .*, 1, false)}.
     * @param index       the location of a Lucene index.
     * @param destination where to store the Term-statistics.
     * @throws IOException if extraction failed due to an I/O error.
     */
    public void dumpStats(File index, File destination) throws IOException {
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format("dumpStats(%s, %s) called", index, destination));
        extract(index, destination.toString(), "dumpstats", Arrays.asList(".*"), 1, -1, false);
    }

    /**
     * Merges multiple stats created by @link #dumpStats into a single file.
     * Merging simply adds the docFreq's for the different Terms. In order for
     * the merger to work, the term-statics must be ordered (dumpStats does
     * this automatically).
     * @param sources     any number of term-statistics.
     * @param destination where to store the result of the merge.
     * @throws IOException if the destination could not be updated.
     */
    public void mergeStats(List<File> sources, File destination) throws IOException {

        // TODO: Split on maxOpenSources or the merger will die on ~500 sources
        doMerge(sources, destination);
    }

    // TODO: Make the merger more robust with regard to errors in the sources
    private void doMerge(List<File> fileSources, File destinationFile) throws IOException {
/*        Profiler profiler = new Profiler();
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
                */
    }

/*    private void updateMergeStats(List<TermStat> sources, TermStat destination) {
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
    }*/
}

