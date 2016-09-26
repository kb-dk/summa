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
import java.util.regex.Pattern;

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
    public void testBasicEnrichment() throws IOException {
        ObjectFilter enricher = new EnrichWithLocationsFilter(Configuration.newMemoryBased(
                EnrichWithLocationsFilter.CONF_INPUT_FIELD, "fulltext",
                EnrichWithLocationsFilter.CONF_LOCATIONS_SOURCE, "common/filter/object/fake_locations.dat"
        ));
        assertEnriched(enricher, "Westminster", "London Westminster");
    }

    @Test
    public void testLongestEnrichment() throws IOException {
        ObjectFilter enricher = new EnrichWithLocationsFilter(Configuration.newMemoryBased(
                EnrichWithLocationsFilter.CONF_INPUT_FIELD, "fulltext",
                EnrichWithLocationsFilter.CONF_LOCATIONS_SOURCE, "common/filter/object/fake_locations.dat",
                EnrichWithLocationsFilter.CONF_MATCH_MODE, "longest"
        ));
        assertNotEnriched(enricher, "Westminster", "Aarhus");
        assertEnriched(enricher, "London Westminster", "Somewhere", "Aarhus Ø");
    }

    @Test
    public void testIsolation() throws IOException {
        ObjectFilter enricher = new EnrichWithLocationsFilter(Configuration.newMemoryBased(
                EnrichWithLocationsFilter.CONF_INPUT_FIELD, "fulltext",
                EnrichWithLocationsFilter.CONF_LOCATIONS_SOURCE, "common/filter/object/fake_locations.dat",
                EnrichWithLocationsFilter.CONF_MATCH_SPACE_POSTFIX, true
        ));
        assertNotEnriched(enricher, "Somewhere");
    }

    @Test
    public void testNoGenitive() throws IOException {
        ObjectFilter enricher = new EnrichWithLocationsFilter(Configuration.newMemoryBased(
                EnrichWithLocationsFilter.CONF_INPUT_FIELD, "fulltext",
                EnrichWithLocationsFilter.CONF_LOCATIONS_SOURCE, "common/filter/object/fake_locations.dat",
                EnrichWithLocationsFilter.CONF_MATCH_SPACE_PREFIX, true,
                EnrichWithLocationsFilter.CONF_MATCH_SPACE_POSTFIX, true,
                EnrichWithLocationsFilter.CONF_MATCH_MODE, "all"
        ));
        assertNotEnriched(enricher, "København");
        assertEnriched(enricher, "Aarhus Ø", "Københavns Have", "Aarhus");
    }

    @Test
    public void testGenitive() throws IOException {
        ObjectFilter enricher = new EnrichWithLocationsFilter(Configuration.newMemoryBased(
                EnrichWithLocationsFilter.CONF_INPUT_FIELD, "fulltext",
                EnrichWithLocationsFilter.CONF_INPUT_DANISH_GENITIVE, true,
                EnrichWithLocationsFilter.CONF_LOCATIONS_SOURCE, "common/filter/object/fake_locations.dat",
                EnrichWithLocationsFilter.CONF_MATCH_SPACE_PREFIX, true,
                EnrichWithLocationsFilter.CONF_MATCH_SPACE_POSTFIX, true,
                EnrichWithLocationsFilter.CONF_MATCH_MODE, "all"
        ));
        assertEnriched(enricher, "Aarhus Ø", "Københavns Have", "København", "Aarhus");
    }

    @Test
    public void testGenitiveLongest() throws IOException {
        ObjectFilter enricher = new EnrichWithLocationsFilter(Configuration.newMemoryBased(
                EnrichWithLocationsFilter.CONF_INPUT_FIELD, "fulltext",
                EnrichWithLocationsFilter.CONF_INPUT_DANISH_GENITIVE, true,
                EnrichWithLocationsFilter.CONF_LOCATIONS_SOURCE, "common/filter/object/fake_locations.dat",
                EnrichWithLocationsFilter.CONF_MATCH_SPACE_PREFIX, true,
                EnrichWithLocationsFilter.CONF_MATCH_SPACE_POSTFIX, true,
                EnrichWithLocationsFilter.CONF_MATCH_MODE, "longest"
        ));
        assertNotEnriched(enricher, "København", "Aarhus");
        assertEnriched(enricher, "Aarhus Ø", "Københavns Have");
    }

    @Test
    public void testCleanAndIsolate() throws IOException {
        Pattern clean = Pattern.compile("[^\\p{IsAlphabetic}]+");
        ObjectFilter enricher = new EnrichWithLocationsFilter(Configuration.newMemoryBased(
                EnrichWithLocationsFilter.CONF_INPUT_FIELD, "fulltext",
                EnrichWithLocationsFilter.CONF_LOCATIONS_SOURCE, "common/filter/object/fake_locations.dat",
                EnrichWithLocationsFilter.CONF_INPUT_CLEAN_REGEXP, clean.pattern(),
                EnrichWithLocationsFilter.CONF_MATCH_SPACE_POSTFIX, true
        ));
        assertEnriched(enricher, "Westminster");
        assertEnriched(enricher, "Somewhere");
        assertEnriched(enricher, "Ærø");
    }

    private void assertEnriched(ObjectFilter enricher, String... fieldContents) throws IOException {
        String enriched = getEnrichedSample(enricher);
        for (String content: fieldContents) {
            String expected = "<field name=\"location_name\">" + content + "</field>";
            assertTrue("The produced content should contain a field with '" + content + "'\n" + enriched,
                       enriched.contains(expected));
        }
    }

    private void assertNotEnriched(ObjectFilter enricher, String... fieldContents) throws IOException {
        String enriched = getEnrichedSample(enricher);
        for (String content: fieldContents) {
            String expected = "<field name=\"location_name\">" + content + "</field>";
            assertFalse("The produced content should not contain a field '" + content + "'\n" + enriched,
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
        try {
            return enricher.next().getRecord().getContentAsUTF8();
        } finally {
            enricher.close(true);
        }
    }

}