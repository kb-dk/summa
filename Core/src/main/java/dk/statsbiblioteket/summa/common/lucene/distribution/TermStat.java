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
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.util.ArrayUtil;
import dk.statsbiblioteket.util.LineReader;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.nio.BufferOverflowException;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent structure for TermStats from an index. The structure must be
 * filled in sequential order and allows for look ups in O(log n) using binary
 * search.
 * </p><p>
 * The structure uses a human readable persistence layer with an index table
 * for fast lookup. The format of the persistence layer is heading lines
 * prepended with '#', one line with column names, lines with terms and stats;
 * {@code
# My stat file
Term\tstat1\tstat2
Foo\t87\t123
Zoo\t54\t89
}
 * The terms must be in natural order.
 * </p><p>
 * Note: {@link #open} or {@link #create} must be called before the TermStat
 * can be updated or queried.
 * </p><p>
 * Note: this TermStat-component is limited to Integer.MAX_VALUE terms.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStat extends AbstractList<TermEntry> implements Configurable {
    private static Log log = LogFactory.getLog(TermStat.class);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String TERMSTAT_PERSISTENT_NAME = "termstats";

    /**
     * If true, the terms are memory based. If false, the content is stored on
     * storage during operation.
     * </p><p>
     * Optional. Default is false.
     */
/*    public static final String CONF_MEMORYBASED =
            "common.distribution.termstat.memorybased";
    public static final boolean DEFAULT_MEMORYBASED = false;
  */

    /**
     * The size of the LRU-cach.}.
     * </p><p>
     * Optional. Default is 10000.
     */
    public static final String CONF_CACHE_SIZE =
        "common.distribution.termstat.cachesize";
    public static final int DEFAULT_CACHE_SIZE = 10000;

    /**
     * Expert setting.
     * </p><p>
     * The buffer for the reader. If a readLine is not within the buffer, the
     * bugger will be filled from the position. The buffer size should
     * correspond to the maximum term length as described in
     * {@link LineReader#binaryLineSearch}.
     * </p><p>
     * Optional. Default is 400.
     */
    public static final String CONF_READER_BUFFER =
        "common.distribution.reader.buffer";
    public static final int DEFAULT_READER_BUFFER = 400;

    /**
     * Lines at the beginning of the persistent files that are prepended with
     * this are treated as comments.
     * </p><p>
     * Optional. Default is "#".
     */
    public static final String CONF_COMMENT_PREFIX =
        "common.distribution.comment.prefix";
    public static final String DEFAULT_COMMENT_PREFIX = "#";

    /**
     * If true, column names are assumed not to be a part of the persistent
     * file when {@link #open} results in a lookup table rebuild.
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_SKIP_COLUMNNAMES_ON_OPEN =
        "common.distribution.skipcolumnnamesonopen";
    public static final boolean DEFAULT_SKIP_COLUMNNAMES_ON_OPEN = false;

    private LineReader persistent;
    private long docCount = 0;
    private int termCount = 0;
    private long minDocumentFrequency = 0;
    private String source = "No source defined";
    private int cacheSize = DEFAULT_CACHE_SIZE;
    private int bufferSize = DEFAULT_READER_BUFFER;
    private String commentPrefix = DEFAULT_COMMENT_PREFIX;
    private boolean skipColumnNames = DEFAULT_SKIP_COLUMNNAMES_ON_OPEN;

    /**
     * Holds offsets in the backing persistent file for all line entries.
     * lookupTable[i] =   start offset for entry #i.
     * lookupTable[i+1] = start offset for entry #(i+1). If entry #(i+1) does
     *                    not exist, the table will hold the offset for
     *                    potential new entries.
     */
    private long[] lookupTable = new long[1];
    private String[] columns = null;

    /**
     * The cache facilitates faster look ups. It caches the mapping from
     * term string to entry.
     */
    private final HashMap<String, TermEntry> cache = createCache();

    public TermStat(Configuration conf) {
        cacheSize = conf.getInt(CONF_CACHE_SIZE, cacheSize);
        bufferSize = conf.getInt(CONF_READER_BUFFER, bufferSize);
        commentPrefix = conf.getString(CONF_COMMENT_PREFIX, commentPrefix);
        skipColumnNames = conf.getBoolean(
            CONF_SKIP_COLUMNNAMES_ON_OPEN, skipColumnNames);
    }

    /**
     * Open the TermStats at the given location. The TermStats are always opened
     * in read-only mode. For building term stats, call {@link #create}.
     * @param location where the TermStats are stored.
     * @return true if the open was successful.
     * @throws IOException if the open failed due to invalid persistent data.
     */
    public boolean open(File location) throws IOException {
        File realFile = getFile(location, ".dat");
        if (!realFile.exists()) {
            throw new FileNotFoundException("Unable to locate " + realFile);
        }
        synchronized (this) {
            closeExisting();
            log.info(String.format("Opening TermStats at '%s' as readonly",
                                   location));
            persistent = new LineReader(realFile, "r");
            persistent.setBufferSize(bufferSize);
            boolean result = openMeta();
            openLookup();
            return result;
        }
    }

    private void closeExisting() throws IOException {
        if (persistent != null) {
            log.debug("Closing down existing connection to persistent data");
            persistent.close();
        }
        docCount = 0;
        termCount = 0;
        cache.clear();
        lookupTable = new long[1];
        source = "No Source";
    }

    @Override
    public void clear() {
        try {
            close();
        } catch (IOException e) {
            throw new RuntimeException("IOException clearing", e);
        }
    }

    private boolean openMeta() {
        File metaFile = getFile(persistent.getFile().getParentFile(), ".meta");
        try {
            log.debug("Opening meta data from '" + metaFile + "'");
            String meta = Resolver.getUTF8Content(metaFile.toURI().toURL());
            if (log.isTraceEnabled()) {
                log.trace("Got meta data:\n" + meta);
            }
            Document dom = DOM.stringToDOM(meta, false);
            docCount = Long.parseLong(DOM.selectString(
                dom,"termstatmeta/doccount"));
            termCount = Integer.parseInt(DOM.selectString(
                dom,"termstatmeta/termcount"));
            minDocumentFrequency = Long.parseLong(DOM.selectString(
                dom,"termstatmeta/mindocumentfrequency", "0"));
            columns = DOM.selectString(dom,"termstatmeta/columns").
                split(" *, *");
            source = DOM.selectString(dom, "termstatmeta/source");
            log.debug(String.format(
                "Extracted docCount %d, termCount %d, mdf %d, columns '%s' "
                + "and source '%s'  from '%s'",
                docCount, termCount, minDocumentFrequency,
                Strings.join(columns, ", "), source, metaFile));
            return true;
        } catch (Exception e) {
            log.error(String.format(
                "Unable to open '%s' which holds the docCount. Count will "
                + "be set to 0, which will lead to wonky ranking",
                metaFile), e);
            return false;
        }
    }

    private File getFile(File location, String prefix) {
        return new File(location, TERMSTAT_PERSISTENT_NAME + prefix);
    }


    /**
     * Get stat at the given index for the given term. If the term does not
     * exist, 0 is returned.
     * @param term  the term to request stat for.
     * @param index the index for the wanted stat.
     *              Use {@link #getIndex(String)} to resolve the index.
     * @return the stat at the given index.
     */
    public long getStat(String term, int index) {
        TermEntry entry = getEntry(term);
        return entry == null ? 0 : entry.getStat(index);
    }

    /**
     * Summing equivalent to {@link #getStat(String, int)}.
     * @param term    the term to request stats for.
     * @param indexes the indexes for the stats to sum.
     *                Use {@link #getIndex(String)} to resolve the indexes.
     * @return the sum of the stats for the given indexes.
     */
    public long getSum(String term, int[] indexes) {
        TermEntry entry = getEntry(term);
        return entry == null ? 0 : entry.getSum(indexes);
    }

    /**
     * @param column the wanted column. If this matches the column for the
     *                term, -1 is returned.
     * @return the stat-index for the given column.
     * @throws ArrayIndexOutOfBoundsException if the column could not be
     *         located.
     */
    public int getIndex(String column) throws ArrayIndexOutOfBoundsException {
        for (int i = 0 ; i < columns.length ; i++) {
            if (column.equals(columns[i])) {
                return i-1;
            }
        }
        throw new ArrayIndexOutOfBoundsException(
            "Unable to locate '" + column + " in "
            + Strings.join(columns, ", "));
    }


    /**
     * Create a persistent structure at the given location, making it ready for
     * updates.
     * </p><p>
     * @param location the folder where the TermStats should be stored.
     * @param header   the header is inserted at the top of the file and
     *                 contains non-constrained information.
     *                 Optional (specify null for no header).
     * @param columns column names. The number of columns must match the
     *                 number of stat-columns plus 1. the column names will
     *                 be written after the custom heading.
     * @return true if the creation succeeded.
     * @throws IOException if the structure could not be created.
     */
    public boolean create(
        File location, String header, String[] columns) throws IOException {
        synchronized (this) {
            closeExisting();
            if (columns.length <= 1) {
                throw new IllegalArgumentException(
                    "There must be at least 2 column names, got "
                    + Strings.join(columns, ", "));
            }
            if (columns[0].startsWith("#")) {
                throw new IllegalArgumentException(
                    "The first column name must not start with '#', got "
                    + Strings.join(columns, ", "));
            }
            this.columns = columns;
            if (!location.exists()) {
                if (!location.mkdirs()) {
                    throw new IOException(String.format(
                        "Unable to create the folder '%s'", location));
                }
            }

            File per = getFile(location, ".dat");
            if (per.exists()) {
                log.debug("The data file " + per + " already exists. "
                          + "Deleting old file");
                if (!per.delete()) {
                    throw new IOException(
                        "Unable to delete old data file "
                        + per.getAbsolutePath());
                }
            }
            persistent = new LineReader(per, "rw");
            if (header != null && !"".equals(header)) {
                String[] lines = header.split("\n");
                for (String line: lines) {
                    persistent.write(commentPrefix + " ");
                    persistent.write(line);
                    persistent.write("\n");
                }
            }
            persistent.write(Strings.join(columns, "\t"));
            persistent.write("\n");
            lookupTable[0] = persistent.getPosition();
            persistent.setBufferSize(bufferSize);
            return true;
        }
    }

    /**
     * Update the persistent data for the term stat representation. This uses
     * the location given in {@link #open} or {@link #create}.
     * Note that this does not close the connection to the persistent data.
     * @throws IOException if the data could not be stored.
     */
    public void store() throws IOException {
        synchronized (this) {
            persistent.flush();
            storeLookup();
            storeMeta();
        }
    }

    private void storeMeta() throws IOException {
        File metaFile = getFile(persistent.getFile().getParentFile(), ".meta");
        log.debug(String.format(
            "StoreMeta called, storing in '%s' with docCount=%d, termCount=%d, "
            + "mdf=%d, columns=%s", metaFile, docCount, termCount,
            minDocumentFrequency, Strings.join(columns, ", ")));
        XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
        FileOutputStream fileOut = new FileOutputStream(metaFile);
        XMLStreamWriter xmlOut;
        try {
            xmlOut = xmlFactory.createXMLStreamWriter(fileOut, "utf-8");
            xmlOut.writeStartDocument();
            xmlOut.writeStartElement("termstatmeta");
            xmlOut.writeStartElement("doccount");
            xmlOut.writeCharacters(Long.toString(getDocCount()));
            if (getDocCount() == 0) {
                log.warn(String.format(
                    "docCount for source '%s' was 0. Unless the index has "
                    + "no terms, this is normally an error", getSource()));
            }
            xmlOut.writeEndElement();
            xmlOut.writeStartElement("termcount");
            xmlOut.writeCharacters(Long.toString(getTermCount()));
            if (getDocCount() == 0) {
                log.warn(String.format(
                    "termCount for source '%s' was 0. Unless the index is "
                    + "empty, this is normally an error", getSource()));
            }
            xmlOut.writeEndElement();

            xmlOut.writeStartElement("mindocumentfrequency");
            xmlOut.writeCharacters(Long.toString(minDocumentFrequency));
            xmlOut.writeEndElement();

            xmlOut.writeStartElement("columns");
            xmlOut.writeCharacters(Strings.join(columns, ", "));
            xmlOut.writeEndElement();

            //noinspection DuplicateStringLiteralInspection
            xmlOut.writeStartElement("source");
            xmlOut.writeCharacters(getSource());
            xmlOut.writeEndElement();
            xmlOut.writeEndDocument();
            xmlOut.close();
            fileOut.close();
            log.debug("StoreMeta completed for '" + metaFile + "'");
        } catch (Exception e) {
            throw new IOException(
                "Unable to write meta to '" + metaFile + "'", e);
        }
    }

    private void storeLookup() throws IOException {
        log.debug("Storing offsets for " + termCount + " terms from lookup "
                  + "table at " + getLookupFile());
        FileOutputStream fileOut = new FileOutputStream(getLookupFile());
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        for (int i = 0 ; i <= termCount ; i++) {
            out.writeLong(lookupTable[i]);
        }
        out.flush();
        out.close();
        fileOut.close();
    }

    private void openLookup() throws IOException {
        log.trace("openLookup called");
        if (!getLookupFile().exists()) {
            createLookup();
            return;
        }
        log.debug("Opening existing lookup file with offsets for " + termCount
                  + " terms from " + getLookupFile());
        FileInputStream fileIn = new FileInputStream(getLookupFile());
        ObjectInputStream in = new ObjectInputStream(fileIn);
        lookupTable = new long[termCount+1];
        for (int i = 0 ; i < lookupTable.length ; i++) {
            lookupTable[i] = in.readLong();
        }
        in.close();
        fileIn.close();
    }

    private void createLookup() throws IOException {
        log.debug("Creating lookup table for " + getLookupFile());
        long creationTime = -System.currentTimeMillis();
        lookupTable = new long[termCount+1];
        persistent.seek(0);
        String line;
        // Skip heading
        long previous = 0;
        while ((line = persistent.readLine()) != null) {
            if (!line.startsWith(commentPrefix)) {
                break;
            } else {
                previous = persistent.getPosition();
            }
        }
        if (line == null && !skipColumnNames) {
            throw new IOException("No column names in " + getLookupFile());
        }

        // columns
        if (!skipColumnNames) {
            columns = line.split("\t");
            log.debug("Column names: " + Strings.join(columns, ", "));
        } else {
            log.debug("Skipping loading of column names");
            persistent.seek(previous);
        }

        for (int i = 0 ; i < lookupTable.length ; i++) {
            lookupTable[i] = persistent.getPosition();
            try {
                persistent.readLine();
            } catch (IOException e) {
                throw new IOException(
                    "IOException while reading row #" + i
                    +" (starting from 0) from " + getLookupFile(), e);
            }
        }
        storeLookup();
        creationTime += System.currentTimeMillis();
        log.info("Created lookup file " + getLookupFile() + " for "
                 + getDataFile() + " in " + creationTime + " ms");
    }

    private File getLookupFile() {
        return getFile(persistent.getFile().getParentFile(), ".lookup");
    }

    private File getDataFile() {
        return getFile(persistent.getFile().getParentFile(), ".dat");
    }

    private String termToString(TermEntry value) {
        return value.toPersistent() + "\n";
    }
/*    private TermEntry stringToTerm(String content) {
        return new TermEntry(content, columns);
    }*/

    /**
     * Close down all connections to persistent files and free memory. Only
     * {@link #create} or {@link #open} must be called after close.
     * @throws java.io.IOException if the close failed.
     */
    public void close() throws IOException {
        synchronized (this) {
            if (persistent != null) {
                persistent.flush();
                persistent.close();
            }
        }
    }

    /**
     * Add the term entry to the stats. Adds must be done sequentially with the
     * terms in unicode order.
     * @param value the term and term count to add.
     */
    @Override
    public synchronized boolean add(TermEntry value) {
        synchronized (this) {
            try {
                if (persistent.getPosition() != lookupTable[termCount]) {
                    persistent.seek(lookupTable[termCount]);
                }
                if (log.isTraceEnabled()) {
                    log.trace("Writing " + value + " at offset "
                              + persistent.getPosition());
                }
//            log.debug(value);
                persistent.write(termToString(value));
                termCount++;
                lookupTable =
                    ArrayUtil.makeRoom(lookupTable, termCount, 1.2, 10000, 0);
                lookupTable[termCount] = persistent.getPosition();
                return true;
            } catch (IOException e) {
                throw new RuntimeException(
                    "Unable to add entry " + value + " to persistent layer", e);
            }
        }
    }

    /**
     * The contains takes either a TermEntry or a term (String). Given a
     * TermEntry, only termEntry.getTerm()-existence is verified.
     * @param o the object to seek.
     * @return true if the object exists.
     */
    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public int indexOf(Object o) {
        String key;
        if (o instanceof TermEntry) {
            key = ((TermEntry)o).getTerm();
        } else if (o instanceof String) {
            key = (String)o;
        } else {
            return -1;
        }
        return binarySearch(key);
    }

    // Returns -1 if not found
    private int binarySearch(String term) {
        int low = 0;
        // TODO: Test whether termCount or termCount+1 should be used
        int high = termCount-1;
        while (low <= high) {
            int middle = low + high >>> 1;
            String midVal = getLine(middle);
            final int compareResult = TermEntry.comparePersistent(term, midVal);
            if (compareResult > 0) {
                low = middle + 1;
            } else if (compareResult < 0) {
                high = middle - 1;
            } else {
                return middle;
            }
        }
        return -1;
    }

    /**
     * Finds the entry with the given term in {@code log2(n)} time.
     * The look ups are LRU cached so multiple calls for the same term in
     * short order are cheap..
     * @param term the term for the wanted entry.
     * @return the entry for the term if it exists, else null;
     */
    public TermEntry getEntry(String term) {
        TermEntry entry = cache.get(term);
        if (entry != null) {
            cache.put(term, entry);
            return entry;
        }
        int index = indexOf(term);
        if (index == -1) {
            return null;
        }
        entry = get(index);
        cache.put(term, entry);
        return entry;
    }

    @Override
    public TermEntry get(int index) {
        return new TermEntry(getLine(index), columns);
    }

    protected String getLine(int index) {
        if (index >= termCount) {
            throw new ArrayIndexOutOfBoundsException(
                "There is " + termCount + " terms in the collection, the term "
                + "at position " + index + " were requested");
        }
        synchronized (this) {
            try {
                persistent.seek(lookupTable[index]);
            } catch (IOException e) {
                throw new RuntimeException(
                    "IOException seeking to file offset " + lookupTable[index]
                    + " for index " + index + " in " + getLookupFile(), e);
            }
            try {
                return persistent.readLine();
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(String.format(
                    "Illegal argument after seeking to position %d and reading "
                    + "line for index %d in '%s'",
                    lookupTable[index], index, getDataFile()), e);
            } catch (BufferOverflowException e) {
                throw new RuntimeException(String.format(
                    "Buffer overflow after seeking to position %d and reading "
                    + "line for index %d in '%s'",
                    lookupTable[index], index, getDataFile()), e);
            } catch (IOException e) {
                throw new RuntimeException(
                    "IOException getting data for line at file offset "
                    + lookupTable[index] + " for index " + index + " in "
                    + getLookupFile(), e);
            }
        }
    }


    @Override
    public int size() {
        return termCount;
    }

    /**
     * @return the number of documents in the (potentially virtual) index that
     *         this represents.
     */
    public long getDocCount() {
        return docCount;
    }

    public void setDocCount(long docCount) {
        this.docCount = docCount;
    }

    /**
     * @return the number of terms with stats in this structure.
     */
    public long getTermCount() {
        return termCount;
    }

    /**
     * The source is meant for feedback and debugging only. No guarantees are
     * given as to how it is constructed.
     * @return the source of the data.
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setMinDocumentFrequency(long minDocumentFrequency) {
        this.minDocumentFrequency = minDocumentFrequency;
    }

    @Override
    public String toString() {
        return "TermStat(docs " + getDocCount()
               + ", source '" + getSource() + "')";
    }

    /**
     * @return the folder with the persistent term stat data.
     */
    public File getLocation() {
        return persistent.getFile().getParentFile();
    }

    private HashMap<String, TermEntry> createCache() {
        return new LinkedHashMap<String, TermEntry>(cacheSize) {
            @Override
            protected boolean removeEldestEntry(
                Map.Entry<String, TermEntry> eldest) {
                return size() >= cacheSize;
            }
        };
    }
}

