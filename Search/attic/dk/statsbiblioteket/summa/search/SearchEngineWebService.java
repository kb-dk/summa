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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory; 

import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;


@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, mv")
public class SearchEngineWebService {

    private static final Log log = LogFactory.getLog(SearchEngineWebService.class);
    private static final boolean isDebug = log.isDebugEnabled();
    private static final boolean isInfo = log.isInfoEnabled();



    public String getRecord(String recordID) throws RemoteException {
       return SearchEngineImpl.getInstance().getRecord(recordID);
    }

    public String getShortRecord(String recordID) {
       return SearchEngineImpl.getInstance().getShortRecord(recordID);
    }

    public String simpleSearch(String query, int numberOfRecords, int startIndex) {
        if (isInfo){log.info("called simple search");}
        String res =   SearchEngineImpl.getInstance().simpleSearch(query, numberOfRecords, startIndex);
        if (isDebug){log.debug("result: " + res);}
        return res;
    }

    public int getHitCount(String query) {
        return SearchEngineImpl.getInstance().getHitCount(query);
    }

    public String simpleSearchSorted(String query, int numberOfRecords, int startIndex, String sortKey, boolean reverse) {
        return SearchEngineImpl.getInstance().simpleSearchSorted(query,numberOfRecords,startIndex,sortKey, reverse);
    }

    public String filteredSearch(String query, int numberOfRecords, int startIndex, String filterQuery) {
        return SearchEngineImpl.getInstance().filteredSearch(query,numberOfRecords,startIndex,filterQuery);
    }

    public String filteredSearchSorted(String query, int numberOfRecords, int startIndex, String filterQuery, String sortKey, boolean reverse) {
        return SearchEngineImpl.getInstance().filteredSearchSorted(query,numberOfRecords,startIndex,filterQuery,sortKey, reverse);
    }

    public String getSearchDescriptor() {
        return SearchEngineImpl.getInstance().getSearchDescriptor();
    }

    /**
     * @deprecated This method should be moved to dedicated webservice
     */
    public String getSimilarDocuments(String recordID, int numberOfRecords, int startIndex) {
        return SearchEngineImpl.getInstance().getSimilarDocuments(recordID,numberOfRecords,startIndex);
    }

    public String getQueryLang() {
        return SearchEngineImpl.getInstance().getQueryLang();
    }

    /**
     * @deprecated This method should be moved to dedicated webservice
     */
    public String getOpenUrl(String recordID){
        return SearchEngineImpl.getInstance().getOpenUrl(recordID);
    }

    /**
     * //TODO: Does this method belong in the storage ws?
     * @deprecated
     */
    public int getItemCount (String recordID) {
        return SearchEngineImpl.getInstance().getItemCount(recordID);
    }

    /**
     * //TODO: Does this method belong in the storage ws?
     * @deprecated
     */
    public int[] getItemCounts (String[] recordIDs) {
        return SearchEngineImpl.getInstance().getItemCounts(recordIDs);
    }
}


