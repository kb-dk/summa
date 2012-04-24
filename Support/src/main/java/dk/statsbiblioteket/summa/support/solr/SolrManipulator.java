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
package dk.statsbiblioteket.summa.support.solr;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Receives Records containing Solr Documents intended for indexing in Solr and delivers them to an external Solr using
 * HTTP.
 * </p><p>
 * The first version is expected to flush received documents one at a time, which is inefficient but simple.
 * For later versions a batching model should be considered.
 * See {@link dk.statsbiblioteket.summa.storage.api.filter.RecordWriter}
 * and {@link dk.statsbiblioteket.summa.common.filter.PayloadQueue} for
 * inspiration as they maintain a byte-size-controlled queue for batching. Streaming should also be examined, but this
 * is vulnerable to errors and has little gain over batching as documents are not visible in the searcher until
 * {@link #commit()} has been called.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SolrManipulator implements IndexManipulator {
    private static Log log = LogFactory.getLog(SolrManipulator.class);

    private boolean orderChanged = false;

    /**
     * The entry point for calls to Solr.
     * </p><p>
     * Optional. Default is localhost:8983 (Solr default).
     */
    public static final String CONF_SOLR_HOST = "solr.host";
    public static final String DEFAULT_SOLR_HOST = "localhost:8983";
    /**
     * The rest call at {@link #CONF_SOLR_HOST}.
     * </p><p>
     * Optional. Default is '/solr' (Solr default).
     */
    public static final String CONF_SOLR_RESTCALL = "solr.restcall";
    public static final String DEFAULT_SOLR_RESTCALL = "/solr";

    public static final String CONF_ID_FIELD = "solr.field.id";
    public static final String DEFAULT_ID_FIELD = "recordID";

    public static final String UPDATE_COMMAND = "/update";

    protected final String host;
    protected final String restCall;
    protected final String FIELD_ID;
    private final String UPDATE;

    private int updatesSinceLastCommit = 0;

    public SolrManipulator(Configuration conf) {
        host = conf.getString(CONF_SOLR_HOST, DEFAULT_SOLR_HOST);
        restCall = conf.getString(CONF_SOLR_RESTCALL, DEFAULT_SOLR_RESTCALL);
        UPDATE = "http://" + host + restCall + UPDATE_COMMAND;
        FIELD_ID = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        log.info("Created SolrManipulator(" + UPDATE + ")");
    }

    @Override
    public synchronized void open(File indexRoot) throws IOException {
        log.info("Open(" + indexRoot + ") is ignored as this IndexManipulator uses external indexing");
    }

    @Override
    public synchronized void clear() throws IOException {
        log.debug("Attempting to delete all documents in the Solr index");
        send("<delete><query>*:*</query></delete>");
        log.info("Deleted all documents in the Solr index");
    }

    @Override
    public boolean update(Payload payload) throws IOException {
        if (payload.getRecord().isDeleted()) {
            orderChanged = true;
            send("<delete><query>" + FIELD_ID + ":" + XMLUtil.encode(payload.getId()) + "</query></delete>");
            log.trace("Removed " + payload.getId() + " from index");
            return false;
        }
        send(payload.getRecord().getContentAsUTF8());
        updatesSinceLastCommit++;
        log.trace("Updated " + payload.getId() + " (" + updatesSinceLastCommit + " updates waiting for commit)");
        return false;
    }

    @Override
    public synchronized void commit() throws IOException {
        orderChanged  = false;
        log.debug("Attempting commit of " + updatesSinceLastCommit + " updates to Solr");
        send("<commit/>");
        log.info("Committed " + updatesSinceLastCommit + " updates to Solr");
        updatesSinceLastCommit = 0;
    }

    @Override
    public synchronized void consolidate() throws IOException {
        log.info("Consolidate called. Currently there is no implementation of this functionality. Note that consolidate"
                 + " is becoming increasingly less important as Lucene/Solr develops");
        commit(); // Just to make sure
    }

    @Override
    public synchronized void close() throws IOException {
        log.info("Closing down SolrManipulator");
        commit();
    }

    @Override
    public void orderChangedSinceLastCommit() throws IOException {
        log.debug("orderChangedSinceLastCommit() called. No effect for this index manipulator");
    }

    @Override
    public boolean isOrderChangedSinceLastCommit() throws IOException {
        return orderChanged;
    }

    private void send(String command) throws IOException {
        URL url = new URL(UPDATE);
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection)url.openConnection();
        } catch (IOException e) {
            throw new IOException("Unable to establish connection to '" + UPDATE + "'", e);
        }
        try {
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/xml");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write("<add>");
            wr.write(command);
            wr.write("</add>");
            wr.flush();
            wr.close();
            int code = conn.getResponseCode();
            if (code != 200) {
                String message = String.format(
                    "Unable to perform update of Solr. Error code %d\nRequest (trimmed):\n%s\nResponse:\n%s",
                    code, trim(command, 100), getResponse(conn));
                throw new IOException(message);
            }
        } finally {
            conn.disconnect();
        }
    }

    private String trim(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String getResponse(HttpURLConnection conn) throws IOException {
        return conn.getResponseCode() == 200 ?
               Strings.flush(new InputStreamReader(conn.getInputStream())) :
               Strings.flush(new InputStreamReader(conn.getErrorStream()));
    }
}
