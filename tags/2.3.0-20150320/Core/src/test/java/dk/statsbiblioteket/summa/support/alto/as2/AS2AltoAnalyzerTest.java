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
package dk.statsbiblioteket.summa.support.alto.as2;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.support.alto.Alto;
import dk.statsbiblioteket.summa.support.alto.as.ASAltoAnalyzer;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.analysis.payloads.TokenOffsetPayloadTokenFilter;

import java.io.File;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AS2AltoAnalyzerTest extends TestCase {

    public static final String ROOT = "support/alto/as2/sample_2_2/B400022028241-RT2/400022028241-14/";
    public static final String ROOT17 = ROOT + "1795-06-13-01/";
    public static final String ROOT18 = ROOT + "1846-01-20-01/";
    public static final String s1795_1 = ROOT17 + "AdresseContoirsEfterretninger-1795-06-13-01-0006.alto.xml";
    public static final String s1795_2 = ROOT17 + "AdresseContoirsEfterretninger-1795-06-13-01-0007A.alto.xml";
    public static final String s1846_1 = ROOT18 + "AdressecomptoirsEfterretninger-1846-01-20-01-0028.alto.xml";
    public static final String s1846_2 = ROOT18 + "AdressecomptoirsEfterretninger-1846-01-20-01-0029A.alto.xml";
    public static final String s1846_3 = ROOT18 + "AdressecomptoirsEfterretninger-1846-01-20-01-0029B.alto.xml";

    public AS2AltoAnalyzerTest(String name) {
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
        return new TestSuite(AS2AltoAnalyzerTest.class);
    }

    public void test_s1792_1() throws Exception {
        testBasicAnalyze(Configuration.newMemoryBased(), s1795_1);
    }

    public void test_s1846_1() throws Exception {
        testBasicAnalyze(Configuration.newMemoryBased(), s1846_1);
    }

    private void testBasicAnalyze(Configuration conf, String source) throws Exception {
        Alto alto = new Alto(Resolver.getFile(source));
        AS2AltoAnalyzer analyzer = new AS2AltoAnalyzer(conf);
        List<AS2AltoAnalyzer.AS2Segment> segments = analyzer.getSegments(alto);
        for (AS2AltoAnalyzer.AS2Segment segment: segments) {
            System.out.println(segment);
        }
    }
}
