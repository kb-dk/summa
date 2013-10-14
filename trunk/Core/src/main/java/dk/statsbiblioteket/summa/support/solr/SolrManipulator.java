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
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.PayloadBatcher;
import dk.statsbiblioteket.summa.common.filter.PayloadQueue;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.DeferredSystemExit;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.*;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * While additions and updates are batched, deletes require individual connections. If the frequency of requests
     * get too high, the local machine will run out of ephemeral ports. The ephemeral port range can be inspected
     * under Linux with {@code cat /proc/sys/net/ipv4/ip_local_port_range}. To get an estimate of the maximum requests,
     * find the number og ephemeral ports and divide it with the timeout in seconds for tcp, attainable under Linux by
     * executing {@code cat /proc/sys/net/ipv4/tcp_fin_timeout}. The value is theoretical max so it is advisable to
     * set it markedly lower.
     * </p><p>
     * Optional. Default is 100 (very conservative).
     */
    public static final String CONF_CONNECTION_MAXREQUESTS = "solr.connection.maxrequestspersecond";
    public static final int DEFAULT_CONNECTION_MAXREQUESTS = 100;

    public static final String UPDATE_COMMAND = "/update";

    protected final String hostWithPort;
    protected final String restCall;
    protected final String fieldID;
//    private final String updateCommand;
    private final boolean flushOnDelete;


    private final PayloadBatcher batcher;
    private final int maxRequests;
    private final Profiler requestProfiler;
    private int updatesSinceLastCommit = 0;
    private int updates = 0;
    private int deletesSinceLastCommit = 0;
    private int deletes = 0;
    // Not thread safe, but as we need to process updates sequentially, this is not an issue

    // TODO: Guarantee recordID and recordBase
    public SolrManipulator(Configuration conf) {
        hostWithPort = conf.getString(CONF_SOLR_HOST, DEFAULT_SOLR_HOST);
        restCall = conf.getString(CONF_SOLR_RESTCALL, DEFAULT_SOLR_RESTCALL);
//        updateCommand = "http://" + host + restCall + UPDATE_COMMAND;
        fieldID = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        flushOnDelete = conf.getBoolean(CONF_FLUSH_ON_DELETE, DEFAULT_FLUSH_ON_DELETE);
        maxRequests = conf.getInt(CONF_CONNECTION_MAXREQUESTS, DEFAULT_CONNECTION_MAXREQUESTS);
        requestProfiler = new Profiler(1, maxRequests);

        batcher = new PayloadBatcher(conf) {
            @Override
            protected void flush(PayloadQueue queue) {
                List<Payload> payloads = new ArrayList<Payload>(queue.size());
                queue.drainTo(payloads);
                send(payloads);
            }
        };
        setupHttp();
        log.info("Created SolrManipulator(" + hostWithPort + restCall + UPDATE_COMMAND + ")");
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
        updates++;
        if (payload.getRecord().isDeleted()) {
            deletesSinceLastCommit++;
            deletes++;
        }
/*            orderChanged = true;
            if (flushOnDelete) {
                batcher.flush();
            }
            String command = "<delete><id>" + XMLUtil.encode(payload.getId()) + "</id></delete>";
            send(command, command);
            log.trace("Removed " + payload.getId() + " from index");
            return false;
        } */
        batcher.add(payload);
        log.trace("Updated " + payload.getId() + " (" + updatesSinceLastCommit + " updates waiting for commit)");
        return false;
    }

    private void send(List<Payload> payloads) {
        if (payloads.isEmpty()) {
            log.warn("Potential logic failure: send(...) called with empty list of Payloads");
            return;
        }
        int add = 0;
        int del = 0;
        StringWriter command = new StringWriter(payloads.size() * 1000);
        // https://wiki.apache.org/solr/UpdateXmlMessages#Add_and_delete_in_a_single_batch
        command.append("<update>");
        boolean adding = false;
        for (Payload payload: payloads) {
            Record record = payload.getRecord();
            if (adding) {
                if (record.isDeleted()) {
                    command.append("</add>");
                    adding = false;
                }
            } else {
                if (!record.isDeleted()) {
                    command.append("<add>");
                    adding = true;
                }
            }

            if (record.isDeleted()) {
                command.append("<delete><id>").append(XMLUtil.encode(payload.getId())).append("</id></delete>");
                del++;
            } else {
                command.append(removeHeader(payload.getRecord().getContentAsUTF8()));
                add++;
            }
        }
        if (adding) {
            command.append("</add>");
        }
        command.append("</update>");
        try {
            send(payloads.size() + " Payloads", command.toString());
            if (flushOnDelete) {
                batcher.flush();
            }
        } catch (NoRouteToHostException e) {
            String error = String.format(
                    "NoRouteToHostException sending %d updates (%d adds, %d deletes) to %s. "
                    + "This is likely to be caused by depletion of ports in the ephemeral range. "
                    + "Consider adjusting batch setup from the current %s"
                    + "Payloads: %s. The JVM will be shut down in 5 seconds. First part of command:\n%s",
                    payloads.size(), add, del, this, batcher,
                    Strings.join(payloads, ", "), trim(command.toString(), 1000));
            Logging.logProcess("SolrManipulator", error, Logging.LogLevel.FATAL, "", e);
            Logging.fatal(log, "SolrManipulator.send", error, e);
            new DeferredSystemExit(1, 5000);
        } catch (IOException e) {
            String error = String.format(
                    "IOException sending %d updates (%d adds, %d deletes) to %s. Payloads: %s. "
                    + "The JVM will be shut down in 5 seconds. First part of command:\n%s",
                    payloads.size(), add, del, this, Strings.join(payloads, ", "), trim(command.toString(), 1000));
            Logging.logProcess("SolrManipulator", error, Logging.LogLevel.FATAL, "", e);
            Logging.fatal(log, "SolrManipulator.send", error, e);
            new DeferredSystemExit(1, 5000);
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
        log.info("commit(): Flushing batcher with " + batcher.size() + " pending Payloads");
        batcher.flush();
        orderChanged  = false;
        log.info("Attempting commit of " + updatesSinceLastCommit + " updates to Solr");
        send(null, "<commit/>");
        log.info("Committed " + updatesSinceLastCommit + " updates ("
                 + (updatesSinceLastCommit - deletesSinceLastCommit) + " adds, " + deletesSinceLastCommit
                 + " deletes) to Solr. Total updates: " + updates + " with " + deletes + " deletes");
        updatesSinceLastCommit = 0;
        deletesSinceLastCommit = 0;
    }

    @Override
    public synchronized void consolidate() throws IOException {
        log.info("Consolidate called. Currently there is no implementation of this functionality besides requesting a "
                 + "commit. Note that consolidate is generally now recommended for Solr 4+.");
        commit(); // Just to make sure
    }

    @Override
    public synchronized void close() throws IOException {
        log.info("Closing down " + this);
        batcher.close();
        commit();
        conn.close();
    }

    @Override
    public void orderChangedSinceLastCommit() throws IOException {
        log.debug("orderChangedSinceLastCommit() called. No effect for this index manipulator");
    }

    @Override
    public boolean isOrderChangedSinceLastCommit() throws IOException {
        return orderChanged;
    }

    DefaultHttpClientConnection conn;
    HttpHost host;
    HttpParams params;
    HttpRequestExecutor httpexecutor;
    HttpProcessor httpproc;
    HttpContext context;
    ConnectionReuseStrategy connStrategy;
    private void setupHttp() {
/*        http = new DefaultHttpClient();
        http = new DefaultHttpClient(http.getParams().setBooleanParameter(CoreConnectionPNames.SO_KEEPALIVE, true));

        post = new HttpPost(updateCommand);
        post.addHeader("Content-Type", "application/xml");
        post.addHeader("Accept", "application/xml");
        post.addHeader("Accept-Charset", "utf-8");
  */
       // Taken more or less directly from
       // https://hc.apache.org/httpcomponents-core-ga/httpcore/examples/org/apache/http/examples/ElementalHttpPost.java
        params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "Summa SolrManipulator 20120918");
//        HttpProtocolParams.setUseExpectContinue(params, true);
//        post.addHeader("Content-Type", "application/xml");
//        post.addHeader("Accept", "application/xml");

        httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
            // Required protocol interceptors
            new RequestContent(),
            new RequestTargetHost(),
            // Recommended protocol interceptors
            new RequestConnControl(),
            new RequestUserAgent(),
            new RequestExpectContinue()});

        httpexecutor = new HttpRequestExecutor();

        context = new BasicHttpContext(null);

        String hostName = hostWithPort.split(":")[0];
        Integer port = Integer.valueOf(hostWithPort.split(":")[1]);
        host = new HttpHost(hostName, port);

        conn = new DefaultHttpClientConnection();
        connStrategy = new DefaultConnectionReuseStrategy();

        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);
    }

    private synchronized void send(String designation, String command) throws IOException {
        if (maxRequests > 0 && requestProfiler.getBps() > maxRequests) {
            try {
                Thread.sleep(maxRequests > 500 ? 2 : 1000/maxRequests);
            } catch (InterruptedException e) {
                log.warn(
                    "send(" + designation + ") interrupted while taking a timeout to lower requests rate. Continuing");
            }
        }
        requestProfiler.beat();
        if (!conn.isOpen()) {
            Socket socket = new Socket(host.getHostName(), host.getPort());
            conn.bind(socket, params);
        }
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
            "POST", restCall + UPDATE_COMMAND); // "/servlets-examples/servlet/RequestInfoExample");
        request.setEntity(new StringEntity(command, "utf-8"));
        request.setHeader("Content-Type", "application/xml");
