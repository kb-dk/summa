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

import java.rmi.RemoteException;
import java.util.Arrays;

/**
 * The interface that all searchers in Summa should implement. The interface is
 * expected to be implemented by classes that are used with RMI or a similar
 * mechanism.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface SummaSearcher {
    /**
     * When a search has been performed by the underlying index searcher,
     * the content of these fields should be returned.
     * Note that this can be overrided by the {@link #fullSearch}-method.
     * </p><p>
     * This value is extracted by calling
     * {@link dk.statsbiblioteket.summa.common.configuration.Configuration#getStrings(String)}.
     */
    public static final String CONF_RESULT_FIELDS =
            "summa.search.result-fields";
    public static final String[] DEFAULT_RESULT_FIELDS =
            "recordID shortformat".split(" ");

    /**
     * If a result-field is not present in a given hit, the fallback-value
     * at the same array-position is returned. If no fallback-values are
     * defined, null is returned.
     * </p><p>
     * This value is extracted by calling
     * {@link dk.statsbiblioteket.summa.common.configuration.Configuration#getStrings(String)}.
     * </p><p>
     * Note: The number of fallback-values must either be null or the same
     *       as the number of result-fields.
     */
    public static final String CONF_FALLBACK_VALUES =
            "summa.search.fallback-values";
    public static final String[] DEFAULT_FALLBACK_VALUES = null;

    /**
     * The maximum number of records to return, as a long. This takes precedence
     * over the value specified in the method {@link #fullSearch}.
     */
    public static final String CONF_MAX_RECORDS = "summa.search.maxRecords";
    public long DEFAULT_MAX_NUMBER_OF_RECORDS = Long.MAX_VALUE;

    /**
     * The special sortKey signifying that sorting should be done on score,
     * thus making the search return records in order of relevance.
     */
    public static final String SORT_ON_SCORE = "summa-score";

    /**
     * The complete search with all possible parameters. The filterQuery
     * narrows the search-field, using the exact same syntax as a query.
     * The query matches documents aka records and a maximum of maxRecords
     * record-representations are returned, starting from position startIndex,
     * counting from 0.<br />
     * If sortKey is defined, the matches are sorted by the given key, in
     * reverse order if reverseSort is true. fields and defaultValues define
     * how the records should be represented.
     * </p><p>
     * The result is returned in XML:<br />
     * {@code
     * <?xml version="1.0" encoding="UTF-8"?>
     * <searchresult filter="..." query="..."
     *               startIndex="..." maxRecords="..."
     *               sortKey="..." reverseSort="..."
     *               fields="..." searchTime="...">
     *   <record score="..." sortValue="...">
     *     <field="recordID">...</field>
     *     <field="shortformat">...</field>
     *   </record>
     *   ...
     * </searchresult>
     * }
     * sortValue is the value that the sort was performed on. If the result from
     * several searchers are to be merged, merge-ordering should be dictated by
     * this value.<br />
     * score is the score-value returned by the index implementation.<br />
     * searchTime is the number of milliseconds it took to perform the search.
     * </p><p>
     * Optional parameters can be null, signifying that they are not defined.
     * The content in the XML is entity-escaped.
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
     * @param fields      the fields to extract content from.
     *                    This parameter is optional. Default is specified
     *                    in the conf. at {@link #CONF_RESULT_FIELDS}.
     * @param fallbacks   if the value of a given field cannot be extracted,
     *                    the corresponding value from fallbacks is returned.
     *                    Note that the length of fallbacks and fields must
     *                    be the same.
     * @return the result of a search in XML, as specified above.
     * @throws RemoteException if there was an exception during search.
     */
    public String fullSearch(String filter, String query,
                             long startIndex, long maxRecords,
                             String sortKey, boolean reverseSort,
                             String[] fields, String[] fallbacks)
            throws RemoteException;
}
