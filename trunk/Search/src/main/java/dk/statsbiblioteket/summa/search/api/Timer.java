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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Timing information for performance measurements etc.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface Timer {
    /**
     * Recommended xml attribute for delivering timing information.
     */
    public static final String TIMING = "timing";

    /**
     * Adds the given timing information.
     * @param key the designation for the timing information. Keep it short and
     * simple: "dym", "search,raw", "facet.count", "facet.extract" etc.
     * @param ms the number of milliseconds.
     */
    void addTiming(String key, long ms);

    /**
     * @return a |-separated list of measured processing time for generating the
     * response. Each entry consists of key:ms.
     */
    String getTiming();

}
