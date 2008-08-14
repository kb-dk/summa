package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link SearchNode} implementation that simply returns statistics about
 * its usage. This class is mainly used for debugging.
 */
public class SearchNodeVoid extends SearchNodeImpl {

    /**
     * Configuration property defining what the {@code SearchNode} puts
     * in its repsonse to {@link SearchNodeImpl#managedSearch}.
     * Default is "Void (request_num)".
     */
    public static final String PROP_SEARCH_RESP = "summa.search.void.response";

    private class VoidResponse implements Response {

        protected String warmUps;
        protected String opens;
        protected String closes;
        protected String searches;

        public VoidResponse (int warmUps, int opens, int closes, int searches) {
            this.warmUps = "" + warmUps;
            this.opens = "" + opens;
            this.closes = "" + closes;
            this.searches = "" + searches;
        }

        public String getName () {
            return "VoidResponse";
        }

        public void merge (Response other) throws ClassCastException {
            VoidResponse resp = (VoidResponse)other;
            warmUps += ", " + resp.warmUps;
            opens += ", " + resp.opens;
            closes += ", " + resp.closes;
            searches += ", " + resp.searches;
        }

        public String toXML () {
            return String.format ("<VoidResponse>\n" +
                                  "  <warmUps>%s</warmUps>\n"+
                                  "  <opens>%s</opens>\n"+
                                  "  <closes>%s</closes>\n"+
                                  "  <searches>%s</searches>\n"+
                                  "</VoidResponse>",
                                  warmUps, opens, closes, searches);
        }
    }

    private static final Log log = LogFactory.getLog (SearchNodeVoid.class);

    private int warmupCount;
    private int openCount;
    private int closeCount;
    private int searchCount;

    public SearchNodeVoid (Configuration conf) {
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

        responses.add (new VoidResponse (warmupCount, openCount,
                                         closeCount, searchCount));
        searchCount++;
    }
}
