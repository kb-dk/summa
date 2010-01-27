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
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.LineReader;
import dk.statsbiblioteket.util.reader.ReplaceReader;
import dk.statsbiblioteket.util.reader.ReplaceFactory;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.util.FactoryPool;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.*;

/**
 * Persistent structure for TermStats from an index. The structure must be
 * filled in sequential order and allows for lookups in O(log n) using binary
 * search.
 * </p><p>
 * Note: {@link #open} or {@link #create} must be called before the TermStat
 * can be updated or queried.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStat {
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
     * The size of the LRU-cache used for {@link #getTermCount(String)}.
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
     * correspond to the term length as described in
     * {@link LineReader#binaryLineSearch}.
     * </p><p>
     * Optional. Default is 400.
     */
    public static final String CONF_READER_BUFFER =
            "common.distribution.reader.buffer";
    public static final int DEFAULT_READER_BUFFER = 400;


    private LineReader persistent;
  //  private boolean memoryBased = DEFAULT_MEMORYBASED;
    private FactoryPool<TermEntry> entryPool;
    private long docCount = 0;
    private long termCount = 0;
    private String source = "No source defined";
    private int cacheSize = DEFAULT_CACHE_SIZE;
    private int bufferSize = DEFAULT_READER_BUFFER;

    /**
     * The cache facilitates faster look ups. It caches the mapping from
     * term string to term count.
     */
    private final HashMap<String, Integer> cache = createCache();

    public TermStat(Configuration conf) {
        cacheSize = conf.getInt(CONF_CACHE_SIZE, cacheSize);
        bufferSize = conf.getInt(CONF_READER_BUFFER, bufferSize);
//        memoryBased = conf.getBoolean(CONF_MEMORYBASED, memoryBased);
        //noinspection DuplicateStringLiteralInspection
//        log.debug(String.format("Constructed %s-based TermStat",
//                                memoryBased ? "memory" : "storage"));
/*        if (memoryBased) {
            persistent = new MemoryPool<TermEntry>(
                    new TermValueConverter(), null);
        } else {
            persistent = new DiskPool<TermEntry>(
                    new TermValueConverter(), null);
        }*/
        entryPool = new FactoryPool<TermEntry>() {
            @Override
            protected TermEntry createNewElement() {
                return new TermEntry("DummyTermEntry", 0); 
            }
        };
    }

    /**
     * Open the TermStats at the given location. The TermStats are always opened
     * in read-only mode. For brulding term stats, call {@link #create}.
     * @param location where the TermStats are stored.
     * @return true if the open was successful.
     * @throws IOException if the open failed due to invalid persistent data.
     */
    public boolean open(File location) throws IOException {
        if (!location.exists()) {
            throw new FileNotFoundException("Unable to locate " + location);
        }
        closeExisting();
        log.info(String.format("Opening TermStats at '%s' as readonly",
                               location));
        persistent = new LineReader(getFile(location, ".dat"), "r");
        persistent.setBufferSize(bufferSize);
        return openMeta();
    }

    private void closeExisting() throws IOException {
        if (persistent != null) {
            log.debug("Closing down existing connection to persistent data");
            persistent.close();
        }
        docCount = 0;
        termCount = 0;
        cache.clear();
        source = "No Source";
    }

    private boolean openMeta() {
        File metaFile = getFile(persistent.getFile().getParentFile(), ".meta");
        try {
            log.debug("Opening meta data from '" + metaFile + "'");
            String meta = Resolver.getUTF8Content(metaFile.toURI().toURL());
            if (log.isDebugEnabled()) {
                log.debug("Got meta data:\n" + meta);
            }
            Document dom = DOM.stringToDOM(meta, false);
            docCount = Long.parseLong(DOM.selectString(
                    dom,"termstatmeta/doccount"));
            termCount = Long.parseLong(DOM.selectString(
                    dom,"termstatmeta/termcount"));
            source = DOM.selectString(dom, "termstatmeta/source");
            log.debug(String.format(
                    "Extracted docCount %d, termCount %d and source '%s' from "
                    + "'%s'",
                    docCount, termCount, source, metaFile));
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
     * Create a persistent structure at the given location, making it ready for
     * updates.
     * </p><p>
     * It is recommended to call {@link #setSource(String)} after this.
     * @param location where the TermStats should be stored.
     * @return true if the creation succeded.
     * @throws IOException if the structure could not be created.
     */
    public boolean create(File location) throws IOException {
        closeExisting();
        if (!location.exists()) {
            if (!location.mkdirs()) {
                throw new IOException(String.format(
                        "Unable to create the folder '%s'", location));
            }
        }
        persistent = new LineReader(getFile(location, ".dat"), "rw");
        persistent.setBufferSize(bufferSize);
        return true;
    }

    /**
     * Update the persistent data for the term stat representation. This uses
     * the location given in {@link #open} or {@link #create}.
     * Note that this does not close the connection to the persistent data.
     * @throws IOException if the data could not be stored.
     */
    public void store() throws IOException {
        persistent.flush();
        storeMeta();
    }

    private void storeMeta() throws IOException {
        File metaFile = getFile(persistent.getFile().getParentFile(), ".meta");
        log.debug("StoreMeta called, storing in '" + metaFile + "'");
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
            //noinspection DuplicateStringLiteralInspection
            xmlOut.writeStartElement("source");
            xmlOut.writeCharacters(getSource());
            xmlOut.writeEndElement();
            xmlOut.writeEndDocument();
            xmlOut.close();
            fileOut.close();
            log.debug("StoreMeta completed for '" + metaFile + "'");
        } catch (Exception e) {
            throw new IOException("Unable to write meta to '" + metaFile + "'",
                                  e);
        }
    }

    private static String DELIMITER = " ";
    private static final ReplaceReader newlineEscaper;
    private static final ReplaceReader newlineUnEscaper;
    static {
        Map<String, String> escape = new LinkedHashMap<String, String>(10);
        escape.put("\\", "\\\\");
        escape.put("\n", "\\n");
        newlineEscaper = ReplaceFactory.getReplacer(escape);

        Map<String, String> unEscape = new LinkedHashMap<String, String>(10);
        escape.put("\\n", "\\n");
        escape.put("\\\\", "\\");
        newlineUnEscaper = ReplaceFactory.getReplacer(unEscape);

    }
    private String termToString(TermEntry value) {
        return newlineEscaper.transform(value.getTerm()) + DELIMITER
               + Integer.toString(value.getCount()) + "\n";
    }
    private TermEntry stringToTerm(String content) {
        int pos = content.lastIndexOf(DELIMITER);
        // No explicit check for existence of delimiter as we don't care whether
        // a "No DELIMITER found or an ArrayIndexOutOfBounds is thrown
        // The same goes for the Integer.parseInt-call. In all three cases, we
        // can infer what happened.
        return this.entryPool.get().
                setTerm(newlineUnEscaper.transform(content.substring(0, pos))).
                setCount(Integer.parseInt(
                        content.substring(pos + 1, content.length())));
    }

    private String extractTermStringWithEscapes(String line) {
        // No explicit check for existence of delimiter as we don't care whether
        // a "No DELIMITER found or an ArrayIndexOutOfBounds is thrown
        // The same goes for the Integer.parseInt-call. In all three cases, we
        // can infer what happened.
        return line.substring(0, line.lastIndexOf(DELIMITER));
    }
    private int extractTermCount(String line) {
        int pos = line.lastIndexOf(DELIMITER);
        return Integer.parseInt(line.substring(pos + 1, line.length()));
    }

    /**
     * Close down all connections to persistent files and free memory. Only
     * {@link #create} or {@link #open} must be called after close.
     * @throws java.io.IOException if the close failed.
     */
    public void close() throws IOException {
        if (persistent != null) {
            persistent.flush();
            persistent.close();
        }
    }

    /**
     * Add the term entry to the stats. Adds must be done sequentially with the
     * terms in unicode order.
     * @param value the term and term count to add.
     * @throws java.io.IOException in case of errors with the persistence layer.
     */
    public void add(TermEntry value) throws IOException {
        persistent.write(termToString(value));
        termCount++;
    }

    /**
     * Locate the given value in the Structure. Note that this matches on the
     * String part of a previously added term entry.
     * @param termString the String to locate
     * @return the position of the given value in the structure or -1 if it does
     *         not exist.
     * @throws java.io.IOException in case of IO-errors with persistent data.
     */
    private long indexOf(String termString) throws IOException {
        return persistent.binaryLineSearch(
                termLocator, newlineEscaper.transform(termString));
    }
    private Comparator<String> termLocator = new Comparator<String>() {
        // o1 will always be the search value, o2 is the line
        public int compare(String o1, String o2) {
            return o1.compareTo(extractTermStringWithEscapes(o2));
        }
    };

    /**
     * Returns the number of times the term occur.
     * @param term the (@code field:value} to resolve the count for.
     * @return the count for the given term or -1 if the term does not exist.
     * @throws java.io.IOException in case of IO-errors with persistent data.
     */
    public int getTermCount(String term) throws IOException {
        Integer tc = cache.get(term);
        if (tc != null) {
            return tc;
        }
        long pos = indexOf(term);
        if (pos < 0) {
            tc = -1;
        } else {
            persistent.seek(pos);
            tc = extractTermCount(persistent.readLine());
        }
        cache.put(term, tc);
        return tc;
    }

    /**
     * Set the position to the first TermEntry.
     * @throws java.io.IOException in case of IO-errors with persistent data.
     */
    public void reset() throws IOException {
        persistent.seek(0);
    }

    /**
     * @return true if there are more entries.
     */
    public boolean hasNext() {
        return !persistent.eof();
    }

    /**
     * @return the TermEntry at the current position, without advancing to the
     *         next TermEntry. If null is returned, there are no more entries.
     * @throws java.io.IOException in case of IO-errors with persistent data.
     */
    public TermEntry peek() throws IOException {
        return read(true);
    }

    /**
     * @return the TermEntry at the current position, advancing the pointer to
     *         the next TermEntry. If null is returned, there are no more
     *         entries.
     * @throws java.io.IOException in case of IO-errors with persistent data.
     */
    public TermEntry get() throws IOException {
        return read(false);
    }

    private TermEntry read(boolean resetPos) throws IOException {
        if (!hasNext()) {
            return null;
        }
        long startPos = persistent.getPosition();
        String line = persistent.readLine();
        if (resetPos) {
            persistent.seek(startPos);
        }
        if (line == null || "".equals(line)) {
            return null;
        }
        return stringToTerm(line);
    }


    /**
     * @return the number of documents in the (potentially virtual) index that
     *         this represents.
     */
    public long getDocCount() {
        return docCount;
    }

    /**
     * @param docCount the number of documents in the index.
     */
    public void setDocCount(long docCount) {
        this.docCount = docCount;
    }

    /**
     * @return the number of terms with counts in this structure.
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

    /**
     * @param source the source for these data.
     */
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "TermStat(docs " + getDocCount()
               + ", source '" + getSource() + ")";
    }

    /**
     * @return the folder with the persistent term stat data.
     */
    public File getLocation() {
        return persistent.getFile().getParentFile();
    }

    private HashMap<String, Integer> createCache() {
        return new LinkedHashMap<String, Integer>(cacheSize) {
            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<String, Integer> eldest) {
                return size() >= cacheSize;
            }
        };
    }

}

