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

import dk.statsbiblioteket.summa.common.Logging;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Receives Records containing Solr Documents intended for indexing in Solr and delivers them to an external Solr using
 * HTTP.
 * </p><p>
 * The current version flushes received documents one at a time as soon as they are received. This is not very efficient
 * but has the upside of being simple and solid.
 * For later versions a batching model should be considered.
 * See {@link dk.statsbiblioteket.summa.storage.api.filter.RecordWriter}
 * and {@link dk.statsbiblioteket.summa.common.filter.PayloadQueue} for
 * inspiration as they maintain a byte-size-controlled queue for batching.
 *
 * Streaming could also be examined, but transmission errors would leave the overall state as unknown with regard to
 * the amount of documents passed to Solr. Speed-wise  this is vulnerable to errors and has little gain over batching as documents
 * are not visible in the searcher until {@link #commit()} has been called. Besides, the Solr FAQ states that it is
 * not significantly faster: https://wiki.apache.org/solr/FAQ#How_can_indexing_be_accelerated.3F
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: How to handle new index?
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
    public static final String DEFAULT_ID_FIELD = IndexUtils.RECORD_FIELD;

    public static final String UPDATE_COMMAND = "/update";

    protected final String host;
    protected final String restCall;
    protected final String FIELD_ID;
    private final String UPDATE;

    private int updatesSinceLastCommit = 0;

    // TODO: Guarantee recordID and recordBase
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
        send(null, "<delete><query>*:*</query></delete>");
        commit();
        log.info("Delete all documents in the Solr index request sent");
    }

    @Override
    public boolean update(Payload payload) throws IOException {
        if (payload.getRecord().isDeleted()) {
            orderChanged = true;
            send(payload, "<delete><id>" + XMLUtil.encode(payload.getId()) + "</id></delete>");
            log.trace("Removed " + payload.getId() + " from index");
            updatesSinceLastCommit++;
            return false;
        }
        send(payload, packAddition(payload.getRecord().getContentAsUTF8()));
        updatesSinceLastCommit++;
        log.trace("Updated " + payload.getId() + " (" + updatesSinceLastCommit + " updates waiting for commit)");
        return false;
    }

    private int ok = 0;
    private int fail = 0;
    private String packAddition(String solrDocument) {
        if (solrDocument.contains("<")) {
            ok++;
        } else {
            fail++;
        }
        log.info("packAddition(ok=" + ok + ", fail=" + fail + "): "
                 + (solrDocument.length() < 20 ? solrDocument : solrDocument.substring(0, 20).replace("\n", "")));
        final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
        return "<add>" + (
            solrDocument.startsWith(XML_HEADER) ?
            solrDocument.substring(XML_HEADER.length(), solrDocument.length()) :
            solrDocument)
               + "</add>";
    }

    @Override
    public synchronized void commit() throws IOException {
        orderChanged  = false;
        log.debug("Attempting commit of " + updatesSinceLastCommit + " updates to Solr");
        send(null, "<commit/>");
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

    private void send(Payload payload, String command) throws IOException {
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
            conn.setRequestProperty("Accept", "application/xml");
            conn.setRequestProperty("Accept-Charset", "utf-8");
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(command);
            wr.flush();
            wr.close();
            int code = conn.getResponseCode();
            if (code != 200) {
                String message = String.format(
                    "Unable to index document '%s' into Solr at %s. Error code %d. Trimmed request:\n%s\nResponse:\n%s",
                    payload == null ? "null Payload" : payload.getId(), UPDATE, code, trim(command, 100),
                    getResponse(conn));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, payload);
                throw new IOException(message);
            }
        } catch (ConnectException e) {
            String snippet = command.length() > 40 ? command.substring(0, 40) : command;
            throw (IOException)new IOException(
                "ConnectException for '" + UPDATE + "' with '" + snippet + "'").initCause(e);
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
