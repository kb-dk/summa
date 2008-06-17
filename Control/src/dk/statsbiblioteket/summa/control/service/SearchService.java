/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.control.service;

import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.search.SummaSearcher;
import dk.statsbiblioteket.summa.search.LuceneSearcher;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper for a {@link SummaSearcher}, which will normally translate to a
 * {@link LuceneSearcher}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchService extends ServiceBase implements SummaSearcher {
    private Log log = LogFactory.getLog(SearchService.class);

    private SummaSearcher searcher;

    public SearchService(Configuration conf) throws RemoteException {
         super(conf);
         exportRemoteInterfaces();
        setStatus(Status.CODE.constructed,
                  "Created SearchService object",
                  Logging.LogLevel.DEBUG);

         setStatus(Status.CODE.constructed,
                   "Remote interfaces are up for SearchService",
                   Logging.LogLevel.DEBUG);
     }

    public void start() throws RemoteException {
        // TODO: Implement this
    }

    public void stop() throws RemoteException {
        // TODO: Implement this
    }

    /* Simple pass-through to the underlying searcher */


    /**
     * Implementation of the {@link SummaSearcher} interface. See the JavaDocs
     * for {@link SummaSearcher#fullSearch} for details on usage.
     * @param filter      a query that narrows the search. A filter does not
     *                    affect scores.<br />
     *                    This parameter is optional. Default is null.
     * @param query       a query as entered by a user. This is expanded to
     *                    the underlying index query-system, normally with
     *                    the use of
     *           {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor}.
     * @param startIndex  the starting index for the result, counting from 0.
     *                    If the result is to be merged with the result from
     *                    other searchers, this needs to be 0 in order to
     *                    ensure proper merging.
     * @param maxRecords  the maximum number of records to return.<br />
     *                    this parameter is mandatory and is rounded down to
     *                    the value specified in properties, using the key
     *                    {link #CONF_MAX_NUMBER_OF_RECORDS}.
     * @param sortKey     specifies how to sort. If this is null or
     *                    {@link #SORT_ON_SCORE}, sorting will be done on the
     *                    scores for the hits. If a field-name is specified,
     *                    sorting will be done on that field.<br />
     *                    This parameter is optional.
     *                    Default is {@link #SORT_ON_SCORE}.
     *                    Specifying null is the same as specifying
     *                    {@link #SORT_ON_SCORE}.
     * @param reverseSort if true, the sort is performed in reverse order.
     * @param resultFields the fields to extract content from.
     *                    This parameter is optional. Default is specified
     *                    in the conf. at {@link #CONF_RESULT_FIELDS}.
     * @param fallbacks   if the value of a given field cannot be extracted,
     *                    the corresponding value from fallbacks is returned.
     *                    Note that the length of fallbacks and fields must
     *                    be the same.
     * @return the result of a search in XML, as specified above.
     * @throws RemoteException if there was an exception during search.
     */
    public String fullSearch(String filter, String query, long startIndex,
                             long maxRecords, String sortKey,
                             boolean reverseSort, String[] resultFields,
                             String[] fallbacks) throws RemoteException {
        return searcher.fullSearch(filter, query, startIndex, maxRecords,
                                   sortKey, reverseSort,
                                   resultFields, fallbacks);
    }

    /**
     * Simple shortcut for fullSearch. Equivalent to {@code
     * fullSearch(null, query, startIndex, maxRecords, null, false, null, null);
     * }
     * @param query       a query as entered by a user. This is expanded to
     *                    the underlying index query-system, normally with
     *                    the use of
     *           {@link dk.statsbiblioteket.summa.common.index.IndexDescriptor}.
     * @param startIndex  the starting index for the result, counting from 0.
     *                    If the result is to be merged with the result from
     *                    other searchers, this needs to be 0 in order to
     *                    ensure proper merging.
     * @param maxRecords  the maximum number of records to return.<br />
     *                    this parameter is mandatory and is rounded down to
     *                    the value specified in properties, using the key
     *                    {link #CONF_MAX_NUMBER_OF_RECORDS}.
     * @return the search-result in XML, as specified in {@link #fullSearch}.
     * @throws RemoteException if there was an exception during search.
     */
    public String simpleSearch(String query, long startIndex,
                               long maxRecords) throws RemoteException {
        return searcher.simpleSearch(query, startIndex, maxRecords);
    }
}
