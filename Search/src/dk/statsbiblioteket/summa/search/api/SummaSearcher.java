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
package dk.statsbiblioteket.summa.search.api;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

import java.rmi.RemoteException;
import java.io.IOException;

/**
 * A SummaSearcher is a collection of one or more SearchNodes. Any given Search
 * is propagated to the underlying SearchNodes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface SummaSearcher extends Configurable {

    /**
     * The port on which the searcher service should communicate.
     * The default is 28020.
     */
    public static final String CONF_SERVICE_PORT = "summa.searcher.port";

    /**
     * The class used to instantiate the searcher. Default is
     * {@link dk.statsbiblioteket.summa.search.SummaSearcherImpl}
     */
    public static final String CONF_CLASS = "summa.searcher.class";

    /**
     * Perform a search and collect the output from all sub-searchers.
     * The {@link Request} argument contains a list of key-value pairs,
     * also known as <i>search keys</i> from which the sub searchers will
     * extract their arguments.
     * <p></p>
     * A search will normally involve the addition of an implementation-specific
     * {@link Response} to responses.  Some searchers (notably the FacetSearcher)
     * requires data from previous Searchers (DocumentSearcher, in the case of
     * FacetSearcher).
     * @param request   contains SearchNode-specific request-data.
     * @return responsed from the underlying SearchNodes.
     * @throws RemoteException if one of the SearchNodes threw an exception.
     */
    public ResponseCollection search(Request request) throws IOException;

    /**
     * Close down the searcher. The searcher is not available for search after
     * this.
     * @throws RemoteException if an error happened during close.
     */
    public void close() throws IOException;
}




