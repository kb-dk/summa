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
package dk.statsbiblioteket.summa.performance;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Iterates through a list of logged queries, performing a search on each.
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
    public static final String DEFAULT_INDEX_LOCATION = "index";

    /**
     * The location of the index descriptor. The descriptor is used by the
     * query parser.
     * </p><p>
     * Optional. Default is "index/IndexDescriptor.xml".
     */
    public static final String CONF_INDEX_DESCRIPTOR =
            "summa.performance.index.descriptor";
    public static final String DEFAULT_INDEX_DESCRIPTOR =
            "index/IndexDescriptor.xml";

    /**
     * The Lucene Directory to use.
     * </p><p>
     * Valid values are FSDirectory, RAMDirectory, NIOFSDirectory.
     * </p><p>
     * Optional. Default is "FSDirectory".
     */
    public static final String CONF_DIR_TYPE = "summa.performance.dir.type";
    public static final String CONF_DIR_FS =    "FSDirectory";
    public static final String CONF_DIR_RAM =   "RAMDirectory";
    public static final String CONF_DIR_NIOFS = "NIOFSDirectory";

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
     * If true, searches are skipped and only query-parsing is performed.
     * </p><p>
     * Optional. Default is false;
     */
    public static final String CONF_SIMULATE = "summa.performance.simulate";
    public static final boolean DEFAULT_SIMULATE = false;

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
     * The maximum number of hits to extract from the result set.
     * Note that fields are also extracted as part of this.
     * </p><p>
     * Optional. Default is 20.
     */
    public static final String CONF_MAX_HITS = "summa.performance.maxhits";
    public static final int DEFAULT_MAX_HITS = 20;

    /**
     * The locations of the queries to use. One query/line.
     * </p><p>
     * Optional. Default is "queries.dat".
     */
    public static final String CONF_QUERIES_FILE
            = "summa.performance.queries.file";
    public static final String DEFAULT_QUERIES_FILE = "queries.dat";

    /**
     * The maximum number of queries to use.
     * </p><p>
     * Optional. Default is Integer.MAX_VALUE.
     */
    public static final String CONF_MAX_QUERIES =
            "summa.performance.maxqueries";
    public static final int DEFAULT_MAX_QUERIES = Integer.MAX_VALUE;
}
