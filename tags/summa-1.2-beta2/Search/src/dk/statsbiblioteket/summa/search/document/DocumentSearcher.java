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
package dk.statsbiblioteket.summa.search.document;

import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;

/**
 * A DocumentSearcher performs a query-based search and returns a set of fields
 * from a corpus of documents. The base example is a Lucene searcher.
 * </p><p>
 * All constants prefixed with SEARCH maps directly to from {@link Request} to
 * {@link #fullSearch( String, String, long, long, String, boolean, String[],
 * String[])}. The only mandatory value is {@link #SEARCH_QUERY}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface DocumentSearcher extends SearchNode, DocumentKeys {

    /**
     * The default sort option. This can either be the name of a field or
     * the string {@link #SORT_ON_SCORE} ("summa-score") which signifies that
     * sorting should be done according to ranking.
     * </p><p>
     * This is optional. Default is {@link #SORT_ON_SCORE}.
     */
    public static final String CONF_DEFAULT_SORTKEY =
            "summa.search.defaultsortkey";
    public static final String DEFAULT_DEFAULT_SORTKEY = DocumentKeys.SORT_ON_SCORE;

    /**
     * The maximum number of records to return, as a long. This takes precedence
     * over the value specified in the method {@link #fullSearch}.
     */
    public static final String CONF_MAX_RECORDS = "summa.search.maxrecords";
    public long DEFAULT_MAX_NUMBER_OF_RECORDS = Long.MAX_VALUE;

    /**
     * When a search has been performed by the underlying index searcher,
     * the content of these fields should be returned.
     * Note that this can be overrided by the {@link #fullSearch}-method.
     * </p><p>
     * This value is extracted by calling
     * {@link dk.statsbiblioteket.summa.common.configuration.Configuration#getStrings(String)}.
     */
    public static final String CONF_RESULT_FIELDS = "summa.search.resultfields";
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
            "summa.search.fallbackvalues";
    public static final String[] DEFAULT_FALLBACK_VALUES = null;

    /**
     * A list of the result-fields that should not be entity-escaped.
     * </p><p>
     * Optional. If not specified, all result-fields are entity-escaped when
     * Response-XML is generated.
     */
    public static final String CONF_NONESCAPED_FIELDS =
            "summa.search.nonescapedfields";

    /**
     * If a start-index is not specified in the Request, this value is used.
     * </p></p>
     * Optional. Default is {@link #DEFAULT_START_INDEX} (0).
     */
    public static final String CONF_START_INDEX = "summa.search.startindex";
    public static final long DEFAULT_START_INDEX = 0;

    /**
     * If max-records is not specified in the Request, this value is used.
     * </p><p>
     * Optional. Default is {@link #DEFAULT_RECORDS} (20).
     */
    public static final String CONF_RECORDS = "summa.search.records";
    public static final long DEFAULT_RECORDS = 20;

    /**
     * If true, docIDs for all hits in the search are collected and send on
     * through the chain of search-nodes. This enables later nodes to piggy-back
     * on the search.
     * </p><p>
     * This must be true for the FacetBrowser search node to work.
     * </p><p>
     * The document ids are collected in a {@link DocIDCollector} which is
     * stored temporarily with the key "documentsearcher.docids".
     * </p><p>
     * Optional. Default is false.
     */
    public static final String CONF_COLLECT_DOCIDS =
            "summa.search.collectdocids";
    public static final boolean DEFAULT_COLLECT_DOCIDS = false;
    public static final String DOCIDS = "documentsearcher.docids";

    /**
     * The result fields are the fields extracted from each hit from a search.
     * @return The result fields for the searcher.
     * @throws RemoteException if the fields could not be retrieved.
     * @see #CONF_RESULT_FIELDS
     */
    public String[] getResultFields() throws RemoteException;

    /**
     * The result fields are the fields extracted from each hit from a search.
     * </p><p>
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param fieldNames the result fields to use in the searcher.
     * @throws RemoteException if the fields could not be set.
     * @see #CONF_RESULT_FIELDS
     */
    // TODO: consider merging this setter with the fallback value setter
    public void setResultFields(String[] fieldNames) throws RemoteException;


    /**
     * If a the content of a result field cannot be extracted from a document,
     * the value at the same array position is returned.
     * @return The fallback values for the searcher.
     * @throws RemoteException if the values could not be retrieved.
     * @see #CONF_FALLBACK_VALUES
     */
    public String[] getFallbackValues() throws RemoteException;

    /**
     * If a the content of a result field cannot be extracted from a document,
     * the value at the same array position is returned.
     * </p><p>
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param fallbackValues the new fallback values. Null is an acceptable
     *                       value and will result in null being used as
     *                       default value for all result fields.
     * @throws RemoteException if the values could not be set.
     * @see #CONF_FALLBACK_VALUES
     */
    public void setFallbackValues(String[] fallbackValues) throws
                                                           RemoteException;

    /**
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param maxRecords the maximum number of records (hits) this searcher is
     *                   willing to return, providing a limit on the number
     *                   explicitely requested when a search is invoked.
     * @throws RemoteException if the value could not be changed.
     * @see #CONF_MAX_RECORDS
     */
    public void setMaxRecords(long maxRecords) throws RemoteException;

    /**
     * @return the default sort key for searches.
     * @throws RemoteException if the key could not be determined.
     * @see #CONF_DEFAULT_SORTKEY
     */
    public String getSortKey() throws RemoteException;

    /**
     * This value should only be tweaked manually for experimental purposes, as
     * it is not persistent across instantiations of searchers. In order to make
     * the change persistent, the underlying configuration for the searcher must
     * be updated - this is normally done through the Control module.
     * @param sortKey the default sort key for searches.
     * @throws RemoteException if the value could not be changed.
     * @see #CONF_DEFAULT_SORTKEY
     */
    public void setSortKey(String sortKey) throws RemoteException;

    /**
    * @return the maximum number of records (hits) this searcher is willing to
    *         return, providing a limit on the number explicitely requested
    *         when a search is invoked.
    * @throws RemoteException if the value could not be retrieved.
    * @see #CONF_MAX_RECORDS
    */
    public long getMaxRecords() throws RemoteException;

    /**
     * @return the default start index.
     * @throws RemoteException if the value could not be retrieved.
     */
    public long getStartIndex() throws RemoteException;

    /**
     * @param startIndex the default start index.
     * @throws RemoteException if the value could not be changed.
     */
    public void setStartIndex(long startIndex) throws RemoteException;

    /**
     * @return the default number of records to return.
     * @throws RemoteException if the value could not be retrieved.
     */
    public long getRecords() throws RemoteException;

    /**
     * @param records the default number of records to return.
     * @throws RemoteException if the value could not be changed.
     */
    public void setRecords(long records) throws RemoteException;

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
    public String simpleSearch(String query, long startIndex, long maxRecords)
                                                         throws RemoteException;

    /**
     * The complete search with all possible parameters. The filterQuery
     * narrows the search-field, using the exact same syntax as a query.
     * The query matches documents aka records and a maximum of maxRecords
     * record-representations are returned, starting from position startIndex,
     * counting from 0.<br />
     * If sortKey is defined, the matches are sorted by the given key, in
     * reverse order if reverseSort is true. fields and defaultValues define
     * how the records should be represented.<br />
     * Optional parameters can be null, signifying that they are not defined.
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
     * @return the result of a search, suitable for merging and XML generation.
     * @throws RemoteException if there was an exception during search.
     */
    public DocumentResponse fullSearch(String filter, String query,
                                   long startIndex, long maxRecords,
                                   String sortKey, boolean reverseSort,
                                   String[] resultFields, String[] fallbacks)
            throws RemoteException;

}