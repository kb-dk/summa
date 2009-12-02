/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.ingest.stream;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.marc4j.*;
import org.marc4j.marc.Record;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class ISO2709ToMARCXMLFilterTest extends TestCase {
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
        MarcWriter writer = new MarcXmlWriter(System.out, true);

        while (reader.hasNext()) {
            Record record = reader.next();
            writer.write(record);
        }
        writer.close();
    }

    public void testBasicMarc4j() throws Exception {
//        File SAMPLE = Resolver.getFile("data/iso2709/summerland.data");
        File SAMPLE = Resolver.getFile("data/iso2709/t2.data");
//        File SAMPLE = Resolver.getFile("data/iso2709/dpu20091109_sample.data");
        FileInputStream sampleIn = new FileInputStream(SAMPLE);
//        MarcStreamReader reader = new MarcStreamReader(sampleIn, "MARC-8");
//        MarcStreamReader reader = new MarcStreamReader(sampleIn, "MARC-8");
//        MarcStreamReader reader = new MarcStreamReader(sampleIn);
        MarcPermissiveStreamReader reader = new MarcPermissiveStreamReader(
                sampleIn, false, false, "Unimarc");

        int counter = 0;
        while (reader.hasNext()) {
            Record record = reader.next();
            System.out.println("Got MARC Record " + ++counter + ":\n" + record);
            dumpRecord(record);
//            dumpRecordXML(record);
            System.out.println("\nDump finished");
        }
        sampleIn.close();
    }

    private void dumpRecord(Record record) {
        System.out.println("Dumping lineformat for " + record.getId());
//        MarcWriter lineWriter = new MarcStreamWriter(System.out, "UTF-8");
        MarcWriter lineWriter = new MarcStreamWriter(System.out);
        lineWriter.write(record);
        System.out.println("\nDump finished");
        //lineWriter.close();
    }

    private void dumpRecordXML(Record record) {
        System.out.println("\n\nDumping XML for " + record.getId());
        MarcWriter writer = new MarcXmlWriter(System.out, true);

        writer.write(record);
        writer.close();
        System.out.println("Finished dumping XML for " + record.getId());
    }

    public void testDirectDump() throws Exception {
        File SAMPLE = Resolver.getFile("data/iso2709/dpu20091109_sample.data");
        FileInputStream sampleIn = new FileInputStream(SAMPLE);
        ByteOutputStream out = new ByteOutputStream((int)SAMPLE.length());
        Streams.pipe(sampleIn, out);
        System.out.println("Content of " + SAMPLE + "in ISO-8859-1 is:\n"
                           + new String(out.getBytes(), "cp850"));
    }

    public void testTransform() throws Exception {
        File SAMPLE = Resolver.getFile("data/iso2709/t2.data");
//        File SAMPLE = Resolver.getFile("data/iso2709/dpu20091109_sample.data");
//        File SAMPLE = Resolver.getFile(
//                "/home/te/projects/data/dpb/dpb20091130.data");
//        File SAMPLE = Resolver.getFile("data/iso2709/summerland.data");
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
        System.out.println("Dumping Produced content:\n" + xml);
    }

    public void testXMLDump2() throws Exception {
//        File SAMPLE = Resolver.getFile("data/iso2709/t2.data");
//        File SAMPLE = Resolver.getFile("data/iso2709/dpu20091109_sample.data");
        File SAMPLE = Resolver.getFile(
//                "/home/te/projects/data/dpb/dpb20091109");
        "/home/te/projects/data/dpb/dpb20091130.data");
//        File SAMPLE = Resolver.getFile("data/iso2709/summerland.data");
        assertNotNull("The sample file " + SAMPLE + " should exist",
                   SAMPLE.exists());
        FileInputStream sampleIn = new FileInputStream(SAMPLE);
        PayloadFeederHelper feeder = new PayloadFeederHelper(Arrays.asList(
                new Payload(sampleIn)));
        ISO2709ToMARCXMLFilter isoFilter = new ISO2709ToMARCXMLFilter(
                Configuration.newMemoryBased(
                        ISO2709ToMARCXMLFilter.CONF_INPUT_CHARSET, "iso-8859-1"));
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
        System.out.println("First 4000 UTF8:\n" + new String(head, "utf8"));
        /*
        String xml = Strings.flush(xmlPayload.getStream());
        System.out.println("Dumping Produced content:\n" + xml);*/
    }

}
