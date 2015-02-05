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
package dk.statsbiblioteket.summa.search.tools;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QueryFuzzinatorTest extends TestCase {
    private static Log log = LogFactory.getLog(QueryFuzzinatorTest.class);

    public void testRewrite() throws Exception {
        assertFuzzy(Configuration.newMemoryBased(), new Request(), "foo~2", "foo");
        assertFuzzy(Configuration.newMemoryBased(), new Request(), "foo~1", "foo~1");
        assertFuzzy(Configuration.newMemoryBased(), new Request(), "foo~2 bar~2", "foo bar");
        assertFuzzy(Configuration.newMemoryBased(), new Request(), "foo~2 bar~1", "foo bar~1");
        assertFuzzy(Configuration.newMemoryBased(), new Request(), "foo:bar~1", "foo:bar~1");
    }

    private void assertFuzzy(
            Configuration fuzzyConf, Request request, String expected, String query) throws ParseException {
        QueryFuzzinator fuzzinator = new QueryFuzzinator(fuzzyConf);
        String actual = fuzzinator.rewrite(request, query);
        assertEquals("The query '" + query + "' should be rewritten correctly", expected, actual);
    }
}
