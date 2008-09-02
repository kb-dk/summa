package dk.statsbiblioteket.summa.search.api;

import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

import java.io.IOException;

/**
 * A helper class utilizing a stateless connection to a search engine exposing
 * a {@link SummaSearcher} interface. Unless your needs are very advanced
 * or you must do manual connection management, this is by far the
 * easiest way to use a remote {@link SummaSearcher}.
 * <p></p>
 * It is modelled as a {@link ConnectionConsumer} meaning that you can tweak
 * its behavior by changing the configuration parameters
 * {@link GenericConnectionFactory#RETRIES},
 * {@link GenericConnectionFactory#GRACE_TIME},
 * {@link GenericConnectionFactory#FACTORY}, and
 * {@link ConnectionConsumer#PROP_RPC_TARGET}
 */
public class SearchClient extends ConnectionConsumer<SummaSearcher>
                          implements Configurable {

    public SearchClient (Configuration conf) {
        super (conf);
    }

    /**
     * Perform a search on the remote {@link SummaSearcher}. Connection handling
     * is done transparently underneath.
     * 
     * @param request the request to pass
     * @return what ever response the search engine returns
     * @throws IOException on communication errros with the search engine
     */
    public ResponseCollection search (Request request) throws IOException {
        SummaSearcher searcher = getConnection();

        try {
            return searcher.search(request);
        } catch (Throwable t) {
            connectionError(t);
            throw new IOException("Search failed: " + t.getMessage(), t);
        } finally {
            releaseConnection();
        }
    }

}
