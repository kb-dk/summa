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
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.ingest.stream.FileReader;
import dk.statsbiblioteket.summa.ingest.stream.ISO2709ToMARCXMLFilter;
import dk.statsbiblioteket.util.Strings;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

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

    public void testControlField() throws Exception {
        URL inputDir = Resolver.getURL("data/iso2709/");
        log.debug("getStreamReader: Located root " + inputDir.getFile());
        Configuration conf = Configuration.newMemoryBased();
        conf.set(FileReader.CONF_ROOT_FOLDER, inputDir.getFile());
        conf.set(FileReader.CONF_RECURSIVE, true);
        conf.set(FileReader.CONF_FILE_PATTERN, "dpu20091109_sample\\.data");
        conf.set(FileReader.CONF_COMPLETED_POSTFIX, "");
        ObjectFilter reader = new FileReader(conf);

        ObjectFilter iso2709toMARC = new ISO2709ToMARCXMLFilter(
                Configuration.newMemoryBased(
                     ISO2709ToMARCXMLFilter.CONF_INPUT_CHARSET, "cp850",
                     ISO2709ToMARCXMLFilter.CONF_FIX_CONTROLFIELDS, true));
        iso2709toMARC.setSource(reader);

        assertTrue("There should be at least one payload from iso2709_handler",
                   iso2709toMARC.hasNext());

        String content = Strings.flush(iso2709toMARC.next().getStream());
        log.info("Payload Stream:\n" + content);
        // TODO assert


/*        Configuration confMarc = Configuration.newMemoryBased();
        confMarc.set(StreamController.CONF_PARSER, SBMARCParser.class);
        confMarc.set(SBMARCParser.CONF_BASE, "foo");
        confMarc.set(SBMARCParser.CONF_ID_PREFIX, "sb:");
        ObjectFilter danMARC2 = new StreamController(confMarc);
        danMARC2.setSource(iso2709toMARC);

        assertTrue("There should be at least one payload", danMARC2.hasNext());
        Payload payload = danMARC2.next();
        System.out.println("Dumping Payload content\n"
                           + payload.getRecord().getContentAsUTF8());

        danMARC2.close(true);*/
    }

    public void testsplit() throws Exception {
        assertEquals("The number of splits should be as expected", 
                     3, "-a-a".split("-").length);
    }

    public void testSingleRecordExtraction() throws Exception {
//        String EXPECTED_ID = "sb:6661666";     // From one_book.xml, field 001*a
        String EXPECTED_ID = "sb:3319632";     // From one_book.xml, field 994*z

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

/*    public void testXMLParse() throws Exception {
        String XML =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<element>foo&#31;bar</element>\n";
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);
        XMLStreamReader reader = factory.createXMLStreamReader(
                        new ByteArrayInputStream(XML.getBytes("utf-8")));
        while (reader.hasNext()) {
                reader.next();
            if (reader.getEventType() == XMLStreamReader.CHARACTERS) {
                try {
                    reader.getTextCharacters();
                    fail("A XMLStreamException should be thrown due to &#31");
                } catch (Exception e) {
                    break; // Expected af &#31 is illegal in XML text
                }
                System.out.println("***");
                System.out.println("Got characters '" + reader.getText() + "'");
                assertEquals("The right content should be extracted",
                             "foo\u0031bar", reader.getText());
            }
        }
    }
  */
    public void testXMLWrite() throws Exception {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        StringWriter sw = new StringWriter(1000);
        XMLStreamWriter writer = factory.createXMLStreamWriter(sw);
        writer.writeStartDocument();
        writer.writeStartElement("element");
        writer.writeCharacters("foo\u0031bar");
        writer.writeEndElement();
        writer.close();
        log.info("Produced\n" + sw.toString());
        // TODO test
    }

    // TODO: Test multiple records
}

