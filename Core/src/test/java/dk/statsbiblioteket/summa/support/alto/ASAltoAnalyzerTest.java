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
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ASAltoAnalyzerTest extends TestCase {

    // TODO: We do not have license to publish these files. Generate obfuscated test files from them
    public static final String p5_2012 =
            "/home/te/projects/avisscanning/samples/5/Berlingske-2012-01-02-01-0002.alto.xml";
    public static final String p4_2012 =
            "/home/te/projects/avisscanning/samples/4/Dagbladet_Ringsted-2012-01-02-01-0106.xml";
    public static final String p1_2012 =
            "/home/te/projects/avisscanning/samples/1/Dagbladet-2012-01-02-01-0083.alto.xml";

    public ASAltoAnalyzerTest(String name) {
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
        return new TestSuite(ASAltoAnalyzerTest.class);
    }

    public void test_p1_2012() throws Exception {
        testBasicAnalyze(Configuration.newMemoryBased(
                ASAltoAnalyzer.CONF_DATE_PATTERN, "[^0-9]+([0-9]{4})-([0-9]{2})-([0-9]{2})-.*tif"
        ), p1_2012);
    }

    public void test_p4_2012() throws Exception {
        testBasicAnalyze(Configuration.newMemoryBased(
                ASAltoAnalyzer.CONF_DATE_PATTERN, "[^0-9]+([0-9]{4})-([0-9]{2})-([0-9]{2})-.*tif"
        ), p4_2012);
    }

    public void test_p5_2012() throws Exception {
        // Berlingske-1795-13-06-01-0006.alto.xml
        testBasicAnalyze(Configuration.newMemoryBased(
                ASAltoAnalyzer.CONF_DATE_PATTERN, "[^0-9]+([0-9]{4})-([0-9]{2})-([0-9]{2})-.*tif"
        ), p5_2012);
    }

    private void testBasicAnalyze(Configuration conf, String source) throws Exception {
        Alto alto = new Alto(new File(source));
        ASAltoAnalyzer analyzer = new ASAltoAnalyzer(conf);
        List<ASAltoAnalyzer.Segment> segments = analyzer.getSegments(alto);
        for (ASAltoAnalyzer.Segment segment: segments) {
            System.out.println(segment);
        }
    }
}
