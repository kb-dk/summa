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

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ReduceXMLFilterTest extends TestCase {
    public static final String HEADER = "<?xml version='1.0' encoding='UTF8'?>";
    public static final String SAMPLE = HEADER
            + "<foo><bar zoo=\"true\"></bar><bar zoo=\"true\"></bar><bar zoo=\"false\"></bar><baz /></foo>";

    public void testLimitXMLSimple() throws XMLStreamException, UnsupportedEncodingException {
        assertLimit(SAMPLE, "<foo><baz /></foo>", true, false,
                    "/foo/bar", 0);
        assertLimit(SAMPLE, "<foo><bar zoo=\"true\" /><baz /></foo>", true, false,
                    "/foo/bar", 1);
        assertLimit(SAMPLE, "<foo><bar zoo=\"true\" /><bar zoo=\"true\" /><baz /></foo>", true, false,
                    "/foo/bar", 2);
    }

    public void testLimitPositiveList() throws XMLStreamException, UnsupportedEncodingException {
        assertLimit(SAMPLE, "<foo><bar zoo=\"true\" /></foo>", true, true,
                    "/foo$", -1, "/foo/bar", 1);
        assertLimit(SAMPLE, "<foo><baz /></foo>", true, true,
                    "/foo$", -1, "/foo/baz", 1);
    }

    public void testLimitXMLAttribute() throws XMLStreamException, UnsupportedEncodingException {
        assertLimit(SAMPLE, "<foo><bar zoo=\"false\" /><baz /></foo>", false, false,
                    "/foo/bar#zoo=true", 0);
        assertLimit(SAMPLE, "<foo><bar zoo=\"true\" /><bar zoo=\"false\" /><baz /></foo>", false, false,
                    "/foo/bar#zoo=true", 1);
    }

    private void assertLimit(String input, String expected, boolean onlyElementMatch, boolean discardNonMatched,
                             Object... limits) throws XMLStreamException, UnsupportedEncodingException {
        if (!isCollapsing) {
            expected = expected.replaceAll("<([^>]+)([^>]*)/>", "<$1$2></$1>");
        }
        ArrayList<String> lims = new ArrayList<String>(limits.length / 2);
        for (int i = 0 ; i < limits.length ; i+=2) {
            lims.add(limits[i + 1] + " " + limits[i]);
        }
        ObjectFilter feeder = new PayloadFeederHelper(Arrays.asList(new Payload(
                new Record("sample", "dummy", input.getBytes("utf-8")))));
        ObjectFilter reducer = new ReduceXMLFilter(Configuration.newMemoryBased(
                ReduceXMLFilter.CONF_LIMITS, lims,
                ReduceXMLFilter.CONF_ONLY_CHECK_ELEMENT_PATHS, onlyElementMatch,
                ReduceXMLFilter.CONF_DISCARD_NONMATCHED, discardNonMatched));
        reducer.setSource(feeder);
        Payload processed = reducer.next();
        assertEquals("The payload should be reduced properly for limits " + Strings.join(limits),
                     HEADER + expected, processed.getRecord().getContentAsUTF8());
    }

    private final boolean isCollapsing = writerIsCollapsing();
    @SuppressWarnings("CallToPrintStackTrace")
    private synchronized boolean writerIsCollapsing() {
        XMLOutputFactory xmlOutFactory = XMLOutputFactory.newInstance();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            XMLStreamWriter out = xmlOutFactory.createXMLStreamWriter(os);
            out.writeStartElement("foo");
            out.writeEndElement();
            out.flush();
            return "<foo />".equals(os.toString());
        } catch (XMLStreamException e) {
            throw new RuntimeException("Unable to determine if XMLStreamWriter collapses empty elements", e);
        }
    }
}
