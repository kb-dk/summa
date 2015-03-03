/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.search.api;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.representation.Form;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.ConnectionConsumer;
import dk.statsbiblioteket.summa.common.rpc.GenericConnectionFactory;
import dk.statsbiblioteket.summa.search.rmi.SummaRest;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * A helper class utilizing a stateless connection to a search engine exposing
 * a {@link SummaSearcher} interface. Unless your needs are very advanced
 * or you must do manual connection management, this is by far the easiest way
 * to use a remote {@link SummaSearcher}.
 * <p></p>
 * IF RMI is used, the {@link ConnectionConsumer} is activated, meaning that you can tweak
 * its behavior by changing the configuration parameters
 * {@link GenericConnectionFactory#CONF_RETRIES},
 * {@link GenericConnectionFactory#CONF_GRACE_TIME},
 * {@link GenericConnectionFactory#CONF_FACTORY}, and
 * {@link ConnectionConsumer#CONF_RPC_TARGET}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "mke")
public class SearchClient implements Configurable, SummaSearcher {
    private static Log log = LogFactory.getLog(SearchClient.class);

    /**
     * The remote server to use. If this starts with {@code //}, RMI is used as per
     * {@link ConnectionConsumer#CONF_RPC_TARGET}. Else REST is assumed with backend
     * {@link dk.statsbiblioteket.summa.search.rmi.SummaRest}.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_SERVER = "searchclient.server";

    /**
     * If defined and REST is used, the searcher with the specified ID will be used for requests.
     * Specifying {@code ""} means the default searcher at the server will be used.
     * </p><p>
     * Note: This can be overwritten at request time.
     * </p><p>
     * Optional. Default is {@code ""} (the default searcher at the server).
     */
    public static final String CONF_SEARCHER_ID = "searchclient.server.searcherid";
    public static final String SEARCH_SEARCHER_ID = CONF_SEARCHER_ID;
    public static final String DEFAULT_SEARCHER_ID = "";

    /**
     * Connection timeout in ms, when using REST.
     * </p><p>
     * Optional. Default is 500.
     */
    public static final String CONF_TIMEOUT_CONNECT = "searchclient.timeout.connect";
    public static final int DEFAULT_TIMEOUT_CONNECT = 500;

    /**
     * Read timeout in ms, when using REST.
     * </p><p>
     * Optional. Default is 3600000 (1 hour).
     */
    public static final String CONF_TIMEOUT_READ = "searchclient.timeout.read";
    public static final int DEFAULT_TIMEOUT_READ = 60*60*1000;

    /**
     * Whether or not this client is enabled. If not enabled, an empty ResponseCollection will be returned when
     * {@link #search(Request)} is called.
     */
    public static final String CONF_ENABLED = "client.enabled";
    public static final boolean DEFAULT_ENABLED = true;

    private final String server;
    private final boolean enabled;

    private final ConnectionConsumer<SummaSearcher> rmi;

    private final int timeoutConnect;
    private final int timeoutRead;
    private final ClientConfig config = new DefaultClientConfig();
    private final Client client = Client.create(config);
    private final WebResource rest;
    public final String defaultSearcherID;

    public SearchClient(Configuration conf) {
        server = conf.getString(CONF_SERVER, conf.getString(ConnectionConsumer.CONF_RPC_TARGET, null));
        if (server == null || server.isEmpty()) {
            throw new ConfigurationException("The property " + CONF_SERVER + " must be specified");
        }
        defaultSearcherID = conf.getString(CONF_SEARCHER_ID, DEFAULT_SEARCHER_ID);
        enabled = conf.getBoolean(CONF_ENABLED, DEFAULT_ENABLED);
        timeoutConnect = conf.getInt(CONF_TIMEOUT_CONNECT, DEFAULT_TIMEOUT_CONNECT);
        timeoutRead = conf.getInt(CONF_TIMEOUT_READ, DEFAULT_TIMEOUT_READ);

        if (server.startsWith("//")) {
            log.debug("Establishing RMI connection to " + server);
            if (!conf.containsKey(ConnectionConsumer.CONF_RPC_TARGET)) {
                conf.set(ConnectionConsumer.CONF_RPC_TARGET, server);
            }
            rmi = new ConnectionConsumer<>(conf);
            rest = null;
        } else {
            log.debug("Creating REST connector to " + server);
            rmi = null;
            client.setConnectTimeout(timeoutConnect);
            client.setReadTimeout(timeoutRead);
            rest = client.resource(server);
        }

        log.debug("Created " + this);
    }

    /**
     * Perform a search on the remote {@link SummaSearcher}. Connection handling
     * is done transparently underneath.
     * 
     * @param request The request to pass.
     * @return What ever response the search engine returns.
     * @throws IOException on communication errors with the search engine.
     */
    @Override
    public ResponseCollection search(Request request) throws IOException {
        if (!enabled) {
            log.debug("Skipping search to " + server + " as the SearchClient is disabled");
            return new ResponseCollection();
        }
        log.debug("Performing RMI based remote search against " + server);
        if (rmi != null) {
            return searchRMI(request);
        }
        log.debug("Performing REST based remote search against " + server);
        return searchRest(request);
    }

    private ResponseCollection searchRest(Request request) {
        Form form = new Form();
        form.add(SummaRest.ID, request.getString(SEARCH_SEARCHER_ID, defaultSearcherID));
        form.add(SummaRest.REQUEST, request);
        try {
            ClientResponse response = rest.path("rest").path("searchBinary")
                    .type(MediaType.APPLICATION_FORM_URLENCODED).post(ClientResponse.class, form);
            return response.getEntity(ResponseCollection.class);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get REST response for " + request + ", from " + server, e);
        }
    }

    private ResponseCollection searchRMI(Request request) throws IOException {
        long connectTime = -System.currentTimeMillis();
        SummaSearcher searcher = rmi.getConnection();
        connectTime += System.currentTimeMillis();

        if (searcher == null) {
            final String msg = "The searcher retrieved from getConnection(" + server
                               + ") was null after " + connectTime + "ms";
            log.warn(msg);
            throw new IOException("Unable to connect to '" + server + "' in " + connectTime + "ms");
        }
        try {
            return searcher.search(request);
        } catch (Throwable t) {
            rmi.connectionError(t);
            throw new IOException("Search failed against " + server + ": " + t.getMessage(), t);
        } finally {
            rmi.releaseConnection();
        }
    }

    @Override
    public void close() throws IOException {
        if (rmi != null) {
            rmi.releaseConnection();
        }
    }

    public String getVendorId() {
        return server;
        //return rmi.getVendorId();
    }

    @Override
    public String toString() {
        return String.format("SearchClient(enabled=%b, server='%s' (%s)",
                             enabled, server, rmi == null ? "REST" :
                "RMI, readTimeout=" + timeoutConnect + "ms, readTimeout=" + timeoutRead + "ms");
    }
}
