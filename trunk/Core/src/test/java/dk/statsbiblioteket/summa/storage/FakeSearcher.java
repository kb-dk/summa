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
package dk.statsbiblioteket.summa.storage;

import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FakeSearcher implements SummaSearcher {
    private static final List<String> DEFAULT_FIELDS = Arrays.asList("recordID", "recordBase", "shortformat", "text");
    private Log log = LogFactory.getLog(FakeSearcher.class);

    private List<Request> requests = new ArrayList<>();

    @Override
    public ResponseCollection search(Request request) throws IOException {
        requests.add(request);
        ResponseCollection collection = new ResponseCollection();
        DocumentResponse docs = new DocumentResponse(
                request.getString(DocumentKeys.SEARCH_FILTER, null),
                request.getString(DocumentKeys.SEARCH_QUERY, null),
                request.getLong(DocumentKeys.SEARCH_START_INDEX, 0L),
                request.getLong(DocumentKeys.SEARCH_MAX_RECORDS, 10L),
                request.getString(DocumentKeys.SEARCH_SORTKEY, null),
                request.getBoolean(DocumentKeys.SEARCH_REVERSE, false),
                toArray(request.getStrings(DocumentKeys.SEARCH_RESULT_FIELDS, DEFAULT_FIELDS)),
                1, // responseTime
                1 // hitCount
        );
        DocumentResponse.Record record = new DocumentResponse.Record("someRecord", "someBase", 87.0f, "a");
        record.add(new DocumentResponse.Field("recordID", "someRecord", true));
        record.add(new DocumentResponse.Field("recordBase", "someBase", true));
        record.add(new DocumentResponse.Field("shortformat", "<myxml>foo</myxml>", true));
        record.add(new DocumentResponse.Field("text", "myText", true));
        docs.addRecord(record);
        collection.add(docs);
        return collection;
    }

    /**
     * @return all requests issued to this searcher.
     */
    public List<Request> getRequests() {
        return requests;
    }

    private String[] toArray(List<String> strings) {
        if (strings == null) {
            return null;
        }
        String[] array = new String[strings.size()];
        strings.toArray(array);
        return array;
    }

    @Override
    public void close() throws IOException {
        log.info("Closing down");
    }
}
