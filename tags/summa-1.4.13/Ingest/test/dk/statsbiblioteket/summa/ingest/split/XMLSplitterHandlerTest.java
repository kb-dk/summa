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
