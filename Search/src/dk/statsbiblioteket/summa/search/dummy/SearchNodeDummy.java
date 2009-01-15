package dk.statsbiblioteket.summa.search.dummy;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNodeImpl;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.dummy.DummyResponse;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link dk.statsbiblioteket.summa.search.SearchNode} implementation that
 * simply returns statistics about its usage. Use it by plugging it into
 * a {@link dk.statsbiblioteket.summa.search.SummaSearcherImpl}.
 * <p></p>
 * If you need a stand alone dummy searcher use {@link SummaSearcherDummy}
 * instead.
 * <p></p>
 * This class is mainly used for debugging.
 *
 * @see SummaSearcherDummy
 * @see dk.statsbiblioteket.summa.search.SummaSearcherImpl
 * @see dk.statsbiblioteket.summa.search.api.dummy.DummyResponse
 */
public class SearchNodeDummy extends SearchNodeImpl {

    private static final Log log = LogFactory.getLog (SearchNodeDummy.class);

    private int warmupCount;
    private int openCount;
    private int closeCount;
    private int searchCount;

    public SearchNodeDummy(Configuration conf) {
        super (conf);

        warmupCount = 0;
        openCount = 0;
        closeCount = 0;
        searchCount = 0;
    }

    protected void managedWarmup (String request) {
        log.info ("Warmup (" + warmupCount + "): " + request);
        warmupCount++;
    }

    protected void managedOpen (String location) throws RemoteException {
        log.info ("Open (" + openCount + "): " + location);
        openCount++;
    }

    protected void managedClose () throws RemoteException {
        log.info ("Close ("+closeCount+")");
        closeCount++;
    }

    protected void managedSearch (Request request, ResponseCollection responses) throws RemoteException {
        log.info ("Search:\tRequest:" + request + "\n\tResponses: " + responses);

        responses.add (new DummyResponse(warmupCount, openCount,
                                         closeCount, searchCount));
        searchCount++;
    }
}



