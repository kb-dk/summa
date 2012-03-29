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

import java.io.Serializable;

/**
 * Convenience class for keeping track of timing information.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TimerImpl implements Timer, Serializable {
    private static final long serialVersionUID = -8618416726829272962L;
    private String prefix;
    private StringBuffer timing = null;

    public TimerImpl() {
    }

    public TimerImpl(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Adds the given timing information to the list.
     * @param key the designation for the timing information. Keep it short and
     * simple: "dym", "search,raw", "facet.count", "facet.extract" etc.
     * @param ms the number of milliseconds.
     */
    @Override
    public synchronized void addTiming(String key, long ms) {
        checkTiming();
        if (getPrefix() != null) {
            timing.append(getPrefix());
        }
        timing.append(key).append(":").append(Long.toString(ms));
    }

    /**
     * Adds the given timing information to the list. No prefix will be
     * prepended.
     * @param timing timing expressed as "key:ms".
     */
    public synchronized void addTiming(String timing) {
        if (timing == null || "".equals(timing)) {
            return;
        }
        checkTiming();
        this.timing.append(timing);
    }

    private void checkTiming() {
        if (timing == null) {
            timing = new StringBuffer(100);
        }
        if (timing.length() != 0) {
            timing.append("|");
        }
    }

    @Override
    public String getTiming() {
        return timing == null ? "" : timing.toString();
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

}
