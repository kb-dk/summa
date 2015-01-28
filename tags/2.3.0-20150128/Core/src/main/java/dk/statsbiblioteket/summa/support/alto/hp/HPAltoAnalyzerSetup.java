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
package dk.statsbiblioteket.summa.support.alto.hp;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.alto.AltoAnalyzerSetup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class HPAltoAnalyzerSetup extends AltoAnalyzerSetup {
    private static Log log = LogFactory.getLog(HPAltoAnalyzerSetup.class);

    /**
     * If true, Segments that has no time are merged into the previous segment, if it has time.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_MERGE_SUBSEQUENT_NOTIME = "hpaltoanalyzer.merge.subsequent";
    public static final boolean DEFAULT_MERGE_SUBSEQUENT_NOTIME = true;

    /**
     * If a Segment has no end time, the start time from the next Segment with a start time will be used.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_CONNECT_TIMES = "hpaltoanalyzer.connect.times";
    public static final boolean DEFAULT_CONNECT_TIMES = true;

    /**
     * If true, any leftover floating TextBlocks after standard Segment construction will be attach to the Segments
     * they are closest to, measured from middle of the right side of the segment.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_ATTACH_FLOATERS = "hpaltoanalyzer.floaters.attach";
    public static final boolean DEFAULT_ATTACH_FLOATERS = true;

    private final boolean mergeSubsequent;
    private final boolean connectTimes;
    private final boolean attachFloaters;

    public HPAltoAnalyzerSetup(Configuration conf) {
        super(conf);
        mergeSubsequent = conf.getBoolean(CONF_MERGE_SUBSEQUENT_NOTIME, DEFAULT_MERGE_SUBSEQUENT_NOTIME);
        connectTimes = conf.getBoolean(CONF_CONNECT_TIMES, DEFAULT_CONNECT_TIMES);
        attachFloaters = conf.getBoolean(CONF_ATTACH_FLOATERS, DEFAULT_ATTACH_FLOATERS);
    }

    public boolean doMergeSubsequent() {
        return mergeSubsequent;
    }

    public boolean doConnectTimes() {
        return connectTimes;
    }

    public boolean doAttachFloaters() {
        return attachFloaters;
    }

    public boolean fitsDate(String date) {
        return (getFromDate() == null || getFromDate().compareTo(date) <= 0)
                && (getToDate() == null || getToDate().compareTo(date) > 0);
    }

    @Override
    public String toString() {
        return "HPAltoAnalyzerSetup(mergeSubsequent=" + mergeSubsequent + ", connectTimes=" + connectTimes
                + ", " + super.toString() + ")";
    }
}
