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
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.lucene.queryparser.classic.ParseException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AltoHighlighterTest extends TestCase {
//    private static Log log = LogFactory.getLog(AltoParserTest.class);

    public AltoHighlighterTest(String name) {
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
        return new TestSuite(AltoHighlighterTest.class);
    }

    public void testSimple() throws ParseException {
        AltoHighlighter hi = new AltoHighlighter(Configuration.newMemoryBased());
        assertEquals("foo, bar, zoo", Strings.join(hi.getTokens("foo (bar zoo)"), ", "));
    }

    public void testPhrase() throws ParseException {
        AltoHighlighter hi = new AltoHighlighter(Configuration.newMemoryBased());
        assertEquals("foo, bar, zoo", Strings.join(hi.getTokens("foo \"bar zoo\""), ", "));
    }

    public void testPrefix() throws ParseException {
        AltoHighlighter hi = new AltoHighlighter(Configuration.newMemoryBased());
        assertEquals("foo, bar*, zoo", Strings.join(hi.getTokens("foo bar* zoo"), ", "));
    }

    public void testNot() throws ParseException {
        AltoHighlighter hi = new AltoHighlighter(Configuration.newMemoryBased());
        assertEquals("foo", Strings.join(hi.getTokens("foo -bar -zoo"), ", "));
    }
}
