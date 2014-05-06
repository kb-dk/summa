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
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.unittest.PayloadFeederHelper;
import dk.statsbiblioteket.summa.index.lucene.StreamingDocumentCreator;
import dk.statsbiblioteket.summa.ingest.split.StreamController;
import dk.statsbiblioteket.summa.support.alto.as2.AS2AltoAnalyzer;
import dk.statsbiblioteket.summa.support.alto.as2.AS2AltoAnalyzerTest;
import dk.statsbiblioteket.summa.support.alto.as2.AS2AltoParser;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoGeneratorFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(AltoGeneratorFilterTest.class);

    public void testBasicGeneration() throws Exception {
        final int RECORDS = 5;

        AltoGeneratorFilter generator = getGenerator(RECORDS);
        assertTrue("There should be a Payload", generator.hasNext());
        int received = 0;
        while (generator.hasNext()) {
             //System.out.println(RecordUtil.getString(generator.next()));
            generator.next();
            received++;
        }
        assertEquals("The correct number of ALTO records should be produced", RECORDS, received);
    }

    public void testSpeedGenerate() throws IOException {
        final int RECORDS = 20000;

        AltoGeneratorFilter generator = getGenerator(RECORDS+1);
        generator.pump();

        Profiler profiler = new Profiler(RECORDS, Math.min(RECORDS / 4, 5000));
        do {
            profiler.beat();
        } while (generator.pump());
        generator.close(true);
        log.info(String.format("Generated %d ALTO-records at %d records/sec",
                               profiler.getBeats(), (int) profiler.getBps(true)));
    }

    public void testAnalyzing() throws IOException {
        final int RECORDS = 5;
        final int EXPECTED_SEGMENTS = 35;
        final String infix = "_87_";

        ObjectFilter analyzer = getAnalyzer(RECORDS);
        assertTrue("There should be a Payload", analyzer.hasNext());
        int received = 0;
        Set<String> ids = new HashSet<>();
        while (analyzer.hasNext()) {
            String id = analyzer.next().getId();
            assertTrue("Payload #" + received + " should have the correct ID-infix '" + infix + "' but was " + id,
                       id.contains(infix));
            ids.add(id);
            received++;
        }
        assertEquals("The correct number of ALTO segments should be produced", EXPECTED_SEGMENTS, received);
        assertEquals("IDs should be unique (#segments = #unique_IDs", received, ids.size());
    }

    public void testSpeedAnalyze() throws IOException {
        final int RECORDS = 1000;

        ObjectFilter analyzer = getAnalyzer(RECORDS+1);
        assertTrue("There should be at least 1 payload", analyzer.hasNext());
        analyzer.pump();

        Profiler profiler = new Profiler();
        while (analyzer.pump()) {
            profiler.beat();
        }
        analyzer.close(true);
        log.info(String.format("Processed %d ALTO-records which contained %d articles at %d articles/sec",
                               RECORDS, profiler.getBeats(), (int)profiler.getBps(true)));
    }

    public void testCreator() throws IOException {
        final int RECORDS = 50;

        ObjectFilter analyzer = getDocumentCreator(RECORDS);
        assertTrue("There should be a Payload", analyzer.hasNext());
        int received = 0;
        Set<String> ids = new HashSet<>();
        while (analyzer.hasNext()) {
            Payload altoPayload = analyzer.next();
            String id = altoPayload.getId();
            assertNotNull("The Payload '" + id + "' should have a Lucene document",
                          altoPayload.getData(Payload.LUCENE_DOCUMENT));
            ids.add(id);
            received++;
        }
        log.info("Received " + ids.size() + " article records");
        assertTrue("The number of unique IDs (" + ids.size() + ") should match the number of received Payloads ("
                   + received + ")", received == ids.size());
    }

    private ObjectFilter getDocumentCreator(int records) throws IOException {
        Configuration conf = Configuration.newMemoryBased();
        Configuration descConf = conf.createSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR);
        descConf.set(IndexDescriptor.CONF_ABSOLUTE_LOCATION, "support/alto/AltoIndexDescriptor.xml");
        StreamingDocumentCreator creator = new StreamingDocumentCreator(conf);
        creator.setSource(getAnalyzer(records));
        return creator;
    }

    private ObjectFilter getAnalyzer(int records) throws IOException {
        ObjectFilter altoFilter = new StreamController(Configuration.newMemoryBased(
                StreamController.CONF_PARSER, AS2AltoParser.class,
                //AS2AltoAnalyzer.CONF_ID_PATTERN, "(random_alto_[0-9]+_[0-9])",
                AS2AltoAnalyzer.CONF_ID_PATTERN,
                ".*[/\\\\]([^/\\\\]+-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{4}[^\\.]*.*)",
//                AS2AltoAnalyzer.CONF_URL_REGEXP, "random_alto_([0-9]+_[0-9]+)",
                AS2AltoAnalyzer.CONF_URL_REPLACEMENT, "http://example.com/alto_$1"
        ));
        altoFilter.setSource(getGenerator(records));
        return altoFilter;
    }

    private AltoGeneratorFilter getGenerator(int records) throws IOException {
        AltoGeneratorFilter generator = new AltoGeneratorFilter(Configuration.newMemoryBased(
                AltoGeneratorFilter.CONF_RANDOM_SEED, 87,
                AltoGeneratorFilter.CONF_RECORDS, records
        ));
        generator.setSource(getFeeder());
        return generator;
    }


    private ObjectFilter getFeeder() throws IOException {
        return new PayloadFeederHelper(AS2AltoAnalyzerTest.s1795_1, AS2AltoAnalyzerTest.s1846_1);
    }

}
