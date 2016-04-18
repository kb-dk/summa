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
import dk.statsbiblioteket.summa.common.configuration.Resolver;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final File DOMS_PROBLEM = Resolver.getFile("support/alto/faulty_alto.xml");

    public void testFaulty() throws Exception {
        ObjectFilter splitter = getSplitter(DOMS_PROBLEM, DOMSNewspaperParser.DEFAULT_HEADLINE_MAX_WORDS);
        assertTrue("There should be a Record available", splitter.hasNext());
        int counter = 0;
        while (splitter.hasNext()) {
            Payload payload = splitter.next();
            log.info("Extracted " + payload.getId());
            System.out.println(RecordUtil.getString(payload));
            counter++;
        }
        assertEquals("Test record should be split into the right number of segments", 10, counter);
    }

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

    public void testHeadline() throws IOException {
        if (!DOMS_ALTO.exists()) {
            log.info("Cannon run test as '" + DOMS_ALTO + "' does not exist");
            return;
        }
        final int MAX_HEADLINE_WORDS = 2;
        final Pattern HEADLINE = Pattern.compile("<headline>(.*)</headline>");

        ObjectFilter splitter = getSplitter(MAX_HEADLINE_WORDS);
        assertTrue("There should be a Record available", splitter.hasNext());
        while (splitter.hasNext()) {
            Payload payload = splitter.next();
            log.info("Extracted " + payload.getId());
            Matcher matcher = HEADLINE.matcher(RecordUtil.getString(payload));
            while (matcher.find()) {
                String headline = matcher.group(1);
                assertTrue("There should be no more than " + MAX_HEADLINE_WORDS + " words in any headline, got '"
                           + headline + "'", MAX_HEADLINE_WORDS >= headline.split(" ").length);
            }
        }
    }

    public void testIllustrations() throws IOException {
        if (!DOMS_ALTO.exists()) {
            log.info("Cannon run test as '" + DOMS_ALTO + "' does not exist");
            return;
        }
        final int MAX_HEADLINE_WORDS = 2;

        ObjectFilter splitter = getSplitter(MAX_HEADLINE_WORDS);
        assertTrue("There should be a Record available", splitter.hasNext());
        while (splitter.hasNext()) {
            Payload payload = splitter.next();
            log.info("Extracted " + payload.getId());
            System.out.println(RecordUtil.getString(payload));
            assertTrue("There should be at least 1 illustration\n" + RecordUtil.getString(payload),
                       RecordUtil.getString(payload).contains("<illustration"));
        }
    }

    public void testOCR() throws IOException {
        if (!DOMS_ALTO.exists()) {
            log.info("Cannon run test as '" + DOMS_ALTO + "' does not exist");
            return;
        }
        final Pattern PWA = Pattern.compile("<predictedWordAccuracy>(.*)</predictedWordAccuracy>");

        ObjectFilter splitter = getSplitter();
        assertTrue("There should be a Record available", splitter.hasNext());
        while (splitter.hasNext()) {
            Payload payload = splitter.next();
            log.info("Extracted " + payload.getId());
            Matcher matcher = PWA.matcher(RecordUtil.getString(payload));
            assertTrue("There should be a pwa present\n" + RecordUtil.getString(payload), matcher.find());
            log.info("The PWA was " + matcher.group(1));
//            System.out.println(RecordUtil.getString(payload));
        }
    }

    public void testNoGroup() throws IOException {
        if (!DOMS_ALTO.exists()) {
            log.info("Cannon run test as '" + DOMS_ALTO + "' does not exist");
            return;
        }
        final Pattern ALTO_SEGMENT = Pattern.compile("<altosegment(.*)</altosegment>", Pattern.DOTALL);

        ObjectFilter splitter = getSplitter();
        assertTrue("There should be a Record available", splitter.hasNext());
        while (splitter.hasNext()) {
            Payload payload = splitter.next();
            Matcher matcher = ALTO_SEGMENT.matcher(RecordUtil.getString(payload));
            assertTrue("There should be altosegment in " + payload.getId() + "\n" + RecordUtil.getString(payload),
                       matcher.find());
            log.info("Found altosegment in " + payload.getId());
        }
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
            assertTrue("The transformed record should contain the field 'fulltext'\n" + RecordUtil.getString(payload),
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
        return getSplitter(DOMSNewspaperParser.DEFAULT_HEADLINE_MAX_WORDS);
    }

    private ObjectFilter getSplitter(int maxHeadlineWords) throws IOException {
        return getSplitter(DOMS_ALTO, maxHeadlineWords);
    }

    private ObjectFilter getSplitter(File altofile, int maxHeadlineWords) throws IOException {
        ObjectFilter source = new PayloadFeederHelper(0, altofile.toString());
        ObjectFilter splitter = new StreamController(Configuration.newMemoryBased(
                StreamController.CONF_PARSER, DOMSNewspaperParser.class.getCanonicalName(),
                DOMSNewspaperParser.CONF_HEADLINE_MAX_WORDS, maxHeadlineWords
        ));
        splitter.setSource(source);
        return splitter;
    }
}
