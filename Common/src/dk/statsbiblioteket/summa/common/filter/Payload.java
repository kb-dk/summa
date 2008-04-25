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
package dk.statsbiblioteket.summa.common.filter;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The payload is the object that gets pumped through a filter chain.
 * It is an explicit wrapper for Stream and Record. Normally a Lucene Document
 * will be added somewhere in the process from input to index.
 * </p><p>
 * The payload provides a String=>Object map for extra data, such as a Document.
 * </p><p>
 * Note: This is not the same map as the one provided by Record.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Payload {
    private static Log log = LogFactory.getLog(Payload.class);

    /**
     * The key for storing a Lucene Document in data.
     */
    // TODO: Move this to a more fitting class
    public static final String LUCENE_DOCUMENT = "luceneDocument";

    /**
     * The key for storing a SearchDescriptor in data.
     */
    // TODO: Move this to a more fitting class
    public static final String SEARCH_DESCRIPTOR = "searchDescriptor";

    private InputStream stream = null;
    private Record record = null;

    /**
     * Data for the Payload, such as stream-origin. Used for filter-specific
     * data. The map can be accessed by {@link #getData}.
     */
    private Map<String, Object> data;


    /* Constructors */

    public Payload(InputStream stream) {
        assignIfValid(stream, record);
    }
    public Payload(Record record) {
        assignIfValid(stream, record);
    }
    public Payload(InputStream stream, Record record) {
        assignIfValid(stream, record);
    }

    /* Accessors */

    public InputStream getStream() {
        return stream;
    }
    public Record getRecord() {
        return record;
    }

    /**
     * There is always a data-map for each Payload, but it is created lazily if
     * it has no values. Use {@link #getData(String)} for fast look-up of values
     * where it is expected that the map is empty, as it will never create a
     * new map.
     * @return the data for this Payload.
     */
    public Map<String, Object> getData() {
        if (data == null) {
            data = new HashMap<String, Object>(10);
        }
        return data;
    }
    /**
     * Request a data-value for the given key. This method is more efficient
     * than requesting the full map with {@link #getData()}, as it never creates
     * a new map.
     * @param key the key for the value.
     * @return the value for the key, or null if the key is not in the map.
     */
    public Object getData(String key) {
        return data == null ? null : data.get(key);
    }

    /**
     * Convenience method for extracting Strings from data.
     * @param key the data to get.
     * @return the value for the key if it exists and is a String, else null.
     */
    public String getStringData(String key) {
        Object object = getData(key);
        if (object == null || !(object instanceof String)) {
            return null;
        }
        return (String)object;
    }

    /**
     * @return true if a data-map has been created. Used for time/space
     *         optimization.
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Extracts an id from the Payload, if possible. The order of priority for
     * extracting the ID is as follows:
     * 1. If the data contains IndexUtils#RECORD_FIELD, the value is used.
     * 2. If a Record is defined, its ID is used.
     * 3. If none of the above is present, null is returned.
     * @return the id for the Payload if present, else null.
     */
    public String getId() {
        try {
            String id = getStringData(IndexUtils.RECORD_FIELD);
            if (id != null) {
                return id;
            }
            if (getRecord() != null) {
                return getRecord().getId();
            }
        } catch (Exception e) {
            log.error("Exception extracting ID from payload '"
                      + super.toString() + "'. Returning null", e);
            return null;
        }
        log.debug("Could not extract ID for payload '"
                  + super.toString() + "'");
        return null;
    }

    /**
     * Store the given id as RecordID in data and Record, if possible.
     * This method is forgiving: It can be called multiple times and tries to
     * correct any inconsistencies.
     * @param id the id to assign to the Payload.
     */
    public void setID(String id) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("setID(" + id + ") called");
        if (id == null || "".equals(id)) {
            throw new IllegalArgumentException("The id must be defined");
        }
        getData().put(IndexUtils.RECORD_FIELD, id);
        if (getRecord() != null) {
            getRecord().setId(id);
        }
    }

    /* Mutators */

    public void setStream(InputStream stream) {
        assignIfValid(stream, record);
    }
    public void setRecord(Record record) {
        assignIfValid(stream, record);
    }

    /**
     * Helper for assignment that ensures that at least one of the core
     * attributes is != null.
     * @param stream   the stream to assign.
     * @param record   the record to assign.
     */
    private void assignIfValid(InputStream stream, Record record) {
        if (stream == null && record == null) {
            throw new IllegalStateException("Either stream or record "
                                            + "must be defined");
        }
        log.debug("Assigned stream: " + stream + " and record: " + record
                  + " to Payload");
        this.stream = stream;
        this.record = record;
    }

    /**
     * Close any open resources held by this Payload. This translates to closing
     * the underlying stream, if present. Close should be called by any
     * end-point for a payload-resource along the filter-chain. The obvious
     * example being a stream => record converter which should call close when
     * the underlying stream is empty.
     * </p><p>
     * Note: Calling close on the payload is not a substitute for calling
     *       close(success) on the Filter.
     */
    public void close() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                log.error("Exception closing stream", e);
            }
        }
    }

    /**
     * Pumps the underlying stream for one byte, if a stream is present.
     * @return true if there is potentially more data available.
     * @throws IOException in case of read errors.
     */
    public boolean pump() throws IOException {
        boolean cont = stream != null && stream.read() != -1;
        if (!cont) {
            close();
        }
        return cont;
    }

    /**
     * The clone-method is a shallow cloning, which means that all fields are
     * copied directly.
     * @return a shallow copy of this object.
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException",
                       "CloneDoesntCallSuperClone"})
    public Payload clone() {
        Payload clone = new Payload(getStream(), getRecord());
        clone.data = data;
        return clone;
    }

    public String toString() {
        return "Payload(" + getId() + ")"; 
    }
}
