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
package dk.statsbiblioteket.summa.search.api;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.lucene.index.codecs.preflex.SegmentTermPositions;

import java.io.Serializable;

/**
 * Convenience class for keeping track of timing information.
 * </p><p>
 * Remember to call super.merge(Response) when implementing merge in order to
 * merge timing information.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
abstract public class ResponseImpl extends TimerImpl
    implements Response, Serializable {
    /**
     * Constructor without explicit prefix. {@link #getName()} + "." will be
     * used for prefix, but will be lazily requested.
     */
    protected ResponseImpl() {
    }

    /**
     * @param prefix will be appended to all timing information. Recommended
     * prefix is "uniquename.".
     */
    protected ResponseImpl(String prefix) {
        super(prefix);
    }

    @Override
    public synchronized void merge(Response other) throws ClassCastException {
        if (other.getTiming() == null) {
            return;
        }
        super.addTiming(other.getTiming());
    }

    @Override
    public String getPrefix() {
        if (super.getPrefix() == null) {
            setPrefix(getName() + ".");
        }
        return super.getPrefix();
    }

}
