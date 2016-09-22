package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.ingest.stream.StreamToContentFilter;
import dk.statsbiblioteket.util.Strings;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

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
public class EnrichWithLocationsFilterTest {

    @Test
    public void TestBasicEnrichment() throws IOException {
        ObjectFilter enricher = new EnrichWithLocationsFilter(Configuration.newMemoryBased(
                EnrichWithLocationsFilter.CONF_INPUT_FIELD, "fulltext",
                EnrichWithLocationsFilter.CONF_LOCATIONS_SOURCE, "common/filter/object/fake_locations.dat"
        ));
        String enriched = getEnrichedSample(enricher);
        {
            String expected = "<field name=\"location_name\">Westminster</field>";
            assertTrue("The produced content should contain the String '" + expected + "'\n" + enriched,
                        enriched.contains(expected));
        }
        {
            String expected = "<field name=\"location_name\">London Westminster</field>";
            assertTrue("The produced content should contain the String '" + expected + "'\n" + enriched,
                       enriched.contains(expected));
        }
    }

    @Test
    public void TestLongestEnrichment() throws IOException {
        ObjectFilter enricher = new EnrichWithLocationsFilter(Configuration.newMemoryBased(
                EnrichWithLocationsFilter.CONF_INPUT_FIELD, "fulltext",
                EnrichWithLocationsFilter.CONF_LOCATIONS_SOURCE, "common/filter/object/fake_locations.dat",
                EnrichWithLocationsFilter.CONF_MATCH_MODE, "longest"
        ));
        String enriched = getEnrichedSample(enricher);
        {
            String expected = "<field name=\"location_name\">Westminster</field>";
            assertFalse("The produced content should not contain the String '" + expected + "'\n" + enriched,
                        enriched.contains(expected));
        }
        {
            String expected = "<field name=\"location_name\">London Westminster</field>";
            assertTrue("The produced content should contain the String '" + expected + "'\n" + enriched,
                       enriched.contains(expected));
        }
    }

    private String getEnrichedSample(ObjectFilter enricher) throws IOException {
        ObjectFilter feeder = new PayloadFeederHelper("common/filter/object/fake_solr_document.xml");
        ObjectFilter toContent = new StreamToContentFilter(Configuration.newMemoryBased(
                StreamToContentFilter.CONF_BASE, "dummy"
        ));
        toContent.setSource(feeder);
        enricher.setSource(toContent);
        return enricher.next().getRecord().getContentAsUTF8();
    }

}