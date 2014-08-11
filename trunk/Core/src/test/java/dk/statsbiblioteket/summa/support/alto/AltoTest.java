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

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoTest extends TestCase {
//    private static Log log = LogFactory.getLog(AltoTest.class);

    // TODO: We do not have license to publish these files. Generate obfuscated test files from them
    public static final String alto1977 = "/mnt/bulk/projects/hvideprogrammer/B-1977-10-02-P-0003.xml";

    public AltoTest(String name) {
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
        return new TestSuite(AltoTest.class);
    }

    public void testBasicParse() throws XMLStreamException, FileNotFoundException {
        Alto alto = new Alto(new File(alto1977));
        assertFalse("The number of groups should be > 0", alto.getTextBlockGroups().isEmpty());
    }

    public void testGroupMinWordCount() throws XMLStreamException, FileNotFoundException {
        Alto alto = new Alto(new File(alto1977));
        int m50 = alto.getTextBlockGroups(0, 50).size();
        int m10 = alto.getTextBlockGroups(0, 10).size();
        assertNotSame("The group count for minWords=50 and minWords=10 should differ", m50, m10);
    }

    public void testGroupMinBlockCount() throws XMLStreamException, FileNotFoundException {
        Alto alto = new Alto(new File(alto1977));
        int m50 = alto.getTextBlockGroups(1, 0).size();
        int m10 = alto.getTextBlockGroups(5, 0).size();
        assertNotSame("The group count for minBlocks=1 and minBlocks=5 should differ", m50, m10);
    }
}
