/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.index;

import java.net.URL;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import junit.framework.TestCase;

/**
 * The old style index format for Summa must be transformed to SummaDocumentXML
 * before indexing into a concrete index can be performed. The transformation
 * is the simple XSLT LegacyToSummaDocumentXML.xslt, which is tested here.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LegacyTest  extends TestCase {
    public LegacyTest(String name) {
        super(name);
    }

    /*
     * Converts xmlResource using the given XSLT, creating a Record with the
     * given id.
     */
    public String transform(String xmlResource, String id, URL xslt) throws
                                                                     Exception {
        String content = Streams.getUTF8Resource(xmlResource);
        Record record = new Record(id, "dummy", content.getBytes("utf-8"));
        Payload payload = new Payload(record);

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xslt);
        conf.set(XMLTransformer.CONF_STRIP_XML_NAMESPACES, false);
        XMLTransformer transformer = new XMLTransformer(conf);

        transformer.processPayload(payload);
        return payload.getRecord().getContentAsUTF8();
    }
/*
    public void testMultiVolume() throws Exception {
        String CONCATENATED = "data/horizon/oldConcatenated.xml";
        String EXPLICIT = "data/horizon/newExplicit.xml";
        URL xslt = XMLTransformerTest.getURL("LegacyMultiVolumeConverter.xslt");

        String transformed = transform(EXPLICIT, "horizon_multi", xslt);
        String expected = Streams.getUTF8Resource(CONCATENATED);
        // TODO: We need a proper XML comparator
        assertEquals("The new style document should be as expected",
                     expected, transformed);
    }*/

    public void testFagref() throws Exception {
        String OLD_JENS = "data/fagref/jens.hansen.oldstyle.xml";
        String EXPECTED_LOC = "data/fagref/jens.hansen.newstyle.xml";
        URL xslt = XMLTransformerTest.getURL("LegacyToSummaDocumentXML.xslt");

        String transformed = transform(OLD_JENS, "fagref:jens.hansen",
                                       xslt);
        String expected = Streams.getUTF8Resource(EXPECTED_LOC);
        // TODO: We need a proper XML comparator
        assertEquals("The new style document should be as expected",
                     expected.trim(), transformed.trim());
    }
}



