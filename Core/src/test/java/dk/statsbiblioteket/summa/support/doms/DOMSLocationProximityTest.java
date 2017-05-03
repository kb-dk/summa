package dk.statsbiblioteket.summa.support.doms;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
public class DOMSLocationProximityTest {
    private static Log log = LogFactory.getLog(DOMSLocationProximityTest.class);

    public static final String LOCATIONS = "common/filter/object/alto_tuned_fake_locations.dat";

    @Test
    public void testBasic() throws IOException {
        ObjectFilter proximiter = getSplitter(new File(DOMSNewspaperParserTest.OLD_ALTO));
        List<Payload> enriched = new ArrayList<>();
        while (proximiter.hasNext()) {
            enriched.add(proximiter.next());
            log.debug("Got Payload " + enriched.get(enriched.size()-1).getId());
        }
        assertFalse("At least 1 payload should be produced", enriched.isEmpty());
 //       assertEquals("The right number of Payloads should be produced", 8, enriched.size());
        final String content = enriched.get(0).getRecord().getContentAsUTF8();
        assertTrue("Annulus 1 should contain the word 'behagelig'",
                   Pattern.compile("<annulus_1>.*behagelig.*</annulus_1>").matcher(content).find());
        assertTrue("Annulus 2 should contain the word 'Dage'",
                   Pattern.compile("<annulus_2>.*Dage.*</annulus_2>").matcher(content).find());
        assertTrue("Annulus 3 should contain the word 'Tirsdag'",
                   Pattern.compile("<annulus_3>.*Tirsdag.*</annulus_3>").matcher(content).find());
        assertTrue("Annulus 4 should contain the word 'Sukker'",
                   Pattern.compile("<annulus_4>.*Sukker.*</annulus_4>").matcher(content).find());
        //System.out.println(content);
    }

    private ObjectFilter getSplitter(File altofile) throws IOException {
        PayloadFeederHelper source = new PayloadFeederHelper(0, altofile.toString());
        ObjectFilter splitter = new StreamController(Configuration.newMemoryBased(
                StreamController.CONF_PARSER, DOMSLocationProximity.class.getCanonicalName(),
                LocationMatcher.CONF_LOCATIONS_SOURCE, LOCATIONS,
                DOMSLocationProximity.CONF_CONTENT_RADII, "500, 1000, 1500, 2000"
        ));
        splitter.setSource(source);
        return splitter;
    }
}