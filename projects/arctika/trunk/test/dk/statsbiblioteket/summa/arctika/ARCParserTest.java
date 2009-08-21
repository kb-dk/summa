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

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
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
        File source = Resolver.getFile(
                "data/arc/ARC-SAMPLE-20060928223931-00000-gojoblack.arc.gz");
        InputStream is = new FileInputStream(source);
        Payload payloadIn = new Payload(is);
        payloadIn.setID(source.toString());
        PayloadFeederHelper feeder =
                new PayloadFeederHelper(Arrays.asList(payloadIn));

        Configuration arcConf = Configuration.newMemoryBased(
                StreamController.CONF_PARSER, ARCParser.class);
        StreamController ap = new StreamController(arcConf);
        ap.setSource(feeder);
        
        assertTrue("At least one Payload should be generated", ap.hasNext());
        log.debug("Iterating through ARC records");
        while (ap.hasNext()) {
            Payload payload = ap.next();
            System.out.println("Found " + payload + " with content length "
                               + streamLength(payload));
            payload.close();
        }
        ap.close(true);
    }

    private long streamLength(Payload payload) throws IOException {
        long length = 0;
        while (payload.getStream().read() != -1) {
            length++;
        }
        return length;
    }
}
