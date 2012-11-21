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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.marc.MARCObject;
import dk.statsbiblioteket.summa.common.marc.MARCObjectFactory;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * Receives Records with MARC21Slim XML as content, parses the content as {@link MARCObject}s, allows for modifications
 * of the object and passes it on in XML form.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class MARCObjectFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(MARCObjectFilter.class);

    public MARCObjectFilter(Configuration conf) {
        super(conf);
        log.info("Constructed MARCObjectFilter");
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        try {
            List<MARCObject> marcObjects = MARCObjectFactory.generate(RecordUtil.getStream(payload));
            if (marcObjects.size() == 0) {
                log.warn("No MARCObject extracted from " + payload);
                Logging.logProcess("MARCObjectFilter", "No MARCObject extracted", Logging.LogLevel.WARN, payload);
                return false;
            }
            if (marcObjects.size() > 1) {
                String message = "Expected a single MARCObject but got " + marcObjects.size()
                                 + ". Only the first object will be processed";
                log.warn(message);
                Logging.logProcess("MARCObjectFilter", message, Logging.LogLevel.WARN, payload);
            } else {
                Logging.logProcess("MARCObjectFilter", "Created MARCObject", Logging.LogLevel.TRACE, payload);
            }
            MARCObject adjusted = adjust(payload, marcObjects.get(0));
            if (adjusted == null) {
                return false;
            }
            if (payload.getStream() != null) {
                payload.setStream(new ByteArrayInputStream(adjusted.toXML().getBytes("utf-8")));
                //payload.setStream(new StringInputStream(adjusted.toXML()));
            } else {
                payload.getRecord().setContent(adjusted.toXML().getBytes("utf-8"), false);
            }
        } catch (XMLStreamException e) {
            throw new PayloadException("Invalid XML", e, payload);
        } catch (IOException e) {
            throw new PayloadException("Inaccessible Stream", e, payload);
        } catch (ParseException e) {
            throw new PayloadException("MARC XML error", e, payload);
        }
        return true;
    }

    /**
     * @param marcObject the MARC to act upon..
     * @param payload the originating Payload.
     * @return the MARC to pass on. null signals that the marcObject should be ignored.
     */
    protected abstract MARCObject adjust(Payload payload, MARCObject marcObject);
}
