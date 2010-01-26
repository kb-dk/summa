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

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A filter is something that can be chained at the end of other filters and
 * activated by pumping data through it.
 * </p><p>
 * Implementations of Filter will always have side-effects, such as requesting
 * data, transforming, inspection etc.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface Filter {

    /**
     * Used in configurations to state the name of the filter. The name of the
     * filter should be used for feedback and debugging purposes only.
     * </p><p>
     * Support for filter-name is optional for Filters. 
     */
    public static final String CONF_FILTER_NAME = "filter.name";

    /**
     * Set the source for the Filter. This allows for chaining. Implementations
     * of Filter will normally put constraints on the type of Filters they
     * accept as sources.
     * @param filter the source for the Filter.
     */
    public void setSource(Filter filter);

    /**
     * Filters are utilized by calling pump until it returns false.
     * Calling pump after it has returned false should return false.
     * </p><p>
     * Any filter must be able to empty its underlying filters completely
     * (if the underlying filters can be emptied completely, that is) by
     * repeated calls to pump until pump returns false.
     * </p><p>
     * Note: The general behaviour of pumping is left to the implementation.
     * For stream-oriented filters, this will normally result in the emptying
     * of streams embedded in Payloads before further payloads are requested
     * downstream.
     * @return true if more data are available, else false.
     * @throws IOException if case of major errors during pumping.
     */
    public boolean pump() throws IOException;

    /**
     * Closes this and any underlying Filters in a manner depending on
     * the state of success. If the state is not successfull, implementations
     * should take appropriate actions, such as marking source-material for
     * later re-ingestion.
     * </p><p>
     * Note: It is legal to continue pumping the Filter after calling close.
     *       This is in order to empty caches and similar.
     * </p><p>
     * Note: The implementation is responsible for calling close(success) on
     *       the filter source, if available.
     * @param success if true a normal shutdown should take place.
     */
    public void close(boolean success);
}
