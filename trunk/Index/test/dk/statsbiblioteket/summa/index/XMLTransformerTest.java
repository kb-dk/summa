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
import java.net.MalformedURLException;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLTransformerTest extends TestCase {
    public XMLTransformerTest(String name) {
        super(name);
    }

    public static final String FAGREF_XSLT_ENTRY = "data/fagref/fagref_index.xsl";
    public static final URL xsltFagrefEntryURL = getURL(FAGREF_XSLT_ENTRY);

    public static URL getURL(String resource) {
        URL url = Resolver.getURL(resource);
        assertNotNull("The resource " + resource + " must be present",
                      url);
        return url;
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(XMLTransformerTest.class);
    }

    public void testTransformerSetup() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xsltFagrefEntryURL);
        new XMLTransformer(conf);
        // Throws exception by itself in case of error
    }

    public void testTransformerSetupWithResource() throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, FAGREF_XSLT_ENTRY);
        new XMLTransformer(conf);
        // Throws exception by itself in case of error
    }

    public void testURL() throws Exception {
        try {
            new URL("foo.bar");
            fail("The URL 'foo.bar' should not be valid");
        } catch (MalformedURLException e) {
            // Expected
        }
    }

    public static final String GURLI = "data/fagref/gurli.margrethe.xml";
    public void testTransformation() throws Exception {
        String content = Streams.getUTF8Resource(GURLI);
        Record record = new Record("fagref:gurli_margrethe", "fagref",
                                   content.getBytes("utf-8"));
        Payload payload = new Payload(record);

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xsltFagrefEntryURL);
        XMLTransformer transformer = new XMLTransformer(conf);

        transformer.processPayload(payload);
        String transformed = payload.getRecord().getContentAsUTF8();
        String[] MUST_CONTAIN = new String[]{"sortLocale", "Yetitæmning",
                                             "<shortrecord>", "boostFactor"};
        for (String must: MUST_CONTAIN) {
            assertTrue("The result must contain " + must,
                       transformed.contains(must));
        }
    }

}



