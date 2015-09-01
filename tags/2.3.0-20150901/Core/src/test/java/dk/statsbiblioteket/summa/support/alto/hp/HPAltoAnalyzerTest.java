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
package dk.statsbiblioteket.summa.support.alto.hp;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.alto.Alto;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HPAltoAnalyzerTest extends TestCase {
    //private static Log log = LogFactory.getLog(HPAltoAnalyzerTest.class);

    // TODO: We do not have license to publish these files. Generate obfuscated test files from them
    public static final String alto1934 = "/home/te/projects/hvideprogrammer/A-1934-04-02-P-0005.xml";
    public static final String alto1947 = "/home/te/projects/hvideprogrammer/A-1947-01-01-P-0005.xml";
    public static final String alto1977 = "/home/te/projects/hvideprogrammer/B-1977-10-02-P-0003.xml";
    public static final String alto1972 = "/home/te/projects/hvideprogrammer/B-1972-01-09-P-0014.xml";
    public static final String alto1983 = "/home/te/projects/hvideprogrammer/B-1983-10-17-P-0080.xml";
    public static final String alto1933_path = "/home/te/projects/hvideprogrammer/samples_with_paths/dhp/data/"
                                               + "Arkiv_A.1/1933_07-09/ALTO/A-1933-07-02-P-0008.xml";

    public HPAltoAnalyzerTest(String name) {
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
        return new TestSuite(HPAltoAnalyzerTest.class);
    }

    public void testPath1933() throws Exception {
        testBasicAnalyze(alto1933_path);
    }

    public void testBasic1934() throws Exception {
        testBasicAnalyze(alto1934);
    }

    public void testBasic1947() throws Exception {
        testBasicAnalyze(alto1947);
    }

    public void testBasic1972() throws Exception {
        testBasicAnalyze(alto1972);
    }

    public void testCustom1972() throws Exception {
        testBasicAnalyze(Configuration.newMemoryBased(
                        HPAltoAnalyzerSetup.CONF_MERGE_SUBSEQUENT_NOTIME, false,
                        HPAltoAnalyzerSetup.CONF_CONNECT_TIMES, true
                ), alto1972);
    }

    public void testBasic1977() throws Exception {
        testBasicAnalyze(alto1977);
    }

    public void testBasic1983() throws Exception {
        testBasicAnalyze(alto1983);
    }

    private void testBasicAnalyze(String source) throws Exception {
        Configuration conf = Configuration.newMemoryBased();
        testBasicAnalyze(conf, source);
    }

    private void testBasicAnalyze(Configuration conf, String source) throws Exception {
        Configuration realConf = Configuration.newMemoryBased();
        List<Configuration> subs = realConf.createSubConfigurations(HPAltoAnalyzer.CONF_SETUPS, 1);
        subs.get(0).importConfiguration(conf);
        Alto alto = new Alto(new File(source));
        HPAltoAnalyzer analyzer = new HPAltoAnalyzer(realConf);
        List<HPAltoAnalyzer.HPSegment> segments = analyzer.getSegments(alto);
        for (HPAltoAnalyzer.HPSegment segment: segments) {
            System.out.println(segment);
        }
    }
}
