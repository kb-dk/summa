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
package dk.statsbiblioteket.summa.common.filter;

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Copies the content if received Streams into memory, closes the source Stream
 * and creates a new Stream from the content in memory.
 * The standard use case is a source that needs Streams to be closed, before it
 * cal deliver more Streams.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "te",
        reviewers = "mke")
public class StreamCopyFilter extends ObjectFilterImpl {

    /**
     * If true, a warning is logged when a Payload with no Stream is
     * encountered. If false, nothing happens.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_WARN_ON_NO_STREAM =
            "common.streamcopy.warnonnostream";
    public static final boolean DEFAULT_WARN_ON_NO_STREAM = true;

    private boolean warnOnNoStream = DEFAULT_WARN_ON_NO_STREAM;

    public StreamCopyFilter(Configuration conf) {
        super(conf);
        warnOnNoStream = conf.getBoolean(
                CONF_WARN_ON_NO_STREAM, warnOnNoStream);
    }

    @Override
    protected boolean processPayload(Payload payload) throws PayloadException {
        if (payload.getStream() == null) {
            if (warnOnNoStream) {
                Logging.logProcess("StreamCopyFilter", "No Stream in Payload",
                                   Logging.LogLevel.WARN, payload);
            }
            return true;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
        try {
            Streams.pipe(payload.getStream(), out); // Auto-closes both Streams
        } catch (IOException e) {
            throw new PayloadException("Unable to copy Stream", e, payload);
        }
        // TODO: Consider reusing the internal byte array from out
        payload.setStream(new ByteArrayInputStream(out.toByteArray()));
        return true;
    }

}

