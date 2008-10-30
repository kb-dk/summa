/* $Id: LowLevelSearch.java,v 1.7 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.7 $
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

import dk.statsbiblioteket.summa.common.search.response.ResponseOutputStream;
import dk.statsbiblioteket.summa.common.search.response.ResponseWriter;
import dk.statsbiblioteket.summa.common.search.request.RequestReader;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public interface LowLevelSearch {
    /**
     * Perform a search with the given arguments. The result must conform to the
     * {@link ResponseOutputStream}, which is optimized towards speed over a
     * local network.
     * Implementations should begin to stream the result as soon as possible,
     * even if complete transmission cannot be guaranteed.
     *
     * @param query Query string.
     * @param numberOfRecords The number of records to return.
     * @param filterQuery The filter is used before the query. A value of null
     *                    or "" means no filtering.
     * @param sortKey the key defining the sort. Valid strings are field-names,
*                     as specified in the search-descriptor.
     * @param reverse if true, the order of sort is reversed.
     * @param queryLang the language, as specified in ISO 639-1.
     * @param maxScore the maximum score for the documents returned.
     * @param output where the result of the query should be stored.
     */
    public void query(String query,
                      int numberOfRecords,
                      String filterQuery,
                      String sortKey,
                      boolean reverse,
                      String queryLang,
                      Float maxScore,
                      ResponseWriter output);

    public void query(RequestReader request,
                      ResponseWriter response);

    /**
     *
     */
    public void abortSearch();
}