//        request.setHeader("Accept", "application/xml");
//        request.addHeader(new BasicHeader("Content-Type", "application/xml"));
//        request.addHeader(new BasicHeader("Accept", "application/xml"));

        String updateCommand = hostWithPort + restCall + UPDATE_COMMAND; // For feedback
        request.setParams(params);
        HttpResponse response;
        try {
            httpexecutor.preProcess(request, httpproc, context);
        } catch (HttpException e) {
            String message = String.format(
                "HttpException while pre-processing the POST request for '%s' to %s. Trimmed request:\n%s",
                designation, updateCommand, trim(command, 100));
            Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
            throw new IOException(message, e);
        }

        try {
            response = httpexecutor.execute(request, conn, context);
        } catch (HttpException e) {
            String message = String.format(
                "HttpException while executing '%s' at %s. Trimmed request:\n%s",
                designation, updateCommand, trim(command, 100));
            Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
            throw new IOException(message, e);
        }

        try {
            response.setParams(params);
            httpexecutor.postProcess(response, httpproc, context);
            //if (!connStrategy.keepAlive(response, context)) {
                //System.out.println("No keep alive!");
            // If we keep the connection alive, we sometimes get errors back
                conn.close();
            //}
        } catch (HttpException e) {
            String message = String.format(
                "HttpException %s while post-processing '%s' to %s. Trimmed request:\n%s\nResponse:\n%s",
                response.getStatusLine().getStatusCode(), designation, updateCommand, trim(command, 100),
                getResponse(response));
            Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
            throw new IOException(message, e);
        }
        int code = response.getStatusLine().getStatusCode();
        if (code == 400) { // Bad Request: Solr did not like the input
            String message = String.format(
                "Error 400 (Bad Request): Solr did not accept the document '%s' at %s. "
                + "Trimmed request:\n%s\nResponse:\n%s",
                designation, updateCommand, trim(command, 100), getResponse(response));
            Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
            throw new IOException(message);
        } else if (code != 200) {
            String message = String.format(
                "Fatal error, JVM will be shut down: Unable to send '%s' to Solr at %s from %s. Error code %d. "
                + "Trimmed request:\n%s\nResponse:\n%s",
                designation, updateCommand, this, code, trim(command, 100), getResponse(response));
            Logging.logProcess("SolrManipulator", message, Logging.LogLevel.ERROR, designation);
            Logging.fatal(log, "SolrManipulator.send", message);
            new DeferredSystemExit(1);
            throw new IOException(message);
        }


        /*

        post.setEntity(new StringEntity(command, "utf-8"));
        HttpResponse httpResponse;
        try {
            httpResponse = http.execute(post);
            int code = httpResponse.getStatusLine().getStatusCode();
            if (code == 400) { // Bad Request: Solr did not like the input
                String message = String.format(
                    "Error 400 (Bad Request): Solr did not accept the document '%s' at %s. "
                    + "Trimmed request:\n%s\nResponse:\n%s",
                    designation, updateCommand, trim(command, 100), getResponse(httpResponse));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
                throw new IOException(message);
            } else if (code != 200) {
                String message = String.format(
                    "Fatal error, JVM will be shut down: Unable to send '%s' to Solr at %s from %s. Error code %d. "
                    + "Trimmed request:\n%s\nResponse:\n%s",
                    designation, updateCommand, this, code, trim(command, 100), getResponse(httpResponse));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.ERROR, designation);
                log.fatal(message);
                new DeferredSystemExit(1);
                throw new IOException(message);
            }
        } catch (Exception e) {
            String snippet = command.length() > 40 ? command.substring(0, 40) : command;
            String message = String.format(
                "Fatal error, JVM will be shut down: Non-explicitely handled Exception while attempting '%s' with"
                + " '%s' to %s from %s",
                updateCommand, snippet, designation, this);
            Logging.logProcess("SolrManipulator", message, Logging.LogLevel.ERROR, designation, e);
            log.fatal(message, e);
            new DeferredSystemExit(1);
            throw new IOException(message, e);
        } finally {
   //         post.reset();
        } */
    }

    private String trim(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private Pattern solrError = Pattern.compile(".*<lst name=\"error\"><str name=\"msg\">(.*)</str>.*", Pattern.DOTALL);
    private String getResponse(HttpResponse response) throws IOException {
        String raw = Strings.flush(response.getEntity().getContent());
        Matcher matcher = solrError.matcher(raw);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return raw;
    }

    @Override
    public String toString() {
        return "SolrManipulator(" + host + restCall + "). Processed " + updates + " updates including " + deletes
               + " deletes";
    }
}
