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
package dk.statsbiblioteket.summa.common.solr.analysis;

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.CharStream;

import java.io.IOException;
import java.io.StringReader;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LowerCaseCharFilterFactoryTest extends TestCase {
    public LowerCaseCharFilterFactoryTest(String name) {
        super(name);
    }
    private LowerCaseCharFilterFactory factory;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        factory = new LowerCaseCharFilterFactory();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testTrivial() throws IOException {
        assertEquals("abc", analyze("ABC"));
        assertEquals("abc", analyze("abc"));
        assertEquals("abc def", analyze("ABC DEF")); // Spaces
        assertEquals("æøå", analyze("ÆØÅ")); // Danish

        // https://en.wikipedia.org/wiki/Capital_%E1%BA%9E
        assertFalse("ß has no upper case version in the traditional sense", "ß".equals(analyze("ẞ")));
        assertEquals("123", analyze("123"));
        assertEquals("ö", analyze("Ö"));
        assertEquals("κ", analyze("Κ")); // Greek
    }

    private String analyze(CharSequence in) throws IOException {
        CharStream stream = factory.create(CharReader.get(new StringReader(in.toString())));
        return Strings.flush(stream);
    }

    public static Test suite() {
        return new TestSuite(LowerCaseCharFilterFactoryTest.class);
    }
}




