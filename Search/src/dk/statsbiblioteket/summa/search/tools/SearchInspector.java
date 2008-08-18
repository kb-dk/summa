package dk.statsbiblioteket.summa.search.tools;

import dk.statsbiblioteket.summa.search.SummaSearcher;
import dk.statsbiblioteket.summa.search.Request;
import dk.statsbiblioteket.summa.search.ResponseCollection;

import java.rmi.Naming;

/**
 * Little tool to hook up to a {@link SummaSearcher} and launch a search
 * on it
 */
public class SearchInspector {

    public static void main (String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println ("USAGE:\n\t" +
                                "search-tool.sh <rmi-address> <key=val> [key=val]...");
            System.err.println ("Example:\n\tsearch-tool.sh " +
                                "//localhost:28000/summa-searcher summa.query=foo");
            System.exit (1);
        }

        System.err.print("Looking up searcher on: " + args[0] + " ... ");
        SummaSearcher searcher = (SummaSearcher)Naming.lookup (args[0]);
        System.err.println("[OK]");

        Request rq = parseArgs (args);

        System.err.print("Performing request... ");
        ResponseCollection resp = searcher.search(rq);
        System.err.println("[OK]");

        System.err.println("Result:");
        System.out.println (resp.toXML());
    }

    private static Request parseArgs(String[] args) {
        Request rq = new Request ();

        /* Start at offset 1, since arg 0 is the rmi address  */
        for (int i = 1; i < args.length; i++) {
            String[] keyVal = args[i].split ("=");
            if (keyVal.length != 2) {
                throw new RuntimeException ("Argument '" + args[i] + "' is not a"
                                            + " valid assignment string. Fx "
                                            + "summa.foo=bar");
            }
            rq.put (keyVal[0], keyVal[1]);
        }
        return rq;
    }
}
