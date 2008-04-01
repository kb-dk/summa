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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.util.StringMap;
import org.apache.lucene.document.Document;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The payload is the object that gets pumped through a filter chain.
 * It is a wrapper for Stream, Record and Document. Only one of these have
 * to be present.
 * </p><p>
 * The payload also provides a String=>String map for meta-information.
 * Note: This is not the same map as the one provided by Record.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Payload {
    private static Log log = LogFactory.getLog(Payload.class);

    private InputStream stream = null;
    private Record record = null;
    private Document document = null;

    /**
     * Meta-data for the Payload, such as stream-origin. Used for filter-
     * specific data. The map can be accessed by {@link#getMeta}. It does not
     * permit null - neither as key, nor value.
     */
    private StringMap meta;

    /* Constructors */

    public Payload(InputStream stream) {
        assignIfValid(stream, record, document);
    }
    public Payload(Record record) {
        assignIfValid(stream, record, document);
    }
    public Payload(Document document) {
        assignIfValid(stream, record, document);
    }
    public Payload(Record record, Document document) {
        assignIfValid(stream, record, document);
    }
    public Payload(InputStream stream, Record record, Document document) {
        assignIfValid(stream, record, document);
    }

    /* Accessors */

    public InputStream getStream() {
        return stream;
    }
    public Record getRecord() {
        return record;
    }
    public Document getDocument() {
        return document;
    }

    /**
     * There is always a meta-map for each Payload, but it is created lazily if
     * it has no values. Use {@link #getMeta(String)} for fast look-up of values
     * where it is expected that the map is empty, as it will never create a
     * new map.
     * @return the meta-map for this Payload.
     */
    public StringMap getMeta() {
        if (meta == null) {
            meta = new StringMap(10);
        }
        return meta;
    }
    /**
     * Request a meta-value for the given key. This method is more efficient
     * than requesting the full map with {@link#getMeta()}, as it never creates
     * a new map.
     * @param key the key for the value.
     * @return the value for the key, or null if the key is not in the map.
     */
    public String getMeta(String key) {
        return meta == null ? null :meta.get(key);
    }
    /**
     * @return true if a meta-map has been created. Used for time/space
     *         optimization.
     */
    public boolean hasMeta() {
        return meta != null;
    }


    /* Mutators */

    public void setStream(InputStream stream) {
        assignIfValid(stream, record, document);
    }
    public void setRecord(Record record) {
        assignIfValid(stream, record, document);
    }
    public void setDocument(Document document) {
        assignIfValid(stream, record, document);
    }

    /**
     * Helper for assignment that ensures that at least one of the core
     * attributes is != null.
     * @param stream   the stream to assign.
     * @param record   the record to assign.
     * @param document the document to assign.
     */
    private void assignIfValid(InputStream stream, Record record,
                        Document document) {
        if (stream == null && record == null && document == null) {
            throw new IllegalStateException("Either stream, record or "
                                            + "document must be defined");
        }
        log.debug("Assigned stream: " + stream + ", record: " + record
                  + " and document: " + document + " to Payload");
        this.stream = stream;
        this.record = record;
        this.document = document;
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
}
