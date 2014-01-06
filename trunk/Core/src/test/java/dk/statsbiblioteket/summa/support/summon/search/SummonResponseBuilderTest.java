/**  Licensed under the Apache License, Version 2.0 (the "License");
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
package dk.statsbiblioteket.summa.support.summon.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummonResponseBuilderTest extends TestCase {
    private static Log log = LogFactory.getLog(SummonResponseBuilderTest.class);

    public SummonResponseBuilderTest(String name) {
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
        return new TestSuite(SummonResponseBuilderTest.class);
    }


    public void testConvert() throws Exception {
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        SummonResponseBuilder rb = new SummonResponseBuilder(Configuration.newMemoryBased());
        ResponseCollection rc = new ResponseCollection();

        rb.buildResponses(request, new SolrFacetRequest("foo", 0, 10, "and"), rc,
                          Resolver.getUTF8Content("support/summon/search/summon_response.xml"), "");
        System.out.println(rc.toXML());
    }

    public void testConvertMulti() throws Exception {
        Request request = new Request();
        request.put(DocumentKeys.SEARCH_QUERY, "foo");
        request.put(DocumentKeys.SEARCH_COLLECT_DOCIDS, true);
        SummonResponseBuilder rb = new SummonResponseBuilder(Configuration.newMemoryBased(
                SummonResponseBuilder.CONF_COLLAPSE_MULTI_FIELDS, false
        ));
        ResponseCollection rc = new ResponseCollection();

        rb.buildResponses(request, new SolrFacetRequest("foo", 0, 10, "and"), rc,
                          Resolver.getUTF8Content("support/summon/search/summon_response.xml"), "");
        System.out.println(rc.toXML());
    }
}
