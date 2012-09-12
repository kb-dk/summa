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
import dk.statsbiblioteket.summa.common.filter.PayloadBatcher;
import dk.statsbiblioteket.summa.common.filter.PayloadQueue;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Receives Records containing Solr Documents intended for indexing in Solr and delivers them to an external Solr using
 * HTTP.
 * </p><p>
 * The manipulator uses {@link PayloadBatcher} for queueing Payloads. This is necessary as a high amount of separate
 * commits depletes the system of available ports, due to ports not being freed immediately upon disconnect. See the
 * JavaDoc for PayloadBatcher for queueing options.
 * </p><p>
 * Streaming could be examined, but transmission errors would leave the overall state as unknown with regard to
 * the amount of documents passed to Solr. Furthermore this is vulnerable to errors and has little gain over batching
 * as documents are not visible in the searcher until {@link #commit()} has been called. Besides, the Solr FAQ states
 * hat it is not significantly faster: https://wiki.apache.org/solr/FAQ#How_can_indexing_be_accelerated.3F
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

    /**
     * If true, a flush of queued updates is called when a delete is encountered. In a setup where the order of updates
     * is irrelevant (standard setup at Statsbiblioteket, where updates are performed in limited batches where no
     * duplicate record ID's are encountered), this should be true.
     * </p><p>
     * If there are a lot of updates intermixed with deletes, setting this to false diminishes the effect of the queue
     * and the system might run out of available ports.
     * </p><p>
     * Optional. Default is false.
     */
    // TODO: Add sleep if flushOnDelete is true to avoid running out of ports.
    public static final String CONF_FLUSH_ON_DELETE = "solr.flush_on_delete";
    public static final boolean DEFAULT_FLUSH_ON_DELETE = false;

    public static final String UPDATE_COMMAND = "/update";

    protected final String host;
    protected final String restCall;
    protected final String FIELD_ID;
    private final String UPDATE;
    private final boolean flushOnDelete;

    private final PayloadBatcher batcher;
    private int updatesSinceLastCommit = 0;
    // Not thread safe, but as we need to process updates sequentially, this is not an issue
    private HttpClient http = new DefaultHttpClient();

    // TODO: Guarantee recordID and recordBase
    public SolrManipulator(Configuration conf) {
        host = conf.getString(CONF_SOLR_HOST, DEFAULT_SOLR_HOST);
        restCall = conf.getString(CONF_SOLR_RESTCALL, DEFAULT_SOLR_RESTCALL);
        UPDATE = "http://" + host + restCall + UPDATE_COMMAND;
        FIELD_ID = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        flushOnDelete = conf.getBoolean(CONF_FLUSH_ON_DELETE, DEFAULT_FLUSH_ON_DELETE);
        batcher = new PayloadBatcher(conf) {
            @Override
            protected void flush(PayloadQueue queue) {
                List<Payload> payloads = new ArrayList<Payload>(queue.size());
                queue.drainTo(payloads);
                send(payloads);
            }
        };
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
        updatesSinceLastCommit++;
        if (payload.getRecord().isDeleted()) {
            orderChanged = true;
            if (flushOnDelete) {
                batcher.flush();
            }
            String command = "<delete><id>" + XMLUtil.encode(payload.getId()) + "</id></delete>";
            send(command, command);
            log.trace("Removed " + payload.getId() + " from index");
            return false;
        }
        batcher.add(payload);
        log.trace("Updated " + payload.getId() + " (" + updatesSinceLastCommit + " updates waiting for commit)");
        return false;
    }

    private void send(List<Payload> payloads) {
        StringWriter command = new StringWriter(payloads.size() * 1000);
        command.append("<add>");
        for (Payload payload: payloads) {
            command.append(removeHeader(payload.getRecord().getContentAsUTF8()));
        }
        command.append("</add>");
        try {
            send(payloads.size() + " Payloads", command.toString());
        } catch (IOException e) {
            String error = "IOException sending " + payloads.size() + " additions to " + this
                           + ". Payloads: " + Strings.join(payloads, ", ") + ". First part of command:\n"
                           + trim(command.toString(), 1000);
            Logging.logProcess("SolrManipulator", error, Logging.LogLevel.WARN, "", e);
            log.warn(error, e);
        }
    }

    private int ok = 0;
    private int fail = 0;
    private String removeHeader(String solrDocument) {
        if (solrDocument.contains("<")) {
            ok++;
        } else {
            fail++;
        }
        if (log.isTraceEnabled()) {
            log.trace("removeHeader(ok=" + ok + ", fail=" + fail + "): "
                      + (solrDocument.length() < 20 ? solrDocument : solrDocument.substring(0, 20).replace("\n", "")));
        }
        final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
        return solrDocument.startsWith(XML_HEADER) ?
               solrDocument.substring(XML_HEADER.length(), solrDocument.length()) :
               solrDocument;
    }

    @Override
    public synchronized void commit() throws IOException {
        batcher.flush();
        orderChanged  = false;
        log.debug("Attempting commit of " + updatesSinceLastCommit + " updates to Solr");
        send(null, "<commit/>");
        log.info("Committed " + updatesSinceLastCommit + " updates to Solr");
        updatesSinceLastCommit = 0;
    }

    @Override
    public synchronized void consolidate() throws IOException {
        log.info("Consolidate called. Currently there is no implementation of this functionality besides requesting a "
                 + "commit. Note that consolidate is becoming increasingly less important as Lucene/Solr develops");
        commit(); // Just to make sure
    }

    @Override
    public synchronized void close() throws IOException {
        log.info("Closing down SolrManipulator");
        batcher.close();
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

    private synchronized void send(String designation, String command) throws IOException {
        HttpPost post = new HttpPost(UPDATE);
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.addHeader("Accept-Charset", "utf-8");
        post.setEntity(new StringEntity(command, "utf-8"));
        HttpResponse httpResponse;
        try {
            httpResponse = http.execute(post);
            int code = httpResponse.getStatusLine().getStatusCode();
            if (code == 400) { // Bad Request: Solr did not like the input
                String message = String.format(
                    "Error 400 (Bad Request): Solr did not accept the document '%s' at %s. "
                    + "Trimmed request:\n%s\nResponse:\n%s",
                    designation, UPDATE, trim(command, 100), getResponse(httpResponse));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
                throw new IOException(message);
            } else if (code != 200) {
                String message = String.format(
                    "Fatal error, JVM will be shut down: Unable to send '%s' to Solr at %s. Error code %d. "
                    + "Trimmed request:\n%s\nResponse:\n%s",
                    designation, UPDATE, code, trim(command, 100), getResponse(httpResponse));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.ERROR, designation);
                log.fatal(message);
                new DeferredSystemExit(1);
                throw new IOException(message);
            }
        } catch (Exception e) {
            String snippet = command.length() > 40 ? command.substring(0, 40) : command;
            String message = String.format(
                "Fatal error, JVM will be shut down: Non-explicitely handled Exception while attempting '%s' with"
                + " '%s' to %s",
                UPDATE, snippet, designation);
            Logging.logProcess("SolrManipulator", message, Logging.LogLevel.ERROR, designation, e);
            log.fatal(message, e);
            new DeferredSystemExit(1);
            throw new IOException(message, e);
        } finally {
            post.reset();
        }
    }

    private String trim(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String getResponse(HttpResponse response) throws IOException {
        return Strings.flush(response.getEntity().getContent());
    }

    @Override
    public String toString() {
        return "SolrManipulator(" + host + restCall + ")";
    }
}
