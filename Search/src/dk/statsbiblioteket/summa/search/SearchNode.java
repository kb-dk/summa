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

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.search.document.DocumentSearchWrapper;

/**
 * Provides methods for controlling a SummaSearcher as part of a collection
 * of other SummaSearchers. Nodes should not open any indexes as part of the
 * construction-phase. Only {@link #open(String)} should trigger opening.
 * </p><p>
 * Nodes are not responsible for keeping track of running searches when open
 * or close is called. This is the responsibility of the user and is
 * implemented in {@link DocumentSearchWrapper}.
 * </p><p>
 * Nodes are not responsible for checking for sizes of fileds and fallbacks and
 * the size of maxRecords. Again, this is the responsibility of the caller.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface SearchNode extends BasicSearcher {

    /**
     * Opens the index at the given location. Implementations should ensure that
     * previously opened connections are closed before opening new.
     * It is not guaranteed that {@link #close()} will be called before open.
     * @param location     where the index can be found.
     * @throws IOException if the index could not be opened.
     */
    public void open(String location) throws IOException;

    /**
     * Perform a warmup-search. This normally means no logging of queries.
     * @param query       the query string to use for search.
     * @param sortKey     specifies how to sort. If this is null or
     *                    {@link #SORT_ON_SCORE}, sorting will be done on the
     *                    scores for the hits. If a field-name is specified,
     *                    sorting will be done on that field.<br />
     *                    This parameter is optional.
     *                    Default is {@link #SORT_ON_SCORE}.
     *                    Specifying null is the same as specifying
     *                    {@link #SORT_ON_SCORE}.
     * @param fields      the fields to extract content from.
     *                    This parameter is optional. Default is specified
     *                    in the conf. at {@link #CONF_RESULT_FIELDS}.
     */
    public void warmup(String query, String sortKey, String[] fields);
}
