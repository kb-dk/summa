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

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.Payload;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.util.List;
import java.util.ArrayList;

public class XMLSplitterHandlerTest extends TestCase implements
                                                     XMLSplitterReceiver {
    public XMLSplitterHandlerTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        received.clear();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(XMLSplitterHandlerTest.class);
    }

    private List<Record> received = new ArrayList<Record>(10);

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void testPlainSplit() throws Exception {
        XMLSplitterParserTarget target =
                new XMLSplitterParserTarget(Configuration.newMemoryBased(
                        XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, true,
                        XMLSplitterFilter.CONF_BASE, "dummy",
                        XMLSplitterFilter.CONF_RECORD_ELEMENT, "record",
                        XMLSplitterFilter.CONF_RECORD_NAMESPACE,
                        "http://www.loc.gov/MARC21/slim",
                        XMLSplitterFilter.CONF_ID_ELEMENT, "leader",
                        XMLSplitterFilter.CONF_ID_NAMESPACE,
                        "http://www.loc.gov/MARC21/slim"
                )
        );
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        XMLSplitterHandler handler = new XMLSplitterHandler(
                Configuration.newMemoryBased(), this, target);
        SAXParser parser = factory.newSAXParser();
        handler.resetForNextStream();
        parser.setProperty(XMLSplitterParser.LEXICAL_HANDLER, handler);

        Payload payload = new Payload(Resolver.getURL(
                "data/double_default_oai.xml").openStream());
        parser.parse(payload.getStream(), handler);
        assertEquals("the right number of Records should be produced",
                     1, received.size());
        Record record = received.get(0);
        System.out.println(record.getContentAsUTF8());
    }

    public void queueRecord(Record record) {
        received.add(record);
    }

    public boolean isTerminated() {
        return false;
    }
}

