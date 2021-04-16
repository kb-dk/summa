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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.PayloadException;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * The old style index format for Summa must be transformed to SummaDocumentXML
 * before indexing into a concrete index can be performed. The transformation
 * is the simple XSLT LegacyToSummaDocumentXML.xslt, which is tested here.
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LegacyTest extends TestCase {
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
        Record record = new Record(id, "dummy", content.getBytes(StandardCharsets.UTF_8));
        Payload payload = new Payload(record);

        Configuration conf = Configuration.newMemoryBased();
        conf.set(XMLTransformer.CONF_XSLT, xslt);
        conf.set(XMLTransformer.CONF_STRIP_XML_NAMESPACES, false);
        OpenTransformer transformer = new OpenTransformer(conf);

        transformer.process(payload);
        return payload.getRecord().getContentAsUTF8();
    }
    private class OpenTransformer extends XMLTransformer {
         private OpenTransformer(Configuration conf) {
             super(conf);
         }
         public boolean process(Payload payload) throws PayloadException {
             return processPayload(payload);
         }
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
        String OLD_JENS = "index/fagref/jens.hansen.oldstyle.xml";
        String EXPECTED_LOC = "index/fagref/jens.hansen.newstyle.xml";
        URL xslt = XMLTransformerTest.getURL("LegacyToSummaDocumentXML.xslt");

        String transformed = transform(OLD_JENS, "fagref:jens.hansen",
                                       xslt);
        String expected = Streams.getUTF8Resource(EXPECTED_LOC);
        // TODO: We need a proper XML comparator
        assertEquals("The new style document should be as expected",
                     expected.trim(), transformed.trim());
    }
}