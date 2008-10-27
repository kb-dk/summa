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
package dk.statsbiblioteket.summa.ingest.split;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * DanMARC2Parser Tester.
 *
 * @author <Authors name>
 * @since <pre>10/24/2008</pre>
 * @version 1.0
 */
public class SBMARCParserTest extends TestCase {
    private static Log log = LogFactory.getLog(SBMARCParserTest.class);

    public SBMARCParserTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SBMARCParserTest.class);
    }

    // TODO: Test 014*z => 014*a (same for 015)
    public ObjectFilter getStreamReader() {
        URL inputDir = Resolver.getURL("data/horizon/");
        log.debug("getStreamReader: Located root " + inputDir.getFile());
        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, inputDir.getFile());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.xml");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, "");
        return new FileReader(conf);
    }

    public void testGetStreamReader() throws Exception {
        ObjectFilter reader = getStreamReader();
        assertTrue("There should be at least one file available",
                   reader.hasNext());
        Payload payload = reader.next();
        assertFalse("There should be no more files available",
                    reader.hasNext());
        assertNotNull("The Payload should have a Stream", payload.getStream());
        payload.close();
    }

    public ObjectFilter getMARCChain() throws Exception {
        ObjectFilter streamReader = getStreamReader();
        Configuration conf = Configuration.newMemoryBased();
        conf.set(StreamController.CONF_PARSER, SBMARCParser.class);
        conf.set(SBMARCParser.CONF_BASE, "foo");
        conf.set(SBMARCParser.CONF_ID_PREFIX, "sb:");
        ObjectFilter danMARC2 = new StreamController(conf);
        danMARC2.setSource(streamReader);
        return danMARC2;
    }

    public void testSingleRecordExtraction() throws Exception {
        String EXPECTED_ID = "sb:6661666";     // From one_book.xml, field 001*a
        String EXPECTED_PARENT = "sb:aParent"; // From one_book.xml, field 014*a
        // From one_book.xml, field 015
        List<String> EXPECTED_CHILDREN = Arrays.asList(
                "sb:childTwo", "sb:childThree", "sb:childSix", "sb:childFour",
                "sb:childOne", "sb:childFive");

        ObjectFilter danMARC2 = getMARCChain();
        assertTrue("There should be at least one payload available",
                   danMARC2.hasNext());
        Payload payload = danMARC2.next();
        assertFalse("There should be no more payloads available",
                    danMARC2.hasNext());
        assertEquals("The id from the Payload should be as expected",
                     EXPECTED_ID, payload.getId());
        assertEquals("The parent should be as expected",
                     EXPECTED_PARENT,
                     payload.getRecord().getParentIds().get(0));
        assertTrue("The children should be as expected. Expected "
                   + payload.getRecord().getChildIds()
                   + ", got " + EXPECTED_CHILDREN,
                   Arrays.equals(EXPECTED_CHILDREN.toArray(),
                                 payload.getRecord().getChildIds().toArray()));
        log.debug("Succesfully extracted MARC record with content\n" 
                  + payload.getRecord().getContentAsUTF8());
    }

    public void testIsBlank() throws Exception {
        assertTrue("Blanks should be blanks", MARCParser.isBlank(" \n\t\n "));
        assertFalse("Non-blanks should not be blanks", 
                    MARCParser.isBlank(" \nh\t\n "));
    }

    // TODO: Test multiple records
}
