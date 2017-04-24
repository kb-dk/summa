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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Timing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Convenience methods for Record stats.
 */
public class StatUtil {

    public static final String CONF_TIMING_NAME = "timing.name";
    public static final String CONF_TIMING_SUBJECT = "timing.subject";
    public static final String CONF_TIMING_UNIT = "timing.unit";
    public static final String CONF_TIMING_STATS = "timing.stats";

    public static Timing createTiming(Configuration conf, String confKeyPrefix,
                               String defaultName, String defaultSubject, String defaultUnit,
                               Timing.STATS[] defaultStats) {

        final String name = conf.getString(getKey(confKeyPrefix, CONF_TIMING_NAME), defaultName);
        final String subject = conf.getString(getKey(confKeyPrefix, CONF_TIMING_SUBJECT), defaultSubject);
        final String unit = conf.getString(getKey(confKeyPrefix, CONF_TIMING_UNIT), defaultUnit);
        final Timing.STATS[] stats = fromStrings(
                conf.getStrings(getKey(confKeyPrefix, CONF_TIMING_STATS), toStrings(defaultStats)));
        return new Timing(name, subject, unit, stats);
    }

    private static List<String> toStrings(Timing.STATS[] stats) {
        if (stats == null || stats.length == 0) {
            return null;
        }
        List<Timing.STATS> lStats = Arrays.asList(stats);
        List<String> slStats = new ArrayList<>(lStats.size());
        for (Timing.STATS stat: lStats) {
            slStats.add(stat.toString());
        }
        return slStats;
    }

    private static Timing.STATS[] fromStrings(List<String> statsStrings) {
        if (statsStrings == null || statsStrings.isEmpty()) {
            return null;
        }
        Timing.STATS[] stats = new Timing.STATS[statsStrings.size()];
        try {
            for (int i = 0; i < statsStrings.size(); i++) {
                stats[i] = Timing.STATS.valueOf(statsStrings.get(i));
            }
            return stats;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Exception while converting '" + Strings.join(statsStrings) + "' to Timing.STATS", e);
        }
    }

    private static String getKey(String confKeyPrefix, String confKey) {
        return confKeyPrefix == null ? confKey :
                confKeyPrefix.endsWith(".") ? confKeyPrefix+confKey : confKeyPrefix + "." + confKey;
    }
}
