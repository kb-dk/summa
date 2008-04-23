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
import dk.statsbiblioteket.summa.storage.io.Access;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

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
     * This property id optional. Default is "progress.xml".
     */
    public static final String CONF_PROGRESS_FILE =
            "summa.storage.RecordReader.MetadataStorage";
    public static final String DEFAULT_PROGRESS_FILE = "progress.xml";

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

    /**
     * Connects to the Storage specified in the configuration, but delays the
     * extraction of Records until the first call to {@link #hasNext()},
     * {@link #next()} or {@link #pump()}
     * @param configuration contains setup information.
     * @see {@link #CONF_BASE}.
     * @see {@link #CONF_MAX_READ_RECORDS}.
     * @see {@link #CONF_MAX_READ_SECONDS}.
     * @see {@link #CONF_METADATA_STORAGE}.
     * @see {@link #CONF_PROGRESS_FILE}.
     */
    public RecordReader(Configuration configuration) {
        log.trace("Constructing RecordReader");
        access = FilterCommons.getAccess(configuration, CONF_METADATA_STORAGE);
        // TODO: Extract properties for RecordReader
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
