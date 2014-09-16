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
package dk.statsbiblioteket.summa.support.doms;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.GraphFilter;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.common.util.PayloadMatcher;
import dk.statsbiblioteket.summa.common.util.RecordUtil;
import dk.statsbiblioteket.summa.index.XMLTransformer;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DOMSNewspaperParserTest extends TestCase {
    private static Log log = LogFactory.getLog(DOMSNewspaperParserTest.class);
    private final File DOMS_ALTO = new File("/home/te/tmp/sumfresh/sites/aviser/avis_4f23.xml");
    private final int EXPECTED_SEGMENTS = 12;
    private final File DOMS_XSLT = new File("/home/te/tmp/sumfresh/sites/aviser/xslt/index/aviser/doms_aviser.xsl");


    public void testSegmenter() throws Exception {
        if (!DOMS_ALTO.exists()) {
            log.info("Cannon run test as '" + DOMS_ALTO + "' does not exist");
            return;
        }

        ObjectFilter splitter = getSplitter();
        assertTrue("There should be a Record available", splitter.hasNext());
        int counter = 0;
        while (splitter.hasNext()) {
            Payload payload = splitter.next();
            log.info("Extracted " + payload.getId());
            System.out.println(RecordUtil.getString(payload));
            counter++;
        }
        assertEquals("Test record should be split into the right number of segments", EXPECTED_SEGMENTS, counter);
    }

    public void testTransformer() throws Exception {
        if (!DOMS_ALTO.exists()) {
            log.info("Cannon run test as '" + DOMS_ALTO + "' does not exist");
            return;
        }
        if (!DOMS_XSLT.exists()) {
            log.info("Cannon run test as '" + DOMS_XSLT + "' does not exist");
            return;
        }

        ObjectFilter transformer = getTransformer();
        assertTrue("There should be a Record available", transformer.hasNext());
        int counter = 0;
        while (transformer.hasNext()) {
            Payload payload = transformer.next();
            log.info("Extracted " + payload.getId());
            assertTrue("The transformed record should contain the field 'fulltext'",
                       RecordUtil.getString(payload).contains("fulltext"));
            System.out.println(RecordUtil.getString(payload));
            counter++;
        }
        assertEquals("Test record should be split into the right number of segments", EXPECTED_SEGMENTS, counter);
    }

    private ObjectFilter getTransformer() throws IOException {
        ObjectFilter splitter = getSplitter();
        Configuration conf = Configuration.newMemoryBased(
                GraphFilter.CONF_SUCCESS_REQUIREMENT, GraphFilter.REQUIREMENT.origin,
                GraphFilter.CONF_VISIT_CHILDREN, false);
        Configuration subConf = conf.createSubConfigurations(XMLTransformer.CONF_SETUPS, 1).get(0);
        subConf.set(XMLTransformer.CONF_XSLT, DOMS_XSLT);
        subConf.set(XMLTransformer.CONF_STRIP_XML_NAMESPACES, false);
        subConf.set(PayloadMatcher.CONF_BASE_REGEX, "aviser");
        ObjectFilter transformer = new XMLTransformer(conf);
        transformer.setSource(splitter);
        return transformer;
    }

    private ObjectFilter getSplitter() throws IOException {
        ObjectFilter source = new PayloadFeederHelper(0, DOMS_ALTO.toString());
        ObjectFilter splitter = new StreamController(Configuration.newMemoryBased(
                StreamController.CONF_PARSER, DOMSNewspaperParser.class.getCanonicalName()
        ));
        splitter.setSource(source);
        return splitter;
    }
}
