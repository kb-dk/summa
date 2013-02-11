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
package dk.statsbiblioteket.summa.support.enrich;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.MARCObjectFilter;
import dk.statsbiblioteket.summa.common.marc.MARCObject;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Creates a sub-Record with ID 'marcdump_<counter>' that holds the cleaned MARC record (parsed and serialized).
 * </p><p>
 * Optionally the generated XML will be in directly indexable Summadocument format. This produces the fields
 * {@code marc_leader}, {@code marc_control_<control_field_tag>}*, {@code marc_controls} (all defined control fields),
 * {@code marc_field_<field><subfield_code>}*, {@code marc_fields} (all defined fields.
 * </p><p>
 * Important: This filter requires the input Payload to have its MARC-XML embedded as Record content,
 * not Payload stream.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MARCXMLCopyFilter extends MARCObjectFilter {
    private static Log log = LogFactory.getLog(MARCXMLCopyFilter.class);

    private static XMLOutputFactory factory = null;

    // TODO: Consider adding direct Solr
    public enum OUTPUT {marc21slim, directsumma}

    /**
     * The output from the processor.
     * </p><p>
     * Optional. Default is marc21slim. Valid values are marc21slim and directsumma.
     */
    public static final String CONF_OUTPUT = "marcxmlcopyfilter.output";
    public static final String DEFAULT_OUTPUT = OUTPUT.marc21slim.toString();

    private int created = 0;
    private final OUTPUT output;

    public MARCXMLCopyFilter(Configuration conf) {
        super(conf);
        output = OUTPUT.valueOf(conf.getString(CONF_OUTPUT, DEFAULT_OUTPUT));
        log.info("Created MARCXMLCopyFilter with output=" + output);
    }

    @Override
    protected MARCObject adjust(Payload payload, MARCObject marcObject) {
        if (payload.getRecord() == null) {
            throw new IllegalArgumentException(
                    "MARC copy does not accept Payload streams as input. A Record must be present");
        }
        List<Record> children = payload.getRecord().getChildren() == null ?
                                new ArrayList<Record>() :
                                new ArrayList<Record>(payload.getRecord().getChildren());
        children.add(new Record("marcdump_" + created++, "marc", getContent(payload, marcObject)));
        payload.getRecord().setChildren(children);
        return marcObject;
    }

    protected byte[] getContent(Payload payload, MARCObject marcObject) {
        try {
            switch (output) {
                case marc21slim: return getMARC(marcObject);
                case directsumma: return getDirectSumma(payload, marcObject);
                default: throw new UnsupportedOperationException("The output format '" + output + "' is unsupported");
            }
        } catch (UnsupportedEncodingException e) {
            log.error("utf-8 should be supported", e);
            return null;
        } catch (XMLStreamException e) {
            Logging.logProcess("MARCXMLCopyFilter", "Unable to serialize MARC object to output " + output,
                               Logging.LogLevel.WARN, payload, e);
            return null;
        }
    }

    private byte[] getMARC(MARCObject marcObject) throws UnsupportedEncodingException, XMLStreamException {
        return marcObject.toXML().getBytes("utf-8");
    }

    private byte[] getDirectSumma(Payload payload, MARCObject marcObject) throws XMLStreamException,
                                                                                 UnsupportedEncodingException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter xml = getFactory().createXMLStreamWriter(sw);
        xml.setDefaultNamespace("http://statsbiblioteket.dk/summa/2008/Document");
        xml.writeStartDocument();

        xml.writeCharacters("\n");
        xml.writeStartElement("http://statsbiblioteket.dk/summa/2008/Document", "SummaDocument");
        xml.writeNamespace(null, "http://statsbiblioteket.dk/summa/2008/Document");
        xml.writeAttribute("version", "1.0");
        xml.writeAttribute("id", "invalid");
        xml.writeCharacters("\n");

        xml.writeStartElement("fields");
        xml.writeCharacters("\n");

        writeField(xml, "marc_leader", marcObject.getLeader().getContent());
        for (MARCObject.ControlField controlField: marcObject.getControlFields()) {
            writeField(xml, "marc_control_" + controlField.getTag(), controlField.getContent());
            writeField(xml, "marc_controls", controlField.getTag());
        }
        for (MARCObject.DataField dataField: marcObject.getDataFields()) {
            for (MARCObject.SubField subField: dataField.getSubFields()) {
                writeField(xml, "marc_field_" + dataField.getTag() + subField.getCode(), subField.getContent());
                writeField(xml, "marc_fields", dataField.getTag() + subField.getCode());
            }
        }

        xml.writeEndElement(); // fields
        xml.writeCharacters("\n");

        xml.writeEndElement(); // Summadocument
        xml.writeCharacters("\n");

        xml.flush();
        return sw.toString().getBytes("utf-8");
    }

    private static final Pattern VALID_FIELD = Pattern.compile("[-a-zA-Z0-9_.]+");
    private final Set<String> discarded = new HashSet<String>();
    private void writeField(XMLStreamWriter xml, String field, String content) throws XMLStreamException {
        if (content == null || content.isEmpty()) {
            return;
        }
        if (!VALID_FIELD.matcher(field).matches()) {
            discarded.add(field);
            return;
        }
        xml.writeCharacters("  ");
        xml.writeStartElement("field");
        xml.writeAttribute("name", field);
        xml.writeCharacters(content);
        xml.writeEndElement();
        xml.writeCharacters("\n");
    }

    private synchronized XMLOutputFactory getFactory() {
        if (factory == null) {
            factory = XMLOutputFactory.newInstance();
        }
        return factory;
    }

    @Override
    public void close(boolean success) {
        super.close(success);
        if (discarded.isEmpty()) {
            log.info("Closing MARCXMLCopyFilter with no invalid field names");
        } else {
            log.info("Closing MARCXMLCopyFilter with " + discarded + " unique discarded field names: "
                     + Strings.join(discarded, ", "));
        }
    }
}
