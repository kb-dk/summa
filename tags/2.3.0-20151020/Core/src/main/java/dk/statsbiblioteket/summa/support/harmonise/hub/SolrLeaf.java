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
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubLeafImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;

/**
 * Connects to a remote Solr installation using the REST API.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrLeaf extends HubLeafImpl {
    private static Log log = LogFactory.getLog(SolrLeaf.class);

    /**
     * The entry point for calls to Solr.
     * </p><p>
     * Optional. Default is localhost:8983 (Solr default).
     */
    public static final String CONF_URL = "solr.url";
    public static final String DEFAULT_URL = "http://localhost:8983";

    /**
     * The protocol used to communicate with the Solr instance. The binary protocol is the fastest and recommended
     * if supported by the Solr instance.
     * </p><p>
     * Optional. Valid values are 'xml' and 'javabin'. Default is 'javabin'.
     */
    public static final String CONF_PROTOCOL = "solr.protocol";
    public static final String DEFAULT_PROTOCOL = PROTOCOL.xml.toString();
    public enum PROTOCOL {xml, javabin}

    /**
     * Connection timeout for communication with the remote Solr server.
     * </p><p>
     * Optional. Default is 500 milliseconds.
     */
    public static final String CONF_CONNECTION_TIMEOUT = "solr.connectiontimeout";
    public static final int DEFAULT_CONNECTION_TIMEOUT = 500;

    /**
     * Read timeout for communication with the remote Solr server.
     * </p><p>
     * Optional. Default is 10000 milliseconds (10 seconds).
     */
    public static final String CONF_READ_TIMEOUT = "solr.readtimeout";
    public static final int DEFAULT_READ_TIMEOUT = 10*1000;

    // TODO: Consider how to handle qt
    // http://wiki.apache.org/solr/SolrRequestHandler#Handler_Resolution
    // public static final String CONF_QT = "solr.qt"

    private final String url;
    private final HttpSolrServer solrServer;
    private final PROTOCOL protocol;
    private int connectionTimeout;
    private int readTimeout;

    public SolrLeaf(Configuration conf) {
        super(conf);
        url = conf.getString(CONF_URL, DEFAULT_URL);
        protocol = PROTOCOL.valueOf(conf.getString(CONF_PROTOCOL, DEFAULT_PROTOCOL));
        connectionTimeout = conf.getInt(CONF_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        readTimeout = conf.getInt(CONF_READ_TIMEOUT, DEFAULT_READ_TIMEOUT);
        solrServer = new HttpSolrServer(url);
        solrServer.setConnectionTimeout(connectionTimeout);
        solrServer.setSoTimeout(readTimeout);
        log.info("Created " + this);
    }

    @Override
    public QueryResponse search(ModifiableSolrParams params) throws SolrServerException {
        params.remove("wt");
        params.add("wt", protocol.toString());
        try {
            return solrServer.query(params);
        } catch (SolrException e) {
            throw new SolrException(SolrException.ErrorCode.getErrorCode(e.code()),
                                    "SolrException in searcher '" + getID() + "' while querying '"
                                    + solrServer.getBaseURL() + "' with " + params, e);
        }
    }

    public HttpSolrServer getSolrServer() {
        return solrServer;
    }

    public String toString() {
        return "SolrLeaf(" + super.toString() + ", url='" + url + "', protocol=" + protocol
               + ", connectionTimeout=" + connectionTimeout + ", readTimeout=" + readTimeout + ")";
    }
}
