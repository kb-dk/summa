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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

public abstract class AltoAnalyzerSetup implements Configurable {

    /**
     * If specified, this setup will only be used for Altos with a date that is on or after the stated date.
     * </p><p>
     * ISO date (YYYYMMDD). Optional. Default is 00000101.
     * </p>
     */
    public static final String CONF_FROM_DATE = "altoanalyzersetup.date.from";
    public static final String DEFAULT_FROM_DATE = "00000101";

    /**
     * If specified, this setup will only be used for Altos with a date that is before the stated date.
     * </p><p>
     * ISO date (YYYYMMDD). Optional. Default is 99991231.
     * </p>
     */
    public static final String CONF_TO_DATE = "altoanalyzersetup.date.to";
    public static final String DEFAULT_TO_DATE = "99991231";

    /**
     * When calculating the distance between two points, the horizontal distance will be multiplied with this factor.
     * Stating a value below 1 means that vertical distance is more significant..
     * </p><p>
     * Optional. Default is 0.5.
     */
    public static final String CONF_HDIST_FACTOR = "hpaltoanalyzer.hdist.factor";
    public static final double DEFAULT_HDIST_FACTOR = 0.5d;

    private final String fromDate;
    private final String toDate;
    private final double hdistFactor;

    public AltoAnalyzerSetup(Configuration conf) {
        hdistFactor = conf.getDouble(CONF_HDIST_FACTOR, DEFAULT_HDIST_FACTOR);
        fromDate = conf.getString(CONF_FROM_DATE, DEFAULT_FROM_DATE);
        toDate = conf.getString(CONF_TO_DATE, DEFAULT_TO_DATE);
    }

    public double getHdistFactor() {
        return hdistFactor;
    }

    public boolean fitsDate(String date) {
        return (fromDate == null || fromDate.compareTo(date) <= 0) && (toDate == null || toDate.compareTo(date) > 0);
    }

    @Override
    public String toString() {
        return "AltoAnalyzerSetup(fromDate=" + fromDate + ", toDate=" + toDate + ", hdistFactor=" + hdistFactor + ')';
    }
}
