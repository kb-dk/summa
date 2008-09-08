package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.document.DocumentKeys;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: mv
 * Date: Sep 4, 2008
 * Time: 1:37:38 PM
 */
/**
 * Created by IntelliJ IDEA.
 * User: mv
 * Date: Sep 4, 2008
 * Time: 9:50:54 PM
 * To change this template use File | Settings | File Templates.
 */
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
            retXML = "<foo error=\"\">Error performing query</foo>";
        }

        return retXML;
    }
}
