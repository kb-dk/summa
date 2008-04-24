/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.storage.filter;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.storage.io.Access;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.File;

/**
 * Retrieves Records from storage based on the criteria given in the properties.
 * Supports stopping during retrieval and resuming by timestamp.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class RecordReader implements ObjectFilter {
    private static Log log = LogFactory.getLog(RecordReader.class);

    /**
     * The Storage to connect to. This is a standard RMI address.
     * Example: //localhost:6789/storage;
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_METADATA_STORAGE =
            "summa.storage.RecordReader.MetadataStorage";

    /**
     * The state of progress is stored in this file upon close. This allows
     * for a workflow where a Storage is harvested in parts.
     * </p><p>
     * The progress file is resolved to the default dir if it is not absolute.
     * </p><p>
     * This property is optional. Default is "<base>.progress.xml",
     * for example "horizon.progress.xml".
     * If no base is defined, the default value is "progress.xml".
     */
    public static final String CONF_PROGRESS_FILE =
            "summa.storage.RecordReader.progress-file";
    public static final String DEFAULT_PROGRESS_FILE_POSTFIX = "progress.xml";

    /**
     * If true, the state of progress is stored in {@link #CONF_PROGRESS_FILE}.
     * This means that new runs will continue where the previous run left.
     * If no progress-file exists, a new one will be created.
     * </p><p>
     * This property is optional. Default is true.
     */
    public static final String CONF_USE_PERSISTENCE =
            "summa.storage.RecordReader.use-persistence";
    public static final boolean DEFAULT_USE_PERSISTENCE = true;

    /**
     * If true, any existing progress is ignored and the harvest from the
     * Storage is restarted.
     * </p><p>
     * This property is optional. Default is false.
     */
    public static final String CONF_START_FROM_SCRATCH =
            "summa.storage.RecordReader.start-from-scratch";
    public static final boolean DEFAULT_START_FROM_SCRATCH = false;

    /**
     * The maximum number of Records to read before signalling EOF onwards in
     * the filter chain. Specifying -1 means no limit.
     * </p><p>
     * This property is optional. Default is -1 (disabled).
     */
    public static final String CONF_MAX_READ_RECORDS =
            "summa.storage.RecordReader.max-read.records";
    public static final int DEFAULT_MAX_READ_RECORDS = -1;

    /**
     * The maximum number of seconds before signalling EOF onwards in the
     * filter chain. Specifying -1 means no limit.
     * </p><p>
     * This property is optional. Default is -1 (disabled).
     */
    public static final String CONF_MAX_READ_SECONDS =
            "summa.storage.RecordReader.max-read.seconds";
    public static final int DEFAULT_MAX_READ_SECONDS = -1;

    /**
     * Only Records with matching base will be retrieved. Specifying the empty
     * string as base means all bases. No wildcards are allowed.
     * </p><p>
     * This property is optional. Default is "" (all records).  
     */
    public static final String CONF_BASE =
            "summa.storage.RecordReader.base";
    public static final String DEFAULT_BASE = "";

    private Access access;
    private File progressFile;
    private boolean usePersistence = DEFAULT_USE_PERSISTENCE;
    private boolean startFromScratch = DEFAULT_START_FROM_SCRATCH;
    private int maxReadRecords = DEFAULT_MAX_READ_RECORDS;
    private int maxReadSeconds = DEFAULT_MAX_READ_SECONDS;
    private String base = DEFAULT_BASE;

    /**
     * Connects to the Storage specified in the configuration and request an
     * iteration of the Records specified by the properties.
     * @param configuration contains setup information.
     * @see {@link #CONF_BASE}.
     * @see {@link #CONF_MAX_READ_RECORDS}.
     * @see {@link #CONF_MAX_READ_SECONDS}.
     * @see {@link #CONF_METADATA_STORAGE}.
     * @see {@link #CONF_PROGRESS_FILE}.
     * @see {@link #CONF_START_FROM_SCRATCH}.
     * @see {@link #CONF_USE_PERSISTENCE}.
     */
    public RecordReader(Configuration configuration) {
        log.trace("Constructing RecordReader");
        access = FilterCommons.getAccess(configuration, CONF_METADATA_STORAGE);
        base = configuration.getString(CONF_BASE, DEFAULT_BASE);
        String progressFileString =
                configuration.getString(CONF_PROGRESS_FILE, null);
        if (progressFileString == null || "".equals(progressFileString)) {
            progressFile = new File(("".equals(base) ? "" : base + ".")
                                    + DEFAULT_PROGRESS_FILE_POSTFIX);
            log.debug("No progress-file defined in key " + CONF_PROGRESS_FILE
                      + ". Constructing progress file '" + progressFile + "'");
        }
        progressFile = progressFile.getAbsoluteFile();
        usePersistence = configuration.getBoolean(CONF_USE_PERSISTENCE,
                                                  DEFAULT_USE_PERSISTENCE);
        startFromScratch = configuration.getBoolean(CONF_START_FROM_SCRATCH,
                                                    DEFAULT_START_FROM_SCRATCH);
        maxReadRecords = configuration.getInt(CONF_MAX_READ_RECORDS,
                                              DEFAULT_MAX_READ_RECORDS);
        maxReadSeconds = configuration.getInt(CONF_MAX_READ_SECONDS,
                                              DEFAULT_MAX_READ_SECONDS);

        // TODO: Get persistent state, get iterator from access
        log.trace("RecordReader constructed, ready for pumping");
    }

    // TODO: Handle close on EOF.

    /* ObjectFilter interface */

    public boolean hasNext() {
        return false;  // TODO: Implement this
    }

    public Payload next() {
        return null;  // TODO: Implement this
    }

    public void remove() {
        throw new UnsupportedOperationException(
                "No removal of Payloads for RecordReader");
    }

    public void setSource(Filter filter) {
        throw new UnsupportedOperationException(
                "RecordReader must be the first filter in the chain");
    }

    public boolean pump() throws IOException {
        if (!hasNext()) {
             return false;
         }
         Payload next = next();
         if (next == null) {
             return false;
         }
         next.close();
         return true;
     }

    public void close(boolean success) {
        // TODO: Store progress state, close down connection
    }
}
