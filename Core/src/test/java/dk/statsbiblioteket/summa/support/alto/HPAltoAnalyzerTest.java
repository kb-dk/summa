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

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HPAltoAnalyzerTest extends TestCase {
    //private static Log log = LogFactory.getLog(HPAltoAnalyzerTest.class);

    // TODO: We do not have license to publish these files. Generate obfuscated test files from them
    public static final String alto1977 = "/home/te/projects/hvideprogrammer/B-1977-10-02-P-0003.xml";
    public static final String alto1934 = "/home/te/projects/hvideprogrammer/A-1934-04-02-P-0005.xml";
    public static final String alto1947 = "/home/te/projects/hvideprogrammer/A-1947-01-01-P-0005.xml";
    public static final String alto1983 = "/home/te/projects/hvideprogrammer/B-1983-10-17-P-0080.xml";

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

    public void testBasic1977() throws XMLStreamException, FileNotFoundException {
        testBasicAnalyze(alto1977);
    }

    public void testBasic1934() throws XMLStreamException, FileNotFoundException {
        testBasicAnalyze(alto1934);
    }

    public void testBasic1947() throws XMLStreamException, FileNotFoundException {
        testBasicAnalyze(alto1947);
    }

    public void testBasic1983() throws XMLStreamException, FileNotFoundException {
        testBasicAnalyze(alto1983);
    }

    private void testBasicAnalyze(String source) throws XMLStreamException, FileNotFoundException {
        Alto alto = new Alto(new File(source));
        HPAltoAnalyzer analyzer = new HPAltoAnalyzer(Configuration.newMemoryBased());
        List<HPAltoAnalyzer.Segment> segments = analyzer.getSegments(alto);
        for (HPAltoAnalyzer.Segment segment: segments) {
            System.out.println(segment);
        }
    }
}
