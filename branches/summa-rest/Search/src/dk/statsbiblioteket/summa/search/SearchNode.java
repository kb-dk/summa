/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.search;

import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;

/**
 * Nodes are the actual searchers under a SummaSearcher. Nodes are controlled
 * by SummaSearcher and should not open any indexes as part of the
 * construction-phase. Only {@link #open(String)} should trigger opening.
 * </p><p>
 * SearchNodes uses the Composite-pattern to achieve sequence, parallelism and
 * controlling open/close.
 * </p><p>
 * Note: It is highly recommended to use the {@link dk.statsbiblioteket.summa.search.SearchNodeImpl} as it
 * keeps track of running searches when open and close is called.
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



