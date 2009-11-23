package dk.statsbiblioteket.summa.search.api.tools;

import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SearchClient;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Naming;

/**
 * Little tool to hook up to a {@link SummaSearcher} and launch a search
 * on it and print the result to stdout.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "mke")
public class SearchTool {

    public static void main (String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println ("USAGE:\n\t" +
                                "search-tool.sh <key=val> [key=val]...");
            System.err.println ("Examples:");
            System.err.println ("\tsearch-tool.sh " +
                                "search.document.query=foo\n");
            System.err.println ("\tsearch-tool.sh " +
                                "search.document.lucene.morelikethis.recordid=myRecordId27\n");
            System.err.println ("\tsearch-tool.sh " +
                                "search.index.field=lme search.index.term=danmark\n");
            System.err.println ("Optionally you may set the CONFIGURATION" +
                                "variable in your shell and it will be used for" +
                                "the summa.configuration property\n");            
            System.exit (1);
        }

        Configuration conf;
        String rpcVendor;

        /* Try and open the system config */
        try {
            conf = Configuration.getSystemConfiguration(false);
        } catch (Configurable.ConfigurationException e) {
            System.err.println ("Unable to load system config: " + e.getMessage()
                                +".\nUsing default configuration");
            conf = Configuration.newMemoryBased();
        }

        /* Make sure the summa.rpc.vendor property is set */
        if (!conf.valueExists(ConnectionConsumer.CONF_RPC_TARGET)) {
            rpcVendor = System.getProperty(ConnectionConsumer.CONF_RPC_TARGET);

            if (rpcVendor != null) {
                conf.set (ConnectionConsumer.CONF_RPC_TARGET, rpcVendor);
            } else {
                conf.set (ConnectionConsumer.CONF_RPC_TARGET,
                          "//localhost:28000/summa-searcher");
            }
        }


        System.err.print("Connecting to searcher on "
                         + conf.getString(ConnectionConsumer.CONF_RPC_TARGET)
                         + " ... ");
        SearchClient searcher = new SearchClient (conf);
        System.err.println("[OK]");

        Request rq = parseArgs (args);

        System.err.print("Performing search ... ");
        long time = System.currentTimeMillis();
        ResponseCollection resp = searcher.search(rq);
        time = System.currentTimeMillis() - time;
        System.err.println("[OK]");

        System.err.println("Result:");
        System.err.flush();
        System.out.println (resp.toXML());
        System.err.println("Response time: " + time + "ms");
    }

    /**
     * Parse key=value pairs from the command line
     * @param args the args as passed to main()
     * @return a compiled {@link Request} object ready to pass to
     *         {@link SummaSearcher#search}
     */
    private static Request parseArgs(String[] args) {
        Request rq = new Request ();

        for (int i = 0; i < args.length; i++) {
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



