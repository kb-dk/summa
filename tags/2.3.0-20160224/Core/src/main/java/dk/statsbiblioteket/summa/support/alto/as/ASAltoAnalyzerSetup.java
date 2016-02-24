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
package dk.statsbiblioteket.summa.support.alto.as;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.alto.AltoAnalyzerSetup;

public class ASAltoAnalyzerSetup extends AltoAnalyzerSetup {

    /**
     * The maximum distance in percent of block width from the center of the block to the center of nearby TextLines in
     * order for the lines to be considered near. Assuming a circular block, setting this to < 0.5 would give 0 nearby
     * TextLines.
     */
    public static final String CONF_NEARBY_DISTANCE = "altoanalyzersetup.nearby";
    public static final double DEFAULT_NEARBY_DISTANCE = 1.0;

    private double nearbyFactor;

    public ASAltoAnalyzerSetup(Configuration conf) {
        super(conf);
        nearbyFactor = conf.getDouble(CONF_NEARBY_DISTANCE, DEFAULT_NEARBY_DISTANCE);
    }

    public boolean fitsDate(String date) {
        return (getFromDate() == null || getFromDate().compareTo(date) <= 0)
                && (getToDate() == null || getToDate().compareTo(date) > 0);
    }

    public double getNearbyFactor() {
        return nearbyFactor;
    }

    @Override
    public String toString() {
        return "ASAltoAnalyzerSetup(" + super.toString() + ")";
    }
}
