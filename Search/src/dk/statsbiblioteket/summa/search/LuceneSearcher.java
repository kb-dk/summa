/**
 * Created: te 30-05-2008 16:01:36
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.rmi.RemoteException;

/**
 *
 */
public class LuceneSearcher extends SummaSearcherImpl {
    /**
     * The maximum number of boolean clauses that a query can be expanded to.
     */
    public static final String CONF_MAX_BOOLEAN_CLAUSES = "search.clauses.max";
    public static final int DEFAULT_MAX_BOOLEAN_CLAUSES = 10000;
    private int maxBooleanClauses = DEFAULT_MAX_BOOLEAN_CLAUSES;

    public LuceneSearcher(Configuration conf) {
        super(conf);
        maxBooleanClauses =
                conf.getInt(CONF_MAX_BOOLEAN_CLAUSES, maxBooleanClauses);
        // TODO: Connect to index
        // TODO: Warm-up (no logging of searches during warm-up)
        //
    }

    public SearchNodeWrapper constructSearchNode(Configuration conf) {
        return null;  // TODO: Implement this
    }

    // TODO: Add Profiler to LuceneSearche

    public String fullSearch(String filter, String query,
                             long startIndex, long maxRecords,
                             String sortKey, boolean reverseSort, 
                             String[] fields, String[] fallbacks) throws
                                                               RemoteException {
        return null;  // TODO: Implement this
    }
}
