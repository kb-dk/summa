/* $Id: Aleph2XML2Test.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:23 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.preingest;

import java.util.regex.Pattern;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.ingest.stream.Aleph2XML2;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.summa.ingest.split.SBMARCParser;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Aleph2XML2 Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "te, hal")
public class Aleph2XML2Test extends TestCase {
    private static Log log = LogFactory.getLog(Aleph2XML2Test.class);

    public Aleph2XML2Test(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public static Test suite() {
        return new TestSuite(Aleph2XML2Test.class);
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public ObjectFilter getStreamReader() {
        URL inputDir = Resolver.getURL("data/aleph/");
        log.debug("getStreamReader: Located root " + inputDir.getFile());
        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, inputDir.getFile());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, ".*\\.data");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, "");
        return new FileReader(conf);
    }

    public void testGetStreamReader() throws Exception {
        ObjectFilter reader = getStreamReader();
        assertTrue("There should be at least one file available",
                   reader.hasNext());
        Payload payload = null;
        for (int i = 0 ; i < 3; i++) {
            assertTrue("There should be more payload available",
                       reader.hasNext());
            payload = reader.next();
        }
        assertFalse("There should be no more files available",
                    reader.hasNext());
        //noinspection ConstantConditions
        assertNotNull("The Payload should have a Stream", payload.getStream());
        payload.close();
    }

    public ObjectFilter getAlephChain() throws Exception {
        ObjectFilter streamReader = getStreamReader();
        Configuration conf = Configuration.newMemoryBased();
        conf.set(StreamController.CONF_PARSER, SBMARCParser.class);
        conf.set(SBMARCParser.CONF_BASE, "foo");
        conf.set(SBMARCParser.CONF_ID_PREFIX, "aleph:");

        ObjectFilter alephConverter = new Aleph2XML2(conf);
        alephConverter.setSource(streamReader);

        ObjectFilter danMARC2 = new StreamController(conf);
        danMARC2.setSource(alephConverter);
        return danMARC2;
    }

    public void testSimplePull() throws Exception {
        ObjectFilter alephChain = getAlephChain();
        for (int i = 0 ; i < 3; i++) {
            assertTrue("There should be more payload available",
                       alephChain.hasNext());
            Payload payload = alephChain.next();
            assertNotNull("All payloads should have Records",
                          payload.getRecord());
        }
    }

    public void testFullConversion() throws Exception {
        ObjectFilter alephChain = getAlephChain();
        Payload deleted = alephChain.next();
        assertTrue("The first Record should be marked as deleted",
                   deleted.getRecord().isDeleted());
        assertEquals("The first Record should have the right ID",
                     "aleph:MAT01-000000001", deleted.getId());
        testDeleted(deleted);

        Payload deleted2 = alephChain.next();
        assertTrue("The second Record should be marked as deleted",
                   deleted2.getRecord().isDeleted());
        assertEquals("The second Record should have the right ID",
                     "aleph:GEO01-000005108", deleted2.getId());
        testDeleted(deleted2);

        Payload plain = alephChain.next();
        assertFalse("The third Record should not be marked as deleted",
                    plain.getRecord().isDeleted());
        assertEquals("The third Record should have the right ID",
                     "aleph:KEM01-000000001", plain.getId());
    }

    Pattern storeID =
            Pattern.compile("(?s).*<datafield tag=\"994\" ind1=\"0\" " +
                           "ind2=\"0\">\n<subfield code=\"z\">.+</subfield>.*");
    Pattern invalidID =
            Pattern.compile("(?s).*<datafield tag=\"994\" ind1=\"0\" " +
                   "ind2=\"0\">\n<subfield code=\"z\">-000000001</subfield>.*");

    private void testDeleted(Payload payload) throws Exception {
        assertTrue("There should be a 994.z with the ID for the record in "
                   + payload, storeID.matcher(
                payload.getRecord().getContentAsUTF8()).matches());
        assertFalse("The ID -000000001 should not be generated for " + payload,
                   invalidID.matcher(
                payload.getRecord().getContentAsUTF8()).matches());
    }

}



