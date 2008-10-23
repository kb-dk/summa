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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DanMARC2Parser extends MARCParser {
    private static Log log = LogFactory.getLog(DanMARC2Parser.class);

    // Old SB-way: 004.a and 004.z in unknown order of priority
    public static final String ID_FIELD = "001";
    public static final String ID_FIELD_SUBFIELD = "a";
    public static final String STATUS_FIELD = "004";
    public static final String STATUS_FIELD_SUBFIELD = "r";
    public static final String STATUS_DELETED = "r";

    /**
     * The id.
     */
    private String id;
    /**
     * The priority for the id. Higher priorities overrides lower priorities.
     */
    private int idPriority;
    /**
     * Whether or not this MARC record is a deletion.
     */
    private boolean isDeleted;

    public DanMARC2Parser(Configuration conf) {
        super(conf);
        log.debug("Constructed DanMARC2Parser");
        initializeNewParse();
    }

    protected void initializeNewParse() {
        id = null;
        idPriority = -1;
        isDeleted = false;
    }

    protected void setLeader(String content) {
        // Do nothing af we do not need the leader for anything in DanMARC2
        // TODO: Check whether position 8 in leader marks deleted for SB-MARC
    }

    protected void beginDataField(String tag, String ind1, String ind2) {
        // Do nothing as we extract the wanted information from the subfields
    }

    protected void endDataField(String tag) {
        // Do nothing as we don't care about entering and exiting datafields
    }

    protected void setSubField(String dataFieldTag, String dataFieldInd1,
                               String dataFieldInd2, String subFieldCode,
                               String subFieldContent) {
        if (ID_FIELD.equals(dataFieldTag) &&
            ID_FIELD_SUBFIELD.equals(subFieldCode)) {
            setID(subFieldContent, 0); // Only one way to specify ID in DanMARC2
        } else if (STATUS_FIELD.equals(dataFieldTag) &&
                   STATUS_FIELD_SUBFIELD.equals(subFieldCode)) {
            // d = deleted, c = corrected, n = new, "" = new or corrected
            log.trace("Status is '" + subFieldContent + "'");
            if (STATUS_DELETED.equals(subFieldContent)) {
                isDeleted = true;
            }
        }
        // 001.c: Last update, 001.d: Creation, both as ISO
    }

    protected Record makeRecord(String xml) {
        if (id == null) {
            log.warn("makerecord called but no ID was extracted from MARC "
                     + "record from " + sourcePayload);
            return null;
        }
        Record record;
        try {
            record = new Record(id, base, xml.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("utf-8 not supported", e);
        }
        record.setDeleted(isDeleted);
        // TODO: Add parents/childs
        return record;
    }

    /* Mutators */

    /**
     * Sets the id if it has idPriority >= the existing idPriority.
     * @param id         the id for the Record.
     * @param idPriority the priority for the id.
     */
    private void setID(String id, int idPriority) {
        if (this.idPriority <= idPriority) {
            log.trace("Setting id to '" + id + "'");
            this.id = id;
        }
    }
}
