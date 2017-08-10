package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

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
public class SearchWSTest {

    @Test
    public void testIndirectCall() {
        SearchWS searchWS = new SearchWS(Configuration.newMemoryBased(
                SearchWS.CONF_PRUNE_HIGHORDERUNICODE, false
        ), new DummySearcher());
        String response = searchWS.simpleSearch("dummy", 10, 0);
        assertTrue("There should be a basic field containing foo\n" + response,
                   response.contains("<field name=\"basic\">foo</field>"));
        assertTrue("There should be an extended field containing \uD835\uDC9C\n" + response,
                   response.contains("<field name=\"extended\">\uD835\uDC9C</field>"));
    }

    @Test
    public void testPruning() {
        SearchWS searchWS = new SearchWS(Configuration.newMemoryBased(
                SearchWS.CONF_PRUNE_HIGHORDERUNICODE, true
        ), new DummySearcher());
        String response = searchWS.simpleSearch("dummy", 10, 0);
        assertTrue("There should be a basic field containing foo\n" + response,
                   response.contains("<field name=\"basic\">foo</field>"));
        assertFalse("There should not be an extended field containing \uD835\uDC9C\n" + response,
                   response.contains("<field name=\"extended\">\uD835\uDC9C</field>"));
    }

    // The Unicode character &#x1d49c; (𝒜) in Summon XML was converted to &#xD835;&#xDC9C; by the web service
    // See also SummonSearchNodeTest for high unicode problem
    private static class DummySearcher implements SummaSearcher {
        @Override
        public ResponseCollection search(Request request) throws IOException {
            ResponseCollection responses = new ResponseCollection();
            DocumentResponse documents = new DocumentResponse(
                    null, "Dummy", 0, 10, null, false, new String[]{"recordID", "content"}, 10, 10);
            DocumentResponse.Record record = new DocumentResponse.Record("DummyID", "unittest", 1.2f, "a");
            record.add(new DocumentResponse.Field("basic", "foo", false));
            record.add(new DocumentResponse.Field("extended", "\uD835\uDC9C", false));
            documents.addGroup(new DocumentResponse.Group(record, "recordID"));
            responses.add(documents);
            return responses;
        }

        @Override
        public void close() throws IOException {

        }
    }
}