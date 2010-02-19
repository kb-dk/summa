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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Wraps the content of incoming Records in Payloads in an InputStream, which
 * is assigned to the Payload and passed on.
 * </p><p>
 * Note 1: Wrapping as stream implies a potential uncompression of the content,
 * which will take place in RAM. This can effectively inflate the memory-hit
 * taken from the Record.
 * </p><p>
 * Note 2: Existing Streams will be closed and removed.
 * </p><p>
 * This class is expected to be deprecated in Summa 2.0.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ContentToStreamFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(ContentToStreamFilter.class);

    public ContentToStreamFilter(Configuration conf) {
        super(conf);
    }

    @Override
    public String getName() {
        return "ContentToStreamFilter";
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getRecord() == null) {
            Logging.logProcess(
                    getName(), "No Record, so no Stream can be created",
                    Logging.LogLevel.TRACE, payload);
            return true;
        }
        if (payload.getRecord().getContent(false) == null) {
            Logging.logProcess(
                    getName(), "No Record content, so no Stream can be created",
                    Logging.LogLevel.TRACE, payload);
            return true;
        }
        if (payload.getStream() != null) {
            Logging.logProcess(
                    getName(), "Closing and discarding existing Stream",
                    Logging.LogLevel.TRACE, payload);
            try {
                payload.getStream().close();
            } catch (IOException e) {
                throw new PayloadException(
                        "Exception closing existing stream", e, payload);
            }
        }
        Logging.logProcess(getName(), "Wrapping record content as stream",
                           Logging.LogLevel.TRACE, payload);
        payload.setStream(new ByteArrayInputStream(
                payload.getRecord().getContent(true)));
        if (log.isTraceEnabled()) {
            log.trace("Wrapped content to Stream in " + payload);
        }
        return true;
    }
}

