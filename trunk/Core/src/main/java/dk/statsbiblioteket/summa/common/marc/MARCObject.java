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
package dk.statsbiblioteket.summa.common.marc;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Object representation of MARC21Slim. See http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd
 */
// TODO: Consider optionally namespacing the output
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MARCObject implements Cloneable {
    private static Log log = LogFactory.getLog(MARCObject.class);

    public static final String MARC21_NAMESPACE = "http://www.loc.gov/MARC21/slim";

    private static XMLOutputFactory factory = null;

    private String id = null;
    private String type = null;
    private Leader leader = null;
    private List<ControlField> controlFields = new ArrayList<ControlField>();
    private List<DataField> dataFields= new ArrayList<DataField>();

    public MARCObject(String id, String type) {
        log.debug("Creating with id='" + id + "', type='" + type + "'");
        this.id = id;
        this.type = type;
    }

    private synchronized XMLOutputFactory getFactory() {
        if (factory == null) {
            factory = XMLOutputFactory.newInstance();
        }
        return factory;
    }

    /**
     * @return the MARCObject in MARC21Slim format including XML declaration.
     */
    public String toXML() throws XMLStreamException {
        StringWriter sw = new StringWriter();
        sw.append(MARC.XML_HEADER).append("\n");
        XMLStreamWriter writer = getFactory().createXMLStreamWriter(sw);
        toXML(writer);
        writer.flush();
        return sw.toString();
    }

    /**
     * @param xml the content of this Object is written to this stream. Note that an XML declaration is not written.
     */
    public void toXML(XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement(MARC.TAG_RECORD);
        xml.writeNamespace("", MARC21_NAMESPACE);
        attribute(xml, MARC.ATTRIBUTE_ID, id);
        attribute(xml, MARC.TAG_RECORD_ATTRIBUTE_TYPE, type);
        if (leader != null) {
            leader.toXML(xml);
        }
        for (ControlField controlField: controlFields) {
            controlField.toXML(xml);
        }
        for (DataField dataField: dataFields) {
            dataField.toXML(xml);
        }
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Leader getLeader() {
        return leader;
    }

    public void setLeader(Leader leader) {
        this.leader = leader;
    }

    public List<ControlField> getControlFields() {
        return controlFields;
    }

    public List<DataField> getDataFields() {
        return dataFields;
    }

    /**
     * @param tag the designation for the wanted DataField.
     * @return the first DataField with the given tag or null if no field satisfies this.
     */
    public DataField getFirstDataField(String tag) {
        for (DataField field: dataFields) {
            if (tag.equals(field.getTag())) {
                return field;
            }
        }
        return null;
    }

    /**
     * @param tag the designation for the wanted DataField.
     * @return the DataFields with the given tag.
     */
    public List<DataField> getDataFields(String tag) {
        List<DataField> fields = new ArrayList<DataField>();
        for (DataField field: dataFields) {
            if (tag.equals(field.getTag())) {
                fields.add(field);
            }
        }
        return fields;
    }

    /**
     * @param tag the designation for the wanted DataField.
     * @param code the designation for the wanted SubField.
     * @return the first SubField that satisfies the given conditions or null if no such SubField could be found.
     */
    public SubField getFirstSubField(String tag, String code) {
        DataField field = getFirstDataField(tag);
        return field == null ? null : field.getFirstSubField(code);
    }

    public static class Leader implements Cloneable {
        private String id;
        private String content;

        public Leader(String id, String content) {
            this.id = id;
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public String getContent() {
            return content;
        }

        public void toXML(XMLStreamWriter xml) throws XMLStreamException {
            xml.writeStartElement(MARC.TAG_LEADER);
            attribute(xml, MARC.ATTRIBUTE_ID, id);
            xml.writeCharacters(content);
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            Leader other = (Leader) super.clone();
            other.id = id;
            other.content = content;
            return other;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Leader)) {
                return false;
            }
            Leader other = (Leader)obj;
            return MARCObject.equals(other.id, id) && MARCObject.equals(other.content, content);
        }
    }

    public static class ControlField implements Cloneable {
        private String id;
        private String tag;
        private String content;

        public ControlField(String tag, String id, String content) {
            this.id = id;
            this.tag = tag;
            this.content = content;
        }

        public String getId() {
            return id;
        }

        public String getTag() {
            return tag;
        }

        public String getContent() {
            return content;
        }

        public void toXML(XMLStreamWriter xml) throws XMLStreamException {
            xml.writeStartElement(MARC.TAG_CONTROLFIELD);
            attribute(xml, MARC.ATTRIBUTE_ID, id);
            attribute(xml, MARC.TAG_CONTROLFIELD_ATTRIBUTE_TAG, tag);
            xml.writeCharacters(content);
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            ControlField other = (ControlField) super.clone();
            other.id = id;
            other.tag = tag;
            other.content = content;
            return other;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ControlField)) {
                return false;
            }
            ControlField other = (ControlField)obj;
            return MARCObject.equals(other.id, id) 
                   && MARCObject.equals(other.tag, tag)
                   && MARCObject.equals(other.content, content);
        }
    }

    public static class DataField implements Cloneable {
        private String id;
        private String tag;
        private String ind1;
        private String ind2;
        private List<SubField> subFields = new ArrayList<SubField>();

        public DataField(String tag, String id, String ind1, String ind2) {
            this.id = id;
            this.tag = tag;
            this.ind1 = ind1;
            this.ind2 = ind2;
        }

        public String getId() {
            return id;
        }

        public String getTag() {
            return tag;
        }

        public String getInd1() {
            return ind1;
        }

        public String getInd2() {
            return ind2;
        }

        public List<SubField> getSubFields() {
            return subFields;
        }

        /**
         * @param code the code for the wanted SubField.
         * @return the first SubField with the wanted code or null if no SubField satisfies the requirement.
         */
        public SubField getFirstSubField(String code) {
            for (SubField subField: subFields) {
                if (code.equals(subField.getCode())) {
                    return subField;
                }
            }
            return null;
        }

        public void toXML(XMLStreamWriter xml) throws XMLStreamException {
            xml.writeStartElement(MARC.TAG_DATAFIELD);
            attribute(xml, MARC.ATTRIBUTE_ID, id);
            attribute(xml, MARC.TAG_DATAFIELD_ATTRIBUTE_TAG, tag);
            attribute(xml, MARC.TAG_DATAFIELD_ATTRIBUTE_IND1, ind1);
            attribute(xml, MARC.TAG_DATAFIELD_ATTRIBUTE_IND2, ind2);
            xml.writeCharacters("\n");
            for (SubField subField: subFields) {
                subField.toXML(xml);
            }
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            DataField other = (DataField) super.clone();
            other.id = id;
            other.tag = tag;
            other.ind1 = ind1;
            other.ind2 = ind2;
            other.subFields = new ArrayList<SubField>(subFields.size());
            for (SubField sf: subFields) {
                other.subFields.add((SubField) sf.clone());
            }
            return other;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DataField)) {
                return false;
            }
            DataField other = (DataField)obj;
            return MARCObject.equals(other.id, id)
                   && MARCObject.equals(other.tag, tag)
                   && MARCObject.equals(other.ind1, ind1)
                   && MARCObject.equals(other.ind2, ind2)
                   && MARCObject.equals(other.subFields, subFields);
        }
    }

    public static class SubField implements Cloneable {
        private String code;
        private String content;

        public SubField(String code, String content) {
            this.code = code;
            this.content = content;
        }

        public String getCode() {
            return code;
        }

        public String getContent() {
            return content;
        }

        public void toXML(XMLStreamWriter xml) throws XMLStreamException {
            xml.writeStartElement(MARC.TAG_SUBFIELD);
            attribute(xml, MARC.TAG_SUBFIELD_ATTRIBUTE_CODE, code);
            xml.writeCharacters(content);
            xml.writeEndElement();
            xml.writeCharacters("\n");
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            SubField other = (SubField) super.clone();
            other.code = code;
            other.content = content;
            return other;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SubField)) {
                return false;
            }
            SubField other = (SubField)obj;
            return MARCObject.equals(other.code, code) && MARCObject.equals(other.content, content);
        }
    }

    static void attribute(XMLStreamWriter xml, String name, String content) throws XMLStreamException {
        if (content == null) {
            return;
        }
        xml.writeAttribute(name, content);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        MARCObject other = (MARCObject) super.clone();
        other.id = id;
        other.type = type;
        other.leader = (Leader) leader.clone();
        other.controlFields = new ArrayList<ControlField>(controlFields.size());
        for (ControlField cf: controlFields) {
            other.controlFields.add((ControlField) cf.clone());
        }
        other.dataFields = new ArrayList<DataField>(dataFields.size());
        for (DataField df: dataFields) {
            other.dataFields.add((DataField) df.clone());
        }
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MARCObject)) {
            return false;
        }
        MARCObject other = (MARCObject)obj;
        return equals(other.id, id)
               && equals(other.type, type)
               && equals(other.leader, leader)
               && equals(other.controlFields, controlFields)
               && equals(other.dataFields, dataFields);
    }


    static boolean equals(Object s1, Object s2) {
        return (s1 == null && s2 == null) || (s1 != null && s1.equals(s2));
    }

    static boolean equals(List l1, List l2) {
        if (l1 == null && l2 == null) {
            return true;
        }
        if (l1 == null || l2 == null || l1.size() != l2.size()) {
            return false;
        }
        for (int i = 0 ; i < l1.size() ; i++) {
            if (!equals(l1.get(i), l2.get(i))) {
                return equals(l1.get(i), l2.get(i));
            }
        }
        return true;
    }
}
