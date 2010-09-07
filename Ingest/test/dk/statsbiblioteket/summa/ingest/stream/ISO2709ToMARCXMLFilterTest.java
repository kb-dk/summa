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
package dk.statsbiblioteket.summa.ingest.stream;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.split.MARCParser;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class ISO2709ToMARCXMLFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(ISO2709ToMARCXMLFilterTest.class);
    public ISO2709ToMARCXMLFilterTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(ISO2709ToMARCXMLFilterTest.class);
    }

    public void testXMLDump() throws Exception {
        // Taken from
        // http://marc4j.tigris.org/files/documents/220/33576/tutorial.html

        File SAMPLE = Resolver.getFile("data/iso2709/summerland.data");

        InputStream input = new FileInputStream(SAMPLE);

        MarcReader reader = new MarcStreamReader(input);
        MarcWriter writer = new MarcXmlWriter(new ByteArrayOutputStream(), true);
        //MarcWriter writer = new MarcXmlWriter(System.out, true);

        while (reader.hasNext()) {
            Record record = reader.next();
            writer.write(record);
        }
        writer.close();
    }

    public void testBasicMarc4j() throws Exception {
        File SAMPLE = Resolver.getFile("data/iso2709/summerland.data");
        //File SAMPLE = Resolver.getFile("data/iso2709/t2.data");
        //File SAMPLE = Resolver.getFile("data/iso2709/dpu20091109_sample.data");
        FileInputStream sampleIn = new FileInputStream(SAMPLE);
        //MarcStreamReader reader = new MarcStreamReader(sampleIn, "MARC-8");
        //MarcStreamReader reader = new MarcStreamReader(sampleIn, "MARC-8");
        //MarcStreamReader reader = new MarcStreamReader(sampleIn);
        MarcPermissiveStreamReader reader = new MarcPermissiveStreamReader(
                sampleIn, false, false, "Unimarc");

        int counter = 0;
        while (reader.hasNext()) {
            Record record = reader.next();
            log.info("Got MARC Record " + ++counter + ":\n" + record);
            dumpRecord(record);
            //dumpRecordXML(record);
            log.info("\nDump finished");
        }
        sampleIn.close();
    }

    private void dumpRecord(Record record) {
        log.info("Dumping lineformat for " + record.getId());
        //MarcWriter lineWriter = new MarcStreamWriter(System.out, "UTF-8");
        //MarcWriter lineWriter = new MarcStreamWriter(System.out);
        MarcWriter lineWriter = new MarcStreamWriter(null);
        lineWriter.write(record);
        log.info("\nDump finished");
        //lineWriter.close();
    }

    private void dumpRecordXML(Record record) {
        log.info("\n\nDumping XML for " + record.getId());
        //MarcWriter writer = new MarcXmlWriter(System.out, true);
        MarcWriter writer = new MarcXmlWriter(new ByteArrayOutputStream(), true);

        writer.write(record);
        writer.close();
        log.info("Finished dumping XML for " + record.getId());
    }

    public void testDirectDump() throws Exception {
        File SAMPLE = Resolver.getFile("data/iso2709/dpu20091109_sample.data");
        FileInputStream sampleIn = new FileInputStream(SAMPLE);
        //ByteOutputStream out = new ByteOutputStream((int)SAMPLE.length());
        ByteArrayOutputStream out =
                                new ByteArrayOutputStream((int)SAMPLE.length());
        Streams.pipe(sampleIn, out);
        log.info("Content of " + SAMPLE + "in ISO-8859-1 is:\n"
                           + new String(out.toByteArray(), "cp850"));
    }

    public void testTransform() throws Exception {
       // File SAMPLE = Resolver.getFile("data/iso2709/t2.data");
//        File SAMPLE = Resolver.getFile("data/iso2709/dpu20091109_sample.data");
//        File SAMPLE = Resolver.getFile(
//                "/home/te/projects/data/dpb/dpb20091130.data");
        File SAMPLE = Resolver.getFile("data/iso2709/summerland.data");
        assertTrue("The sample file " + SAMPLE + " should exist",
                   SAMPLE.exists());
        FileInputStream sampleIn = new FileInputStream(SAMPLE);
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
                new Payload(sampleIn)));
        ISO2709ToMARCXMLFilter isoFilter = new ISO2709ToMARCXMLFilter(
                Configuration.newMemoryBased(
                        ISO2709ToMARCXMLFilter.CONF_INPUT_CHARSET, "cp850"));
        isoFilter.setSource(feeder);
        ArrayList<Payload> processed = new ArrayList<Payload>(3);

        while (isoFilter.hasNext()) {
            processed.add(isoFilter.next());
        }
        assertEquals("The right amount of Payloads should be produced",
                     1, processed.size());
        Payload xmlPayload = processed.get(0);
        String xml = Strings.flush(xmlPayload.getStream());
        log.info("Dumping Produced content:\n" + xml);
    }

    public void testXMLDump2() throws Exception {
//        File SAMPLE = Resolver.getFile("data/iso2709/t2.data");
//        File SAMPLE = Resolver.getFile("data/iso2709/dpu20091109_sample.data");
//        File SAMPLE = Resolver.getFile(
//                "/home/te/projects/data/dpb/dpb20091109");
//        "/home/te/projects/data/dpb/dpb20091130.data");
        File SAMPLE = Resolver.getFile("data/iso2709/summerland.data");
//        File SAMPLE = Resolver.getFile("data/iso2709/TOTALWEB4.data");
        assertNotNull("The sample file " + SAMPLE + " should exist",
                   SAMPLE.exists());
        FileInputStream sampleIn = new FileInputStream(SAMPLE);
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
                new Payload(sampleIn)));
        ISO2709ToMARCXMLFilter isoFilter = new ISO2709ToMARCXMLFilter(
                Configuration.newMemoryBased(
                        ISO2709ToMARCXMLFilter.CONF_INPUT_CHARSET, "cp850",
//                        ISO2709ToMARCXMLFilter.CONF_INPUT_CHARSET, "ISO-8859-1",
                        ISO2709ToMARCXMLFilter.CONF_FIX_CONTROLFIELDS, true));
        isoFilter.setSource(feeder);
        ArrayList<Payload> processed = new ArrayList<Payload>(3);

        while (isoFilter.hasNext()) {
            processed.add(isoFilter.next());
        }
        assertEquals("The right amount of Payloads should be produced",
                     1, processed.size());
        Payload xmlPayload = processed.get(0);

        byte[] head = new byte[4000];
        assertTrue("Stream should contain something",
                   xmlPayload.getStream().read(head) > 0);
        log.info("First 4000 UTF8:\n" + new String(head, "utf8"));
        log.info("\nCounting lines with '<record>'");

        BufferedReader br = new BufferedReader(new InputStreamReader(
                xmlPayload.getStream(), "utf-8"));
        String line;
        int recordCount = 0;
        try {
            while ((line = br.readLine()) != null) {
                if (line.contains("<record>")) {
                    recordCount++;
                }
            }
        } catch (Exception e) {
            log.info("Got an Exception while counting:");
            fail("Got an Exception while counting:");
        }
        log.info("Found " + recordCount + " '<record>' after the "
                           + "first 4000 bytes");
        xmlPayload.close();
        /*
        String xml = Strings.flush(xmlPayload.getStream());
        log.info("Dumping Produced content:\n" + xml);*/
    }

    public void testChain() throws Exception {
//        File SAMPLE = Resolver.getFile("data/iso2709/TOTALWEB4.data");
        File SAMPLE = Resolver.getFile("data/iso2709/summerland.data");
        assertNotNull("The sample file " + SAMPLE + " should exist",
                   SAMPLE.exists());
        FileInputStream sampleIn = new FileInputStream(SAMPLE);
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
                new Payload(sampleIn)));
        ISO2709ToMARCXMLFilter isoFilter = new ISO2709ToMARCXMLFilter(
                Configuration.newMemoryBased(
//                        ISO2709ToMARCXMLFilter.CONF_INPUT_CHARSET, "ISO-8859-1",
                        ISO2709ToMARCXMLFilter.CONF_INPUT_CHARSET, "cp850",
                        ISO2709ToMARCXMLFilter.CONF_FIX_CONTROLFIELDS, true));
        isoFilter.setSource(feeder);
        StreamController streamer = new StreamController(
                Configuration.newMemoryBased(
                        StreamController.CONF_PARSER, 
                        "dk.statsbiblioteket.summa.ingest.split.SBMARCParser",
                        MARCParser.CONF_BASE, "sb_dpb",
                        MARCParser.CONF_ID_PREFIX, "dpb"
                ));

        streamer.setSource(isoFilter);

        ArrayList<Payload> processed = new ArrayList<Payload>(3);

        while (streamer.hasNext()) {
            processed.add(streamer.next());
        }
        log.info("Got a total of " + processed.size() + " Payloads");
    }
}