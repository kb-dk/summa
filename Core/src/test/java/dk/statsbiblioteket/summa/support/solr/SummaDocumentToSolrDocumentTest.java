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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.support.embeddedsolr.EmbeddedJettyWithSolrServer;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.Arrays;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaDocumentToSolrDocumentTest extends TestCase {
    private static Log log = LogFactory.getLog(SummaDocumentToSolrDocumentTest.class);

    public SummaDocumentToSolrDocumentTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SummaDocumentToSolrDocumentTest.class);
    }

    public void testBasicTransform() throws IOException {
        String content = transform("support/solr/SummaDocumentSample1.xml");
        assertTrue("The result should be a Solr doc\n" + content, content.contains("<doc>"));
        assertTrue("The result should contain the right ID\n" + content,
                   // doc1 overrides mybase:grimme_aellinger
                   content.contains("<field name=\"recordId\">doc1</field>"));
        assertTrue("The result should contain the right base\n" + content,
                   content.contains("<field name=\"recordBase\">dummy</field>"));
        log.debug("\n" + content);
    }

    public void testExplicitNamespace() throws IOException {
        String content = transform("support/solr/SummaDocumentSampleNamespace.xml");
        assertTrue("The result should be a Solr doc\n" + content, content.contains("<doc>"));
        assertTrue("The result should contain the right ID\n" + content,
                   // doc1 overrides mybase:grimme_aellinger
                   content.contains("<field name=\"recordId\">doc1</field>"));
        assertTrue("The result should contain the right base\n" + content,
                   content.contains("<field name=\"recordBase\">dummy</field>"));
        log.debug("\n" + content);
    }

    public void testEmptyElimination() throws IOException {
        String content = transform("support/solr/SummaDocumentSampleEmpty.xml");
        assertFalse("The result should not contain the empty field\n" + content,
                    content.contains("<field name=\"empty\""));
        log.debug("\n" + content);
    }

    public void testExtraRecordBase() throws IOException {
        String content = transform("support/solr/SummaDocumentSampleRecordBase.xml");
        assertFalse("The result should not contain the wrong base\n" + content,
                   content.contains("<field name=\"recordBase\">wrong_base</field>"));
        assertTrue("The result should contain the right base\n" + content,
                   content.contains("<field name=\"recordBase\">dummy</field>"));
        log.debug("\n" + content);
    }

    private String transform(String path) throws IOException {
        return transform(path, "doc1");
    }
    private String transform(String path, String recordId) throws IOException {
        Payload summaDoc = new Payload(new Record(
            recordId, "dummy", Resolver.getUTF8Content(path).getBytes()));
        ObjectFilter input = new PayloadFeederHelper(Arrays.asList(summaDoc));

        XMLTransformer transformer = new XMLTransformer(Configuration.newMemoryBased(
            XMLTransformer.CONF_STRIP_XML_NAMESPACES, false,
            XMLTransformer.CONF_XSLT, "SummaDocumentToSolrDocument.xslt"
        ));
        transformer.setSource(input);
        Payload solrDoc = transformer.next();
        return solrDoc.getRecord().getContentAsUTF8();
    }
}
