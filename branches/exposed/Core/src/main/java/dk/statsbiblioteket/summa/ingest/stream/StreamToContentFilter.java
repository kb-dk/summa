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
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Adds a Record to the Payload with the content from the given Stream.
 * </p><p>
 * This class is expected to be deprecated in Summa 2.0.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StreamToContentFilter extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(StreamToContentFilter.class);

    /**
     * Assigns the given base to created Records.
     * </p><p>
     * Optional. Default is blank.
     */
    public static final String CONF_BASE = "summa.streamtocontent.base";
    public static final String DEFAULT_BASE = "";

    private String base = DEFAULT_BASE;

    public StreamToContentFilter(Configuration conf) {
        super(conf);
        base = conf.getString(CONF_BASE, base);
        log.debug(String.format("Filter created with base='%s'", base));
    }

    @Override
    public String getName() {
        return "StreamToContentFilter";
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() == null) {
            Logging.logProcess(
                    getName(), "No Stream, so no Record can be created",
                    Logging.LogLevel.TRACE, payload);
            return true;
        }
        if (payload.getRecord() != null) {
            Logging.logProcess(
                    getName(), "Existing Record will be discarded",
                    Logging.LogLevel.TRACE, payload);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
        try {
            Streams.pipe(payload.getStream(), out); // Auto-closes both Streams
        } catch (IOException e) {
            throw new PayloadException("Unable to copy Stream to content",
                                       e, payload);
        }
        byte[] result = out.toByteArray();
        log.trace("Produced content of length " + result.length);
        Record record = new Record(constructID(payload), base, result);
        payload.setRecord(record);
        payload.setStream(null);

        Logging.logProcess(
                getName(), "Assigned content of Stream to Record",
                Logging.LogLevel.TRACE, payload);
        return true;
    }

    private String constructID(Payload payload) {
        //noinspection DuplicateStringLiteralInspection
        return payload.getId() == null
               ? "Dummy_" + Long.toString(System.currentTimeMillis())
               : payload.getId();
    }
}
