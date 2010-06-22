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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;

/**
 * Nodes are the actual searchers under a SummaSearcher. Nodes are controlled
 * by SummaSearcher and should not open any indexes as part of the
 * construction-phase. Only {@link #open(String)} should trigger opening.
 * </p><p>
 * SearchNodes uses the Composite-pattern to achieve sequence, parallelism and
 * controlling open/close.
 * </p><p>
 * Note: It is highly recommended to use the
 * {@link dk.statsbiblioteket.summa.search.SearchNodeImpl} as it
 * keeps track of running searches when open and close is called.
 * The SearchNodeImpl also keeps track of the IndexDescriptor.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface SearchNode extends Configurable {
    /**
     * A search will normally involve the addition of an implementation-specific
     * Response to responses.  Some searchers (notably the FacetSearcher)
     * requires data from previous Searchers (DocumentSearcher, in the case of
     * FacetSearcher).
     * @param request   contains SearchNode-specific request-data.
     * @param responses where a Response to a Request is stored.
     * @throws RemoteException if the search could not be performed.
     */
    public void search(Request request, ResponseCollection responses) throws
                                                                RemoteException;

    /**
     * Performs a warm-up with the given request. This could be a single query
     * for a DocumentSearcher or a word for a did-you-mean searcher.
     * @param request implementation-specific warmup-data.
     */
    public void warmup(String request);

    /**
     * Opens the index at the given location. Implementations should ensure that
     * previously opened connections are closed before opening new.
     * It is not guaranteed that {@link #close()} will be called before open.
     * @param location     where the index can be found.
     * @throws RemoteException if the index could not be opened.
     */
    public void open(String location) throws RemoteException;

    /**
     * Shut down the searcher and free all associated resources. The searcher
     * cannot be used after close. If an open() is called after close, the
     * searcher must be usable again.
     * @throws RemoteException if an error occured during close. Implementers
     *                         of the interface are urged to free as many
     *                         resources as possible, even in the event of an
     *                         exception.
     */
    public void close() throws RemoteException;

    /**
     * A slot refers to the number of searches or warmups that can be performed
     * simultaneously at the current time. If the number of free slots is 0,
     * new requests should be queued.
     * @return the number of free slots. This can be 0.
     */
    public int getFreeSlots();
}




