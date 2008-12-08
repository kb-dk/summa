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
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;

/**
 * A parser for the MARC-variant used at the State and University Library of
 * Denmark (SB). The variant is a true subset of the danMARC2 standard.
 * </p><p>
 * The parser extracts the following properties from the MARC records:
 * <ul>
 *   <li>recordID<br />
 *       Taken from field 001, subfield a or field 994, subfield z.<br />
 *       994*z (specific for SB) has precedence over 001*a (danMARC2 standard).
 *   </li>
 *   <li>state (deleted or not)<br />
 *       Taken from field 004, subfield r.<br />
 *       d=deleted, c=corrected, n=new, ""=new or corrected</li>
 *   <li>parent<br />
 *       Taken from field 014, subfield a.<br />
 *       014 *x is logged with a warning, but otherwise ignored</li>
 *   <li>children (repeated)<br />
 *       Taken from field 015, subfield a.<br />
 *       015 *x is logged with a warning, but otherwise ignored<br />
 *       If 015 *v is specified, the content of the field is used for sorting
 *       the content of 015 *a by unicode order.<br />
 *       If there is a mix of 015 fields with and without subfield v, the fields
 *       without v are positioned in order of appearance before the fields with
 *       v (which are sorted as stated above).
 * </li>
 * </ul>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te",
        comment = "Stable Summa uses 245*g as fallback for sorting children. "
                  + "dl stated that this led to more trouble than good things, "
                  + "so it has been deprecated")
public class SBMARCParser extends MARCParser {
    private static Log log = LogFactory.getLog(SBMARCParser.class);

    public static final String ID_FIELD = "001";
    public static final String ID_FIELD_SUBFIELD = "a";
    public static final String SBID_FIELD = "994";
    public static final String SBID_FIELD_SUBFIELD = "z";

    public static final String STATUS_FIELD = "004";
    public static final String STATUS_FIELD_SUBFIELD = "r";
    public static final String STATUS_DELETED = "d";

    public static final String PARENT_FIELD = "014";
    public static final String PARENT_FIELD_SUBFIELD_ID = "a";
    public static final String PARENT_FIELD_SUBFIELD_SBID = "z";

    public static final String CHILD_FIELD = "015";
    public static final String CHILD_FIELD_SUBFIELD_ID = "a";
    public static final String CHILD_FIELD_SUBFIELD_SBID = "z";
    public static final String CHILD_FIELD_SUBFIELD_SORT = "v";

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
    /**
     * The parent for the MARC record. This is often null.
     */
    private String parent;
    /**
     * The children for the MARC record. This is often empty.
     */
    private List<Child> children = new ArrayList<Child>(20);
    private String lastChildID;
    private String lastChildSort;

    public SBMARCParser(Configuration conf) {
        super(conf);
        log.debug("Constructed SBMARCParser");
        initializeNewParse();
    }

    /**
     * A Child holds an ID and potentially a sortField for the ID in
     * correspondence with the sorting rules stated in the docs for the class.
     */
    public class Child implements Comparable<Child> {
        String id;
        String sortField;

        public Child(String id, String sortField) {
            this.id = id;
            this.sortField = sortField;
        }

        public int compareTo(Child other) {
            if (this.sortField == null) {
                return other.sortField == null ? 0 : -1;
            }
            return other.sortField == null ? 1 :
                   sortField.compareTo(other.sortField);
        }
    }

    protected void initializeNewParse() {
        id = null;
        idPriority = -1;
        isDeleted = false;
        parent = null;
        children.clear();
        lastChildID = null;
        lastChildSort = null;
    }

    protected void setLeader(String content) {
        // Do nothing af we do not need the leader for anything in SB-MARC
    }

    protected void beginDataField(String tag, String ind1, String ind2) {
        // Do nothing as we extract the wanted information from the subfields
    }


    protected void endDataField(String tag) {
        // If child id and potentially sortField was received, add a child
        if (lastChildID != null) {
            if (!CHILD_FIELD.equals(tag)) {
                log.warn(String.format(
                        "Sanity check failed: Collected child id '%s' for MARC"
                        + " record %s in %s but received endDataField-event for"
                        + " tag '%s'. The expected tag is '%s'",
                        lastChildID, id, sourcePayload, tag, CHILD_FIELD));
            } else {
                children.add(new Child(lastChildID, lastChildSort));
            }
        }
        lastChildID = null;
        lastChildSort = null;
    }

    protected void setSubField(String dataFieldTag, String dataFieldInd1,
                               String dataFieldInd2, String subFieldCode,
                               String subFieldContent) {
        // ID
        if (ID_FIELD.equals(dataFieldTag) &&
            ID_FIELD_SUBFIELD.equals(subFieldCode)) {
            setID(expandID(subFieldContent), 0); // Lowest priority
            return;
        }
        if (SBID_FIELD.equals(dataFieldTag) &&
            SBID_FIELD_SUBFIELD.equals(subFieldCode)) {
            setID(expandID(subFieldContent), 1); // Highest priority
            return;
        }

        // Status
//        System.out.println(dataFieldTag);
        if (STATUS_FIELD.equals(dataFieldTag) &&
            STATUS_FIELD_SUBFIELD.equals(subFieldCode)) {
            // d = deleted, c = corrected, n = new, "" = new or corrected
            log.trace("Status for " + id + " is '" + subFieldContent + "'");
            if (STATUS_DELETED.equals(subFieldContent)) {
                isDeleted = true;
            }
            return;
        }

        // Parent
        if (PARENT_FIELD.equals(dataFieldTag)) {
            if (PARENT_FIELD_SUBFIELD_ID.equals(subFieldCode) ||
                PARENT_FIELD_SUBFIELD_SBID.equals(subFieldCode) ) {
                parent = expandID(subFieldContent);
                log.trace("Parent for " + id + " is " + parent);
            } else {
                unsupported(dataFieldTag, subFieldCode);
            }
            return;
        }

        // Children
        if (CHILD_FIELD.equals(dataFieldTag)) {
            if (CHILD_FIELD_SUBFIELD_ID.equals(subFieldCode) ||
                CHILD_FIELD_SUBFIELD_SBID.equals(subFieldCode)) {
                lastChildID = expandID(subFieldContent);
            } else if (CHILD_FIELD_SUBFIELD_SORT.equals(subFieldCode)) {
                lastChildSort = subFieldContent;
            } else {
                unsupported(dataFieldTag, subFieldCode);
            }
        }
        // 001.c: Last update, 001.d: Creation, both as ISO
    }

    /**
     * The SB-MARC format defines parent/child ids in subfield z instead of a,
     * to this is changed to proper danMARC2 as part of the XML generation.
     * @param reader the XML reader at a subfield.
     * @param tag  the tag for the datafield.
     * @param ind1 ind1 for the datafield.
     * @param ind2 ind2 for the datafield.
     * @param code the subfield code.
     * @return the XML for the begin subfield tag.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected String beginSubFieldTagToString(XMLStreamReader reader,
                                              String tag, String ind1,
                                              String ind2, String code) {
        String replaceSrcCode;
        String replaceDestCode;
        if (PARENT_FIELD.equals(tag) &&
            PARENT_FIELD_SUBFIELD_SBID.equals(code)) {
            replaceSrcCode = PARENT_FIELD_SUBFIELD_SBID;
            replaceDestCode = PARENT_FIELD_SUBFIELD_ID;
        } else if (CHILD_FIELD.equals(tag) &&
                   CHILD_FIELD_SUBFIELD_SBID.equals(code)) {
            replaceSrcCode = CHILD_FIELD_SUBFIELD_SBID;
            replaceDestCode = CHILD_FIELD_SUBFIELD_ID;
        } else {
            return super.beginSubFieldTagToString(
                    reader, tag, ind1, ind2, code);
        }

        StringWriter xml = new StringWriter(50);
        xml.append("<").append(reader.getLocalName());
        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
            String attributeLocalName =
                    replaceSrcCode.equals(reader.getAttributeLocalName(i)) ?
                    replaceDestCode : reader.getAttributeLocalName(i); 
            addAttribute(xml, attributeLocalName, reader.getAttributeValue(i));
        }
        xml.append(">");
        return xml.toString();
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
        log.trace("Setting deleted-status for Record " + id + " to "
                  + isDeleted);
        record.setDeleted(isDeleted);
        if (parent != null) {
            record.setParentIds(Arrays.asList(parent));
        }

        if (children.size() != 0) {
            Collections.sort(children);
            ArrayList<String> childrenIDs =
                    new ArrayList<String>(children.size());
            for (Child child: children) {
                childrenIDs.add(child.id);
            }
            record.setChildIds(childrenIDs);
        }
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

    private void unsupported(String dataFieldTag, String subFieldCode) {
        log.warn(String.format(
                "Received unsupported subfield code '%s' in field %s "
                + "for record '%s' in %s. Ignoring subfield",
                subFieldCode, dataFieldTag, id, sourcePayload));
    }
}
