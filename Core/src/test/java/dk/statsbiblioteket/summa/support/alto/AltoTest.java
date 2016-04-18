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

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoTest extends TestCase {
//    private static Log log = LogFactory.getLog(AltoTest.class);

    // Newspapers > 100 years old has been cleared for free distribution
    public static final String alto1795 = "support/alto/as2/sample_2_2/B400022028241-RT2/400022028241-14/1795-06-13-01/"
                                          + "AdresseContoirsEfterretninger-1795-06-13-01-0006.alto.xml";
    //public static final String ALTO1909 = "suppport/alto/nationaltidende-1909-11-14-02-0281B.alto.xml";
    // FIXME: Why does the classpath version not work?
    public static final String ALTO1909 = "/home/te/projects/summa-trunk/Core/src/test/resources/support/alto/" +
                                          "nationaltidende-1909-11-14-02-0281B.alto.xml";

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

    public void testIllustrations() throws FileNotFoundException, XMLStreamException {
        assertNotNull("The file " + ALTO1909 + " should be available", Resolver.getFile(ALTO1909));
        Alto alto = new Alto(Resolver.getFile(ALTO1909));

    }

    public void testBasicParse() throws XMLStreamException, FileNotFoundException {
        Alto alto = new Alto(Resolver.getFile(alto1795));
        assertFalse("The number of groups should be > 0", alto.getTextBlockGroups().isEmpty());
    }

    // alto1795
    // <String ID="S369" CONTENT="her" WC="0.852" CC="7 8 8" HEIGHT="148" WIDTH="176" HPOS="3700" VPOS="8052" SUBS_TYPE="HypPart1" SUBS_CONTENT="herfra"/>
    // <HYP CONTENT="-" WIDTH="36" HPOS="3884" VPOS="8100"/>
    // </TextLine>
    // <TextLine ID="LINE48" STYLEREFS="TS9.5" HEIGHT="164" WIDTH="3624" HPOS="300" VPOS="8156">
    // <String ID="S370" CONTENT="fra" WC="0.926" CC="8 9 8" HEIGHT="128" WIDTH="168" HPOS="300" VPOS="8164" SUBS_TYPE="HypPart2" SUBS_CONTENT="herfra"/>


    public void testHyphen() throws FileNotFoundException, XMLStreamException {
        Alto alto = new Alto(Resolver.getFile(alto1795));
        alto.setHyphenMode(Alto.HYPHEN_MODE.join);
        assertEquals("The number of 'herfra' terms should match for join of hyphenated multi-line terms",
                     2, count(alto, "herfra"));
        alto.setHyphenMode(Alto.HYPHEN_MODE.split);
        assertEquals("The number of 'herfra' terms should match for non-join of hyphenated multi-line terms",
                     1, count(alto, "herfra"));
    }

    private int count(Alto alto, String term) {
        int count = 0;
        for (String current: alto.getAllTexts()) {
            if (term.equals(current)) {
                count++;
            }
        }
        return count;
    }

    public void testGroupMinWordCount() throws XMLStreamException, FileNotFoundException {
        Alto alto = new Alto(Resolver.getFile(alto1795));
        int m50 = alto.getTextBlockGroups(0, 500).size();
        int m10 = alto.getTextBlockGroups(0, 2).size();
        assertNotSame("The group count for minWords=500 (" + m50 + ") and minWords=2 (" + m10 + ") should differ",
                      m50, m10);
    }

    public void testGroupMinBlockCount() throws XMLStreamException, FileNotFoundException {
        Alto alto = new Alto(Resolver.getFile(alto1795));
        int m50 = alto.getTextBlockGroups(1, 0).size();
        int m10 = alto.getTextBlockGroups(5, 0).size();
        assertNotSame("The group count for minBlocks=1 and minBlocks=5 should differ", m50, m10);
    }
}
