/* $Id: SearchWS.java,v 1.2 2007/10/04 13:28:21 mv Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
 * $Author: mv $
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
package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;

/**
 * A class containing methods meant to be exposed as a web service
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mv")
public class SearchWS {
    SearchClient searcher;
    Configuration conf;

    private SearchClient getSearchClient() {
        if (this.searcher == null) {
            if (this.conf == null) {
                this.conf = Configuration.getSystemConfiguration(true);
            }
            this.searcher = new SearchClient(this.conf);
        }
        return this.searcher;
    }

    public String simpleSearch(String query, int numberOfRecords, int startIndex) {
        return simpleSearchSorted(query, numberOfRecords, startIndex, DocumentKeys.SORT_ON_SCORE, false);
    }

    public String simpleSearchSorted(String query, int numberOfRecords, int startIndex, String sortKey, boolean reverse) {
        String retXML;

        ResponseCollection res;

        Request req = new Request();
        req.put(DocumentKeys.SEARCH_QUERY, query);
        req.put(DocumentKeys.SEARCH_MAX_RECORDS, numberOfRecords);
        req.put(DocumentKeys.SEARCH_START_INDEX, startIndex);
        req.put(DocumentKeys.SEARCH_SORTKEY, sortKey);
        req.put(DocumentKeys.SEARCH_REVERSE, reverse);

        try {
            res = getSearchClient().search(req);
            retXML = res.toXML();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            retXML = "<error>Error performing query</error>";
        }

        return retXML;
    }
}
