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

import java.util.Iterator;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * An ObjectFilter processes a triplet of Stream, Record and Document. Not all
 * parts of the triplet needs to be present. It is expected that most filters
 * will be of this type.
 * </p><p>
 * Streams, Records and Documents are pumped through the chain of filters by
 * calling {@link #pump()} on the last filter in the chain. It is up to the
 * individual filters to process the stream if present.
 * </p><p>
 * Important: pump()-implementations that extracts payloads from source-
 *            ObjectFilters are required to call close() on the extracted
 *            payloads.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface ObjectFilter extends Configurable, Filter, Iterator<Payload> {
    /**
     * This call might be blocking. If true is returned, it is expected that
     * a Payload can be returned by {@link #next()}. If false is returned,
     * it is guaranteed that no more Payloads can be returned by getNext().
     * </p><p>
     * If next() has returned null, hasNext must return false.
     * @return true if more Payloads are available, else false.
     */
    public boolean hasNext();
}



