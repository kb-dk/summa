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
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.StringReader;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LowerCaseCharFilterTest extends TestCase {
    private static Log log = LogFactory.getLog(LowerCaseCharFilterTest.class);

    public void testBasic() throws Exception {
        assertEquals("foo", process("FOO"));
    }

    private String process(String input) throws IOException {
        LowerCaseCharFilter filter = new LowerCaseCharFilter(Version.LUCENE_43, new StringReader(input));
        return Strings.flush(filter);
    }
}
