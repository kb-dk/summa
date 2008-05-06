/* $Id: IndexUtils.java,v 1.4 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:19 $
 * $Author: te $
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
package dk.statsbiblioteket.summa.common.lucene.index;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import javax.xml.XMLConstants;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Logs;

/**
 * IndexUtils is a set of static common used methods used for manipulating or
 * building the index.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class IndexUtils {

    private static Log log = LogFactory.getLog(IndexUtils.class);
    /**
     * The field-name for the Lucene Field containing the Record ID for the
     * Document. All Document in Summa Lucene Indexes must have one and only
     * one RecordID stored and indexed.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String RECORD_FIELD = "recordID";

    /**
     * Stores the given ID in the Field RECORD_FIELD in the given Document.
     * If the Field already exists, it is overwritten.
     * @param id       the ID to store in the document.
     * @param document where the ID is stored.
     */
    @QAInfo(level = QAInfo.Level.NORMAL,
            state = QAInfo.State.QA_NEEDED,
            author = "te")
    public static void assignID(String id, Document document) {
        log.trace("assignID(" + id + ", ...) called");
        String[] ids = document.getValues(RECORD_FIELD);
        if (ids != null && ids.length == 1 && ids[0].equals(id)) {
            return;
        }
        if (ids == null || ids.length == 0) {
            log.trace("setId: Adding id '" + id + "' to Document");
        } else {
            if (id.length() == 1) {
                if (ids[0].equals(id)) {
                    return;
                }
                log.debug("Old Document id was '" + ids[0]
                          + "'. Assigning new id '" + id + "'");
            } else {
                Logs.log(log, Logs.Level.WARN,
                         "Document contains multiple RecordIDs. Clearing "
                         + "old ids and assigning id '" + id
                         + "'. Old ids:", (Object)ids);
            }
            document.removeFields(RECORD_FIELD);
        }
        document.add(new Field(RECORD_FIELD, id,
                               Field.Store.YES,
                               Field.Index.UN_TOKENIZED));
    }

    /**
     * Extracts the ID from a Document, if it is present. In case of multiple
     * ID's, the first one is returned. The ID is stored in the field
     * RECORD_FIELD.
     * @param document where to extract the ID.
     * @return the ID for the Document if present, else null.
     */
    @QAInfo(level = QAInfo.Level.NORMAL,
            state = QAInfo.State.QA_NEEDED,
            author = "te")
    public String getID(Document document) {
        String[] ids = document.getValues(RECORD_FIELD);
        if (ids != null && ids.length > 0) {
            if (ids.length > 1) {
                Logs.log(log, Logs.Level.WARN, "Multiple RecordIDs defined "
                                               + "in Document '"
                                               + this + "'. Returning first"
                                               + " RecordID out of: ",
                         (Object)ids);
            }
            return ids[0];
        }
        log.trace("getID: No ID found in document");
        return null;
    }

    /**
     * Used for extracting fieldType information out of a DOM node.<br>
     * The node has to be an Attribute node and have a localname that equals
     * "type".
     *
     * @param n              the node to examine.
     * @param defaultType    the default fieldType (will be returned if the node
     *                       does not contain type info).
     * @return the fieldType found or the default FieldType if nothong found.
     * @throws IndexServiceException will be thrown if the Node is of wrong type
     *                               (not an Atribute) or has the wrong name
     *                               (not type).
     */
    public static FieldType getType(Node n, FieldType defaultType) throws
                                                         IndexServiceException {
        if (!"type".equals(n.getLocalName()) ||
            Node.ATTRIBUTE_NODE != n.getNodeType()){
            throw new IndexServiceException(
                    new IllegalArgumentException("Node has to be the type "
                                                 + "attribute"));
        }
        FieldType t =  FieldType.getType(n.getNodeValue().toLowerCase().trim());

        return t != null ? t : defaultType;
    }

    /**
     * Extracts information from a Node (from the Summa index-format) and create
     * an IndexField from that node.<br>
     * The Node has to be a Field Node from the SummaIndexDefinition xml.
     *  
     * @param n   The Node to extract xml from
     * @param defaults  predefined defaults, defined fro the index
     * @param group_name  the name of the group the field should belong to.
     * @param NAMESPACE the SummaIndexDefinition namespace URI.
     * @return  a IndexField
     * @throws IndexServiceException
     */
    public static OldIndexField makeField(Node n, IndexDefaults defaults,
                                       String group_name,
                                       String NAMESPACE) throws
                                                         IndexServiceException {


        final NamedNodeMap fieldAtts = n.getAttributes();
            OldIndexField f = new OldIndexField(defaults);
            if (group_name != null){
                f.setGroup(group_name);
            }

            for (int k = 0; k < fieldAtts.getLength(); k++) {
                final Node fieldAtt = fieldAtts.item(k);
                final String localName = fieldAtt.getLocalName();
                final String value = fieldAtt.getNodeValue();
                //log.debug("lopping through field attributes, handling: " + fieldAtt.getNodeName());
                if (fieldAtt.getNamespaceURI().equals(NAMESPACE)) {
                    //log.debug("local field + " + fieldAtt.getLocalName() + "value:" + fieldAtt.getNodeValue());
                    if ("type".equals(localName)) {
                        f.setType(IndexUtils.getType(fieldAtt,
                                                     defaults.getFieldType()));
                    } else if ("name".equals(localName)) {
                        f.setName(value);
                       // log.debug("name:" + f.getName());
                    } else if ("boostFactor".equals(localName)) {

                        try{
                        f.setBoost(Float.parseFloat(value));
                        } catch (NumberFormatException e){
                            IndexUtils.log.warn("unable to get boostfactor on "
                                                + "field using 1.0");
                            f.setBoost(1.0f);
                        }
                        //log.debug("boostFactor: " + f.getBoost());
                    } else if ("suggest".equals(localName)) {
                        f.setSuggest(Boolean.parseBoolean(value));
                        //log.debug("suggest: " + f.isSuggest());
                    //} else if ("groupID".equals(localName)) {
                    //    f.setGroup(value);
                    } else if ("freetext".equals(localName)) {
                        f.setFreetext(Boolean.parseBoolean(value));
                    } else if ("navn".equals(localName)){
                        f.addAlias(new IndexAlias(value, "da"));
                    } else if ("repeat".equals(localName)){
                        f.setRepeat(Boolean.parseBoolean(value));
                    }
                } else if (fieldAtt.getNamespaceURI().
                        equals(XMLConstants.XML_NS_URI)){
                    if ("lang".equals(fieldAtt.getLocalName())){
                        f.setLanguage(fieldAtt.getNodeValue());
                    }
                }
            }

           return f;
    }

    /**
     * Create the IndexDefaults from the NamedNodeMap extracted from the SummaIndexDefinition xml.
     *
     * @param att
     * @param Namespace the SummaIndexDefintion namespace URI
     * @return IndexDefaults for this index.
     */
    public static IndexDefaults getDefaults(NamedNodeMap att, String Namespace){

        IndexDefaults defaults = new IndexDefaults();
        Node group = att.getNamedItemNS(Namespace,"defaultGroup");
        Node suggest = att.getNamedItemNS(Namespace,"defaultSuggest");
        Node boost = att.getNamedItemNS(Namespace, "defaultBoost");
        Node type = att.getNamedItemNS(Namespace, "defaultType");
        Node resolver = att.getNamedItemNS(Namespace, "resolver");
        Node freetext = att.getNamedItemNS(Namespace, "defaultFreetext");

        if (resolver != null){
            defaults.setResolver(resolver.getNodeValue());
        }

        if (group != null){
            defaults.setGroup(Boolean.parseBoolean(group.getNodeValue()));
        }
        if (suggest != null){
            defaults.setSuggest(Boolean.parseBoolean(suggest.getNodeValue()));
        }
        if (freetext != null){
            defaults.setFreeText(Boolean.parseBoolean(freetext.getNodeValue()));
        }
        if (boost != null){
            try{
            defaults.setBoost(Float.parseFloat(boost.getNodeValue()));
            } catch (NumberFormatException e){
                IndexUtils.log.warn("unable to set default boost using 1.0");
                defaults.setBoost(1.0f);
            }
        }
        if (type != null) {
            String ty = type.getNodeValue().trim();
            if ("keyword".equals(ty)) {
                defaults.setFieldType(FieldType.keyWord);
            } else if ("test".equals(ty)) {
                defaults.setFieldType(FieldType.text);
            } else if ("sortkey".equals(ty)) {
                defaults.setFieldType(FieldType.sort);
            } else if ("store".equals(ty)) {
                defaults.setFieldType(FieldType.stored);
            } else if ("date".equals(ty)) {
                defaults.setFieldType(FieldType.date);
            }
        }
        return defaults;
    }

    /**
     * Convenient method to get a collected String of text nodes in an element.
     *
     * @param e the element
     *
     * @return all text from child text nodes of the element, if no text nodes are found an empty string is returned
     *
     * @throws IllegalArgumentException runtime error thrown if the argument is not a ELEMENT_NODE
     */
   public static String getElementNodeValue(final Node e) throws
                                                      IllegalArgumentException {
        String ret = "";
        if (e.getNodeType() == Node.ELEMENT_NODE) {
            final NodeList all = e.getChildNodes();
            for (int i = 0; i < all.getLength(); i++) {
                if (all.item(i).getNodeType() == Node.TEXT_NODE ||
                    all.item(i).getNodeType() == Node.CDATA_SECTION_NODE) {
                    ret += all.item(i).getNodeValue();
                }
            }
        }

        return ret;
    }

}
