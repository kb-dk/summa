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
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.index.IndexReader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Sets up a simulated environment for testing raw search-performance.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchPerformance {
    private static Log log = LogFactory.getLog(SearchPerformance.class);

    /**
     * The location of the Lucene index, relative to the current path.
     * A "segments.gen"-file must be present in the folder.
     * </p><p>
     * Optional. Default is "index".
     */
    public static final String CONF_INDEX_LOCATION =
            "summa.performance.index.location";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String DEFAULT_INDEX_LOCATION =
            "data/performance/index";

    /**
     * The Lucene Directory to use.
     * </p><p>
     * Valid values are FSDirectory, RAMDirectory, NIOFSDirectory.
     * </p><p>
     * Optional. Default is "FSDirectory".
     */
    public static final String CONF_DIR_TYPE = "summa.performance.dir.type";
    public static final String PARAM_DIR_FS =    "FSDirectory";
    public static final String PARAM_DIR_RAM =   "RAMDirectory";
    public static final String PARAM_DIR_NIOFS = "NIOFSDirectory";
    public static final String DEFAULT_DIR_TYPE = PARAM_DIR_FS;

    /**
     * If true, the Lucene directory is opened in read-only mode
     * (new in Lucene 2.4).
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_DIR_READONLY =
            "summa.performance.dir.readonly";
    public static final boolean DEFAULT_DIR_READONLY = true;

    /**
     * The number of threads used for searching.
     * </p><p>
     * Optional. Default is 1.
     */
    public static final String CONF_THREADS = "summa.performance.threads";
    public static final int DEFAULT_THREADS = 1;

    /**
     * If true, one reader is shared among the threads. If false, one reader is
     * opened for each thread.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_SHARED = "summa.performance.sharedreader";
    public static final boolean DEFAULT_SHARED = true;

    /**
     * The default setup-file, containing a {@link Configuration}-compatible
     * XML structure.
     */
    public static final String DEFAULT_CONF_FILE = "SearchPerformance.xml";

    private Configuration conf;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            log.debug("Using default configuration " + DEFAULT_CONF_FILE);
            new SearchPerformance(Configuration.load(DEFAULT_CONF_FILE));
        } else if ("-h".equals(args[0])) {
            System.out.println("Usage: SearchPerformance <configurationFile>");
        } else {
            log.debug("Using configuration " + args[0]);
            new SearchPerformance(Configuration.load(args[0]));
        }
    }

    public SearchPerformance(Configuration conf) throws IOException {
        this.conf = conf;
        boolean shared = conf.getBoolean(CONF_SHARED, DEFAULT_SHARED);
        int threadCount = conf.getInt(CONF_THREADS, DEFAULT_THREADS);

        SearchPerformanceMediator mediator = 
                new SearchPerformanceMediator(conf, threadCount, shared);
        List<SearchPerformanceThread> threads =
                new ArrayList<SearchPerformanceThread>(threadCount);
        for (int i = 0 ; i < threadCount; i++) {
            threads.add(new SearchPerformanceThread(mediator, getSearcher()));
        }
        log.info(String.format("Constructed %d threads. Starting run",
                               threads.size()));
        mediator.start();
        for (SearchPerformanceThread thread: threads) {
            thread.start();
        }
        log.debug("Waiting for threads to finish");
        for (SearchPerformanceThread thread: threads) {
            try {
                synchronized(thread) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                System.err.println("Exception waiting for performance thread");
                throw new IllegalStateException(
                        "Cannot finish with exception from thread", e);
            }
        }
        mediator.stop();
    }

    private IndexSearcher searcher = null;
    /**
     * If {@link #CONF_SHARED} is true, this method works as a Singleton.
     * If false, the method returns a new IndexSearcher for each call.
     * @return an IndexSearcher as specified in the properties.
     * @throws java.io.IOException if the index could not be opened.
     */
    private IndexSearcher getSearcher() throws IOException {
        if (searcher != null && conf.getBoolean(CONF_SHARED, DEFAULT_SHARED)) {
            log.trace("Re-using searcher");
            return searcher;
        }
        String dirType = conf.getString(CONF_DIR_TYPE, DEFAULT_DIR_TYPE);
        boolean readOnly = conf.getBoolean(CONF_DIR_READONLY,
                                           DEFAULT_DIR_READONLY);
        String locStr = conf.getString(CONF_INDEX_LOCATION,
                                       DEFAULT_INDEX_LOCATION);
        File location= Resolver.getFile(locStr // Lucene 2.4
                                        + (locStr.endsWith("/") ? "" : "/")
                                        + "segments.gen");
        if (location == null) { // pre-2.4
            location= Resolver.getFile(locStr
                                       + (locStr.endsWith("/") ? "" : "/")
                                       + "segments");
        }
        location = location == null ? null : location.getParentFile();
        log.debug("dirType = " + dirType + ", readOnly = " + readOnly
                  + ", location = " + location);
        if (location == null) {
            throw new IllegalArgumentException(String.format(
                    "Unable to resolve '%s' to concrete index location",
                    locStr));
        }
        if (PARAM_DIR_FS.equals(dirType)) {
            log.debug("Creating FSDirectory(" + location + ")");
            try {
                //noinspection DuplicateStringLiteralInspection
                System.setProperty("org.apache.lucene.FSDirectory.class",
                                   FSDirectory.class.getName());
                FSDirectory dir = new NIOFSDirectory(location);
                return new IndexSearcher(IndexReader.open(dir, readOnly));
            } catch (IOException e) {
                throw new IOException("Unable to load index from '" + location
                                      + "'", e);
            }
        }
        if (PARAM_DIR_RAM.equals(dirType)) {
            log.debug("Creating RAMDirectory(" + location + ")");
            try {
                RAMDirectory dir = new RAMDirectory(new NIOFSDirectory(location));
                return new IndexSearcher(IndexReader.open(dir, readOnly));
            } catch (IOException e) {
                throw new IOException("Unable to load index into RAM from '"
                                      + location + "'", e);
            }
        }
        if (PARAM_DIR_NIOFS.equals(dirType)) {
            log.debug("Creating NIODirectory(" + location + ")");
            try {
                //noinspection DuplicateStringLiteralInspection
                System.setProperty("org.apache.lucene.FSDirectory.class",
                                   NIOFSDirectory.class.getName());
                FSDirectory dir = new NIOFSDirectory(location);
                return new IndexSearcher(IndexReader.open(dir, readOnly));
            } catch (IOException e) {
                throw new IOException("Unable to load index with NIO from '"
                                      + location + "'", e);
            }
        }
        throw new IllegalArgumentException(
                "The Directory type '" + dirType + "' for key '" + CONF_DIR_TYPE
                + "' is unknown");
    }
}

