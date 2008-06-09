/* $Id: SearchEngine.java,v 1.17 2007/10/11 12:56:24 te Exp $
 * $Revision: 1.17 $
 * $Date: 2007/10/11 12:56:24 $
 * $Author: te $
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

import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * @deprecated in favor of {@link SummaSearcher}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public interface SearchEngine {

    /** Then name of the configuration resource */
    public static final String CONFIG = "search.properties.xml";

    /** Property name for the number of retries if there are errors reading the index */
    public static final String RETRIES = "search.retries";

    /** Property name for the location of the main index */
    public static final String INDEX_MAIN = "search.index.main";

    /** This serves as a base for constructing property names for the locations of
     * parallel indexes. The property names will be constructed as
     * {@code INDEX_PARALLEL + "." + count} where {@code count} is an integer numerating
     * the parallel indexes starting at {@code 1}. */
    public static final String INDEX_PARALLEL = "search.index.parallel";

    /** Property name of the service providing the {@link dk.statsbiblioteket.summa.storage.io.AccessRead} interface for metadata retrieval */
    public static final String IO_SERVICE = "search.io.service";

    /** Property name for the resource name of the {@link dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor} xslt */
    public static final String SEARCH_DESCRIPTOR_XSLT = "search.descriptor.xslt";

    /** Property name for the maximum number of boolean clauses in the expanded search */
    public static final String MAX_BOOLEAN_CLAUSES = "search.clauses.max";

    /** Read a record from storage with a given ID.
     *
     * @param recordID of the record to read
     * @return The record
     * @throws RemoteException On trouble communicating with storage.
     */
    public String getRecord(String recordID) throws RemoteException;


    /** Read a short record from index
     *
     * @param recordID
     * @return The dublincore short record format;
     */
    public String getShortRecord(String recordID);


    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * This method also updates lastResponseTime.
     *
     * @param query Query string
     * @param numberOfRecords The number of records to return
     * @param startIndex The offset to start at
     * @return A string encoding the results in xml,
     * or a specific xml doc on trouble
     */
    public String simpleSearch(String query, int numberOfRecords, int startIndex);

    /**
     * 
     * @param query
     * @return the number of hits from a simple search
     */
    public int getHitCount(String query);


    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset, results will be sorted by
     * given key.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * This method also updates lastResponseTime.
     *
     * @param query Query string
     * @param numberOfRecords The number of records to return
     * @param startIndex The offset to start at
     * @param sortKey the key defining the sort
     * @param reverse, if true the sortOrder is reversed
     * @return A string encoding the results in xml,
     * or a specific xml doc on trouble
     */
    public String simpleSearchSorted(String query, int numberOfRecords, int startIndex, String sortKey, boolean reverse);

    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset, results will be filtered with
     * the given filter.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * This method also updates lastResponseTime.
     *
     * @param query Query string
     * @param numberOfRecords The number of records to return
     * @param startIndex The offset to start at
     * @return A string encoding the results in xml,
     * or a specific xml doc on trouble
     */
    public String filteredSearch(String query, int numberOfRecords, int startIndex, String filterQuery);

    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * This method also updates lastResponseTime.
     *
     * @param query Query string
     * @param numberOfRecords The number of records to return
     * @param startIndex The offset to start at
     * @param sortKey the key defining the sort
     * @param reverse, if true the sortOrder is reversed
     * @return A string encoding the results in xml,
     * or a specific xml doc on trouble
     */
    public String filteredSearchSorted(String query, int numberOfRecords, int startIndex, String filterQuery, String sortKey, boolean reverse);

    /**
     * Get the search descriptor in an XML string
     * @return The XML search descriptor, or null if impossible.
     */
    public String getSearchDescriptor();


    /**
     *
     * @param recordID
     * @param numberOfRecords
     * @param startIndex
     * @return  A String encoding the result in xml, the result contains documents heuristically determined
     * to be similar to the original doc.
     */
    public String getSimilarDocuments(String recordID, int numberOfRecords, int startIndex);

    public SummaQueryParser getSummaQueryParser();

    public String getQueryLang();

    /**
     * This will the queryPart of an openUrl on a record of null, if an openUrl has not been
     * @param recordID
     * @return the ope
     */
    public String getOpenUrl(String recordID);

    public int getItemCount(String recordID);

    public int[] getItemCounts(String[] recordIDs);
}
