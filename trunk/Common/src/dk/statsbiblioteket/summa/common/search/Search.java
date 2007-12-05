/* $Id: Search.java,v 1.4 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.common.search;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface Search {
    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset (the first record has number 0.
     *
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * @param query Query string.
     * @param numberOfRecords The number of records to return.
     * @param startIndex The offset to start at.
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble.
     *         Note: Specify the return format.
     */
    public String simpleSearch(String query,
                               int numberOfRecords,
                               int startIndex);

    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset (the first record has number 0.
     *
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * @param query Query string.
     * @param numberOfRecords The number of records to return.
     * @param startIndex The offset to start at.
     * @param queryLang the language, as specified in ISO 639-1. This selects
     *                  the set of aliases used for query-parsing. A value of
     *                  null selects the default language.
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble.
     *         Note: Specify the return format.
     */
    public String simpleSearch(String query,
                               int numberOfRecords,
                               int startIndex,
                               String queryLang);

    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset, results will be sorted by
     * given key.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * @param query Query string.
     * @param numberOfRecords The number of records to return.
     * @param startIndex The offset to start at.
     * @param sortKey the key defining the sort. Valid strings are field-names,
     *                as specified in the search-descriptor.
     * @param reverse, if true the sortOrder is reversed.
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String simpleSearchSorted(String query,
                                     int numberOfRecords,
                                     int startIndex,
                                     String sortKey,
                                     boolean reverse);

    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset, results will be sorted by
     * given key.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * @param query Query string.
     * @param numberOfRecords The number of records to return.
     * @param startIndex The offset to start at.
     * @param sortKey the key defining the sort. Valid strings are field-names,
     *                as specified in the search-descriptor.
     * @param reverse, if true the sortOrder is reversed.
     * @param queryLang the language, as specified in ISO 639-1. This selects
     *                  the set of aliases used for query-parsing. A value of
     *                  null selects the default language.
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String simpleSearchSorted(String query,
                                     int numberOfRecords,
                                     int startIndex,
                                     String sortKey,
                                     boolean reverse,
                                     String queryLang);

    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset, results will be filtered with
     * the given filter.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * @param query Query string.
     * @param numberOfRecords The number of records to return.
     * @param startIndex The offset to start at.
     * @param filterQuery The filter is used before the query.
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String filteredSearch(String query,
                                 int numberOfRecords,
                                 int startIndex,
                                 String filterQuery);

    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset, results will be filtered with
     * the given filter.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * @param query Query string.
     * @param numberOfRecords The number of records to return.
     * @param startIndex The offset to start at.
     * @param filterQuery The filter is used before the query.
     * @param queryLang the language, as specified in ISO 639-1. This selects
     *                  the set of aliases used for query-parsing. A value of
     *                  null selects the default language.
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String filteredSearch(String query,
                                 int numberOfRecords,
                                 int startIndex,
                                 String filterQuery,
                                 String queryLang);
     
    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * @param query Query string.
     * @param numberOfRecords The number of records to return.
     * @param startIndex The offset to start at.
     * @param sortKey the key defining the sort. Valid strings are field-names,
     *                as specified in the search-descriptor.
     * @param reverse, if true the sortOrder is reversed.
     * @param filterQuery The filter is used before the query.
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String filteredSearchSorted(String query,
                                       int numberOfRecords,
                                       int startIndex,
                                       String filterQuery,
                                       String sortKey,
                                       boolean reverse);

    /** Perform a simple search for query, returning the specified number of
     * results, starting at the specified offset.
     * The query is parsed and transformed into a lucene query by the rules
     * given by the search descriptor.
     *
     * @param query Query string.
     * @param numberOfRecords The number of records to return.
     * @param startIndex The offset to start at.
     * @param sortKey the key defining the sort. Valid strings are field-names,
     *                as specified in the search-descriptor.
     * @param reverse, if true the sortOrder is reversed.
     * @param filterQuery The filter is used before the query.
     * @param queryLang the language, as specified in ISO 639-1. This selects
     *                  the set of aliases used for query-parsing. A value of
     *                  null selects the default language.
     * @return A string encoding the results in xml,
     *         or a specific xml doc on trouble
     */
    public String filteredSearchSorted(String query,
                                       int numberOfRecords,
                                       int startIndex,
                                       String filterQuery,
                                       String sortKey,
                                       boolean reverse,
                                       String queryLang);

    /**
     * Get the search descriptor in an XML string. One obvious use is to extract
     * valid sort keys, for use in queries.
     * @return The XML search descriptor, or null if impossible.
     */
    public String getSearchDescriptor();

    /**
     * Find documents similar to the document with the given recordID.
     * @param recordID        the id at record-level. This corresponds to
     *                        a single document.
     * @param numberOfRecords The number of records to return.
     * @param startIndex The offset to start at.
     * @return  A String encoding the result in xml, the result contains
     *          documents heuristically determined to be similar to the
     *          original doc.
     */
    public String getSimilarDocuments(String recordID,
                                      int numberOfRecords,
                                      int startIndex);
}
