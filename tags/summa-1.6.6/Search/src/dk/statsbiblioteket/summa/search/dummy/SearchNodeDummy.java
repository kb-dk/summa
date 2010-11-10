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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.dummy.DummyResponse;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link dk.statsbiblioteket.summa.search.SearchNode} implementation that
 * simply returns statistics about its usage. Use it by plugging it into
 * a {@link dk.statsbiblioteket.summa.search.SummaSearcherImpl}.
 * <p></p>
 * If you need a stand alone dummy searcher use {@link SummaSearcherDummy}
 * instead.
 * <p></p>
 * This class is mainly used for debugging.
 *
 * @see SummaSearcherDummy
 * @see dk.statsbiblioteket.summa.search.SummaSearcherImpl
 * @see dk.statsbiblioteket.summa.search.api.dummy.DummyResponse
 */
public class SearchNodeDummy extends SearchNodeImpl {

    private static final Log log = LogFactory.getLog (SearchNodeDummy.class);

    /**
     * The id for the dummy. This will be part of the response.
     * </p><p>
     * Optional. Default is {@code this.toString();}.
     */
    public static final String CONF_ID = "search.dummy.id";

    private int warmupCount;
    private int openCount;
    private int closeCount;
    private int searchCount;
    private String id;

    public SearchNodeDummy(Configuration conf) {
        super (conf);

        warmupCount = 0;
        openCount = 0;
        closeCount = 0;
        searchCount = 0;
        id = conf.getString(CONF_ID, this.toString());
    }

    protected void managedWarmup (String request) {
        log.info ("Warmup (" + warmupCount + "): " + request);
        warmupCount++;
    }

    protected void managedOpen (String location) throws RemoteException {
        log.info ("Open (" + openCount + "): " + location);
        openCount++;
    }

    protected void managedClose () throws RemoteException {
        log.info ("Close ("+closeCount+")");
        closeCount++;
    }

    protected void managedSearch (Request request, ResponseCollection responses) throws RemoteException {
        log.info ("Search:\tRequest:" + request + "\n\tResponses: " + responses);

        responses.add(new DummyResponse(id, warmupCount, openCount,
                                        closeCount, searchCount));
        searchCount++;
    }
}




