package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.PayloadMatcher;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class IdentityFilterTest {

    @Test
    public void testBasics() {
        ObjectFilter feeder = new PayloadFeederHelper(1, 1, false, "dummy_", "BaseA");
        IdentityFilter identity = new IdentityFilter(Configuration.newMemoryBased(
                ObjectFilterBase.CONF_SHOWSTATS_PROCESS_SIZE, true // To activate update counting
        ));
        identity.setSource(feeder);
        List<Payload> payloadsA = getPayloads(identity);
        assertEquals("The right number of Payloads should pass", 1, payloadsA.size());
        assertEquals("The right number of Payloads should be processed", 1, identity.getUpdateCount());
    }

    @Test
    public void testSelectiveProcessing() {
        ObjectFilter feeder = PayloadFeederHelper.createHelper(Arrays.asList(
                new Record("recA1", "baseA", new byte[0]),
                new Record("recB1", "baseB", new byte[0]),
                new Record("recA2", "baseA", new byte[0]),
                new Record("recA3", "baseA", new byte[0]),
                new Record("recB2", "baseB", new byte[0])
        ));
        IdentityFilter identity = new IdentityFilter(Configuration.newMemoryBased(
                ObjectFilterBase.CONF_SHOWSTATS_PROCESS_SIZE, true, // To activate update counting
                "objectfilter." + PayloadMatcher.CONF_BASE_REGEX, "baseB"
        ));
        identity.setSource(feeder);
        List<Payload> payloadsA = getPayloads(identity);
        assertEquals("The right number of Payloads should pass", 5, payloadsA.size());
        assertEquals("The right number of Payloads should be processed", 2, identity.getUpdateCount());
    }



    private List<Payload> getPayloads(IdentityFilter idA) {
        List<Payload> payloads = new ArrayList<>();
        while (idA.hasNext()) {
            payloads.add(idA.next());
        }
        return payloads;
    }

}