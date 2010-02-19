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
package dk.statsbiblioteket.summa.search.dummy;

import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.SummaSearcherImpl;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.dummy.DummyResponse;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Dummy implementattion of a {@link dk.statsbiblioteket.summa.search.api.SummaSearcher} returning
 * {@link dk.statsbiblioteket.summa.search.api.dummy.DummyResponse}s.
 *
 * @see SearchNodeDummy
 * @see dk.statsbiblioteket.summa.search.api.dummy.DummyResponse
 * @see SummaSearcherImpl
 */
public class SummaSearcherDummy implements SummaSearcher {
    private static final Log log = LogFactory.getLog (SummaSearcherDummy.class);

    /**
     * See {@link SearchNodeDummy#CONF_ID}.
     */
    public static final String CONF_ID = SearchNodeDummy.CONF_ID;

    private int closeCount;
    private int searchCount;
    private String id;

    public SummaSearcherDummy(Configuration conf) {
        closeCount = 0;
        searchCount = 0;
        id = conf.getString(CONF_ID, this.toString());
    }

    public ResponseCollection search(Request request) throws IOException {
        log.info ("Got request (" + searchCount + "): " + request);

        ResponseCollection resp = new ResponseCollection ();
        resp.add (new DummyResponse(id, 0, 0, closeCount, searchCount));

        searchCount++;
        return resp;
    }

    public void close() throws IOException {
        log.info ("Got close request (" + closeCount + ")");
        closeCount++;
    }
}




