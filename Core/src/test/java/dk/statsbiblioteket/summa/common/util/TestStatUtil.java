package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.Timing;
import org.junit.Test;

import static org.junit.Assert.*;

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
public class TestStatUtil {

    @Test
    public void testConfigStats() {
        Configuration conf = Configuration.newMemoryBased(
                "foo." + StatUtil.CONF_TIMING_STATS, "name, utilization"
        );
        Timing timing = StatUtil.createTiming(conf, "foo", "bar", null, "zoo", null);
        timing.addMS(100);
        System.out.println(timing.toString());
    }

}