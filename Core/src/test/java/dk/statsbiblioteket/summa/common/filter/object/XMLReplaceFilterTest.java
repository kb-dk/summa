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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.ingest.split.MARCParser;
import dk.statsbiblioteket.summa.ingest.split.SBMARCParser;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.util.Timing;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class XMLReplaceFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(XMLReplaceFilterTest.class);

    public void testSpaceRemoval() throws IOException {
        PayloadFeederHelper feeder = new PayloadFeederHelper("common/marc/marc_replacement_sample.xml");
        XMLReplaceFilter replacer = new XMLReplaceFilter(Configuration.newMemoryBased(
                XMLReplaceFilter.CONF_ID_FIELDS, "001*a,002*a,002*b"
        ));
        replacer.setSource(feeder);
        assertEquals("The replaced XML should be as expected",
                     Resolver.getUTF8Content("common/marc/marc_replacement_sansspace.xml"),
                     RecordUtil.getString(replacer.next()).replace(
                             "<?xml version='1.0' encoding='UTF-8'?>", "").trim() + "\n");
    }

    // Large test to locate bottleneck. No open test corpus though, so it can only be run by Toke at Statsbiblioteket
    public void testStreamingSpaceRemoval() throws IOException {
        File SRC = Resolver.getFile("/home/te/tmp/sumfresh/sites/sbsolr/Avis_kronik_rest2.xml");
        if (SRC == null) {
            return;
        }
        Payload in = new Payload(new FileInputStream(SRC), SRC.getPath());
        ObjectFilter feeder = new PayloadFeederHelper(Collections.singletonList(in));
        XMLReplaceFilter replacer = new XMLReplaceFilter(Configuration.newMemoryBased(
                XMLReplaceFilter.CONF_ID_FIELDS, "001*a,002*a,002*c,011*a,013*a,014*a,015*a,016*a,017*a,018*a"
        ));
        replacer.setSource(feeder);
        ObjectFilter splitter = new StreamController(Configuration.newMemoryBased(
                StreamController.CONF_PARSER, SBMARCParser.class,
                MARCParser.CONF_BASE, "sb_dbc_artikelbase",
                MARCParser.CONF_ID_PREFIX, "dbc_artikelbase_"
        ));
        splitter.setSource(replacer);
        
        // For quick tests
        //splitter.setSource(feeder);
        ObjectFilter endpoint = splitter;
        
        Timing timing = new Timing("replace_and_split");
        timing.stop();
        byte[] buffer = new byte[2048];
        Payload payload = null;
        while (endpoint.hasNext()) {
            long startTime = System.nanoTime();
            payload = endpoint.next();
            if (payload.getStream() != null) {
                while (payload.getStream().read(buffer) != -1) ; // Drain the stream
            }
            timing.addNS(System.nanoTime()-startTime);
            //log.debug("Processed #" + timing.getUpdates() + " at avg " + timing.getAverageMS() + " ms/payload");
        }
        log.info("Processed " + timing.getUpdates() + " payloads at avg " + timing.getAverageNS()
                 + " ns/payload and total time " + timing.getMS() + " ms");
        if (payload != null) {
            log.info(RecordUtil.getString(payload));
        }
    }
}