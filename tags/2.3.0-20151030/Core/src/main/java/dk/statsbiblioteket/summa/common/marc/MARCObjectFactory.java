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

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes an InputStream and generates MARCObjects from it.
 * </p><p>
 * This class is Thread safe.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class MARCObjectFactory {
    private static Log log = LogFactory.getLog(MARCObjectFactory.class);

    /**
     * Iterates through the provided MARC21Slim XML, constructs {@link MARCObject}s from the input and return them all
     * at once. This method is appropriate when the amount of MARC elements is known to be small. For streaming
     * processing, use {@link #generate(InputStream, Callback)}.
     * @param xml MARC21Slim XML.
     * @throws XMLStreamException if the provided XML was invalid.
     * @throws IOException if the InputStream could not be read.
     * @throws ParseException if there was a format error in the XML.
     */
    public static List<MARCObject> generate(InputStream xml) throws XMLStreamException, IOException, ParseException {
        final List<MARCObject> result = new ArrayList<>();
        generate(xml, new Callback() {
            @Override
            public void handle(MARCObject marc) {
                result.add(marc);
            }
        });
        return result;
    }

    /**
     * Iterates through the provided MARC21Slim XML, constructs {@link MARCObject}s from the input and delivers them
     * to the sink.
     * @param xml MARC21Slim XML.
     * @param sink constructed MARCObjects will be delivered here.
     * @throws XMLStreamException if the provided XML was invalid.
     * @throws IOException if the InputStream could not be read.
     * @throws ParseException if there was a format error in the XML.
     */
    public static void generate(InputStream xml, final Callback sink)
                                                                throws XMLStreamException, IOException, ParseException {
        MARCStepper parser = new MARCStepper();
        parser.parse(xml, new MARCStepper.MarcCallback() {
            private MARCObject marc;
            private MARCObject.DataField dataField = null; // If assigned, this is the current field

            @Override
            public void startRecord(String id, String type) {
                log.trace("Constructing new MARCObject(id=" + id + ", type=" + type + ")");
                marc = new MARCObject(id, type);
            }

            @Override
            public void leader(String id, String content) {
                marc.setLeader(new MARCObject.Leader(id, content));
            }

            @Override
            public void controlField(String tag, String id, String content) {
                marc.getControlFields().add(new MARCObject.ControlField(tag, id, content));
            }

            @Override
            public void startDataField(String tag, String id, String ind1, String ind2) {
                if (dataField != null) {
                    throw new IllegalStateException(String.format(
                        "Structural error: DataField already exists. Conflicting fields are %s and %s",
                        dataField.getTag(), tag));
                }
                dataField = new MARCObject.DataField(tag, id, ind1, ind2);
            }

            @Override
            public void subField(String fieldTag, String fieldId, String subCode, String subContent) {
                if (dataField == null) {
                    throw new IllegalStateException(String.format(
                        "SubField for field %s encountered, but no startDataField has been called", fieldTag));
                }
                if (!fieldTag.equals(dataField.getTag())) {
                    throw new IllegalStateException(String.format(
                        "SubField for field %s encountered, but existing field is %s", fieldTag, dataField.getTag()));
                }
                dataField.getSubFields().add(new MARCObject.SubField(subCode, subContent));
            }

            @Override
            public void endDataField() {
                marc.getDataFields().add(dataField);
                dataField = null;
            }

            @Override
            public void endRecord() {
                log.trace("Delivering MARCObject to sink");
                sink.handle(marc);
            }
        });
    }

    public static interface Callback {
        /**
         *
         * @param marc a MARC object generated from the source.
         */
        void handle(MARCObject marc);
    }
}
