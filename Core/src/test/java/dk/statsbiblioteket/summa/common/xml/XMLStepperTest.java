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
package dk.statsbiblioteket.summa.common.xml;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.*;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLStepperTest extends TestCase {
    private static Log log = LogFactory.getLog(XMLStepperTest.class);

    private static final String SAMPLE =
            "<foo><bar xmlns=\"http://www.example.com/bar_ns/\">"
            + "<nam:subsub xmlns:nam=\"http://example.com/subsub_ns\">content1<!-- Sub comment --></nam:subsub>"
            + "<!-- Comment --></bar>\n"
            + "<bar><subsub>content2</subsub></bar></foo>";

    private static final String DERIVED_NAMESPACE =
            "<foo xmlns=\"http://www.example.com/foo_ns/\"><bar>simple bar</bar></foo>";

    private XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    {
        xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        // No resolving of external DTDs
        xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    }
    private XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();

    // Sanity check for traversal of sub
    public void testIterateTags() throws Exception {
        XMLStreamReader xml = xmlFactory.createXMLStreamReader(new StringReader(SAMPLE));
        assertTrue("The first 'bar' should be findable", XMLStepper.findTagStart(xml, "bar"));
        xml.next();

        final AtomicInteger count = new AtomicInteger(0);
        XMLStepper.iterateTags(xml, new XMLStepper.Callback() {
            @Override
            public boolean elementStart(
                    XMLStreamReader xml, List<String> tags, String current) throws XMLStreamException {
                count.incrementAndGet();
                return false;
            }
        });
        assertEquals("Only a single content should be visited", 1, count.get());
        assertTrue("The second 'bar' should be findable", XMLStepper.findTagStart(xml, "bar"));
    }

    public void testPipePosition() throws XMLStreamException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(SAMPLE));
        assertTrue("The first 'bar' should be findable", XMLStepper.findTagStart(in, "bar"));
        XMLStepper.pipeXML(in, out, false); // until </bar>
        assertEquals("The reader should be positioned at a character tag (newline) but was positioned at "
                     + XMLUtil.eventID2String(in.getEventType()),
                     XMLStreamConstants.CHARACTERS, in.getEventType());
    }

    public void testPipeComments() throws XMLStreamException {
        final String EXPECTED =
                "<bar xmlns=\"http://www.example.com/bar_ns/\">"
                + "<nam:subsub xmlns:nam=\"http://example.com/subsub_ns\">content1<!-- Sub comment --></nam:subsub>"
                + "<!-- Comment --></bar>";
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(SAMPLE));
        assertTrue("The first 'bar' should be findable", XMLStepper.findTagStart(in, "bar"));
        assertPipe(EXPECTED, in);
    }

    // Currently there is no namespace repair functionality
    public void disabletestPipeNamespace() throws XMLStreamException {
        final String EXPECTED = "<bar xmlns=\"http://www.example.com/foo_ns/\">simple bar</bar>";
        XMLStreamReader in = xmlFactory.createXMLStreamReader(new StringReader(DERIVED_NAMESPACE));
        assertTrue("The first 'bar' should be findable", XMLStepper.findTagStart(in, "bar"));
        assertPipe(EXPECTED, in);
    }

    private void assertPipe(String expected, XMLStreamReader xml) throws XMLStreamException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
        XMLStepper.pipeXML(xml, out, false);
        log.info("Sub-XML: " + os.toString());
        assertEquals("The piper should reproduce the desired sub section of the XML",
                     expected, os.toString());
    }
}
