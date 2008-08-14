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

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

import java.rmi.RemoteException;
import java.rmi.Remote;
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
    public static final String PROP_SERVICE_PORT = "summa.searcher.port";

    /**
     * The class used to instantiate the searcher. Default is
     * {@link SummaSearcherImpl}
     */
    public static final String PROP_CLASS = "summa.searcher.class";

    /**
     * A search will normally involve the addition of an implementation-specific
     * Response to responses.  Some searchers (notably the FacetSearcher)
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
