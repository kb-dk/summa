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
package dk.statsbiblioteket.summa.ingest.split;

import java.util.Iterator;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

/**
 * Parses a stream (provided by a Payload) into multiple Records wrapped in
 * Payloads. Normally used by  {@link StreamController}.
 * A StreamParser is reusable and is cleared and initialized by {@link #open}.
 * </p><p>
 * Note: Implemenetations must return false for hasNext if open has not been
 * called.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public interface StreamParser extends Iterator<Payload>, Configurable {
    /**
     * Clears and initializes the parser with the stream from the given Payload.
     * @param streamPayload the Payload with the stream to parse.
     */
    public void open(Payload streamPayload);

    /**
     * If a parsing is underway, it is stopped and any queued Payloads are
     * discarded. The StreamParser can be reused after stop.
     */
    public void stop();

    /**
     * Shuts down the parser, stopping running Threads and the like. The state
     * of the parser is undefined after close and it is not guaranteed that it
     * can be reused.
     */
    public void close();
}

