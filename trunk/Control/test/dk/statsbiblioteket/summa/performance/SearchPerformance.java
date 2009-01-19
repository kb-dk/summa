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
package dk.statsbiblioteket.summa.performance;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
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
     * The location of the Lucene index.
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
    public static final String DEFAULT_CONF_FILE = "SearchPerformance.cfg";

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
        File location = new File(conf.getString(CONF_INDEX_LOCATION,
                                                DEFAULT_INDEX_LOCATION));
        log.debug("dirType = " + dirType + ", readOnly = " + readOnly
                  + ", location = " + location);
        if (PARAM_DIR_FS.equals(dirType)) {
            log.debug("Creating FSDirectory(" + location + ")");
            try {
                FSDirectory dir = FSDirectory.getDirectory(location);
                return new IndexSearcher(IndexReader.open(dir, readOnly));
            } catch (IOException e) {
                throw new IOException("Unable to load index from '" + location
                                      + "'", e);
            }
        }
        if (PARAM_DIR_RAM.equals(dirType)) {
            log.debug("Creating RAMDirectory(" + location + ")");
            try {
                RAMDirectory dir = new RAMDirectory(location);
                return new IndexSearcher(IndexReader.open(dir, readOnly));
            } catch (IOException e) {
                throw new IOException("Unable to load index into RAM from '"
                                      + location + "'", e);
            }
        }
        if (PARAM_DIR_NIOFS.equals(dirType)) {
            log.debug("Creating NIODirectory(" + location + ")");
            try {
                System.setProperty("org.apache.lucene.FSDirectory.class",
                                   NIOFSDirectory.class.getName());
                FSDirectory dir = FSDirectory.getDirectory(location);
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
