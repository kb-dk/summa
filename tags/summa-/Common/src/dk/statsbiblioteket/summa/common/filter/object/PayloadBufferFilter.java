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

import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.util.List;
import java.util.ArrayList;

/**
 * Buffers all incoming payloads for later retrieval
 */
public class PayloadBufferFilter extends ObjectFilterImpl {
    /** The payloads accepted. */
    List<Payload> payloads;

    /**
     * Constructs a PayloadBufferFilter which stores all incoming payloads for
     * later retrieval.
     * @param conf The configuration.
     */
    public PayloadBufferFilter(Configuration conf) {
        super(conf);
        payloads = new ArrayList<Payload>();
    }

    /**
     * Each payload is locally stored for later retrievel.
     * @param payload The process.
     * @return True.
     */
    protected boolean processPayload(Payload payload) {
        payloads.add(payload);
        return true;
    }

    /**
     * Return the {@code idx}'s payload recieved.
     * @param idx The {@code idx}'th payload to return.
     * @return {@code idx}'th payload.
     */
    public Payload get(int idx) {
        return payloads.get(idx);
    }

    /**
     * Return the number of payloads processed.
     * @return Number of payloads processed.
     */
    public int size() {
        return payloads.size();
    }
}