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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.ingest.split.StreamController;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class ZIPParserTest extends TestCase {
    private static Log log = LogFactory.getLog(ZIPParserTest.class);

    public ZIPParserTest(String name) {
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
        return new TestSuite(ZIPParserTest.class);
    }

    // TODO: Test timeout with missing close

    // Provokes race condition
    public void testRepeatedBasics() throws Exception {
        for (int i = 0 ; i < 10 ; i++) {
            testBasics();
        }
    }

    public void testBasics() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(StreamController.CONF_PARSER, ZIPParser.class);
        StreamController unzipper = new StreamController(conf);

        PayloadFeederHelper feeder = new PayloadFeederHelper(
                Arrays.asList(new Payload(Resolver.getURL(
                        "data/pull/myzip.zip").openStream()),
                              new Payload(Resolver.getURL(
                        "data/pull/myzip2.zip").openStream())));
        unzipper.setSource(feeder);

        assertTrue("The unzipper should have at least one element",
                   unzipper.hasNext());

        List<String> expected = Arrays.asList(
                "foo", "flam", "kaboom", "zoo", "zoo2");
        int received = 0;
        while (unzipper.hasNext()) {
            Payload payload = unzipper.next();
            received++;
            String origin = payload.getData(Payload.ORIGIN).toString();
            log.debug("Received Payload with origin " + origin);
            String entryName = origin.contains("/") ?
                               origin.substring(origin.lastIndexOf("/") + 1) :
                               origin;
            entryName = entryName.substring(0, entryName.lastIndexOf("."));
            assertEquals("The entry name should match the content",
                         entryName, 
                         PullParserTest.getStreamContent(payload).trim());
            payload.close();
        }
        assertEquals("The number of processed Payloads should be correct",
                     expected.size(), received);
        unzipper.close(true);
    }

}
