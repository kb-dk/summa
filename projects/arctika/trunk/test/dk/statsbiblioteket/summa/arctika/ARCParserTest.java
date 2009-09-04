/* $Id: ARCParserTest.java 1661 2009-08-11 14:36:02Z toke-sb $
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
package dk.statsbiblioteket.summa.arctika;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ARCParserTest extends TestCase {
    private static Log log = LogFactory.getLog(ARCParserTest.class);

    public ARCParserTest(String name) {
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
        return new TestSuite(ARCParserTest.class);
    }

    public void testSimpleARC() throws Exception {
        ObjectFilter feeder = getFeeder(Arrays.asList(
                Resolver.getFile("test/data/arc/ARC-SAMPLE-20060928223931-"
                                 + "00000-gojoblack.arc.gz")));

        Configuration arcConf = Configuration.newMemoryBased(
                StreamController.CONF_PARSER, ARCParser.class);
        StreamController ap = new StreamController(arcConf);
        ap.setSource(feeder);

        assertValidARCParse(ap, 120);
    }

    public void testUncompressedread() throws Exception {
/*        ObjectFilter feeder = getFeeder(Arrays.asList(
                Resolver.getFile("test/data/arc/17676-38-20070528072102-00008-sb-prod-har-002.statsbiblioteket.dk.arc")));*/
        ObjectFilter feeder = getFeeder(Arrays.asList(
                Resolver.getFile("test/data/arc/ARC-SAMPLE-20060928223931-"
                                 + "00000-gojoblack.arc")));

        Configuration arcConf = Configuration.newMemoryBased(
                StreamController.CONF_PARSER, ARCParser.class,
                "usefilehack", true);
        StreamController ap = new StreamController(arcConf);
        ap.setSource(feeder);

        assertValidARCParse(ap, 120);
    }

    public void testRecordExtraction() throws Exception {
        ObjectFilter feeder = getFeeder(Arrays.asList(
                Resolver.getFile("test/data/arc/ARC-SAMPLE-20060928223931-"
                                 + "00000-gojoblack.arc")));

        Configuration arcConf = Configuration.newMemoryBased(
                StreamController.CONF_PARSER, ARCParser.class,
                "usefilehack", true);
        StreamController ap = new StreamController(arcConf);
        ap.setSource(feeder);

        String WHITE = "http://www.whitehouse.gov/";
        String expected = Files.loadString(Resolver.getFile(
                "data/arc/whitehouse.gov.dat"));
        while (ap.hasNext()) {
            Payload next = ap.next();
            log.trace("Got " + next);
            if (WHITE.equals(next.getId())) {
                log.debug("Located whitehouse.gov");
                String actual = Strings.flush(next.getStream());
                log.debug("Flushed content of whitehouse.gov");
                assertEquals("whitehouse.gov should be as expected",
                             expected, actual);
                return;
            }
            next.close();
        }
        ap.close(true);
        fail(WHITE + " not found");
    }
    private void assertValidARCParse(ObjectFilter ap, int expected)
                                                              throws Exception {
        Random random = new Random();
        assertTrue("At least one Payload should be generated", ap.hasNext());
        log.debug("Iterating through ARC records");
        List<String> ids = new ArrayList<String>(100);
        while (ap.hasNext()) {
            Payload payload = ap.next();
            System.out.println("Found " + payload + " with content length "
                               + streamLength(payload));
            payload.close();
        }
        ap.close(true);
        assertEquals("The number of payloads should be as expected",
                     expected, ids.size());
    }

    private ObjectFilter getFeeder(List<File> arcs) throws IOException {
        List<Payload> payloads = new ArrayList<Payload>(arcs.size());
        for (File arc: arcs) {
            InputStream is = new FileInputStream(arc);
            Payload payloadIn = new Payload(is);
            payloadIn.getData().put(Payload.ORIGIN, arc.getAbsolutePath());
            payloadIn.setID(arc.toString());
            payloads.add(payloadIn);
        }
        return new PayloadFeederHelper(payloads);
    }

    private long streamLength(Payload payload) throws IOException {
        long length = 0;
        while (payload.getStream().read() != -1) {
            length++;
        }
        return length;
    }
}
