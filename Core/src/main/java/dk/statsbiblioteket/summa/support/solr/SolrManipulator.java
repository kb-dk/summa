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
import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.summa.common.util.RecordStatsCollector;
import dk.statsbiblioteket.summa.index.IndexManipulator;
import dk.statsbiblioteket.util.Profiler;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Timing;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
 * that it is not significantly faster: https://wiki.apache.org/solr/FAQ#How_can_indexing_be_accelerated.3F
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
     * If true and an IOException is encountered during Solr-interaction, the SolrManipulator tries to locate
     * which Payloads failed.
     *
     * Optional. Default is true.
     */
    public static final String CONF_ONERROR_LOCATEBADPAYLOADS = "solr.onerror.locatebadpayloads";
    public static final boolean DEFAULT_ONERROR_LOCATEBADPAYLOADS = true;

    /**
     * If CONF_ONERROR_LOCATEBADPAYLOADS is true the number of bad payloads is equal to or less than then
     * given number, processing continues. If these two parameters are not satisfies, processing is aborted.
     *
     * Optional. Default is 0 (processing always aborts).
     */
    public static final String CONF_ONERROR_ACCEPTABLE_FAULTY_PAYLOADS_PER_BATCH =
            "solr.onerror.acceptablefaultypayloadsperbatch";
    public static final int DEFAULT_ONERROR_ACCEPTABLE_FAULTY_PAYLOADS_PER_BATCH = 0;

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

    /**
     * Log overall status in the class log on INFO for every x Payloads received.
     * </p><p>
     * Optional. Default is 0 (disabled).
     */
    public static final String CONF_STATUS_EVERY = "process.status.every";
    public static final int DEFAULT_STATUS_EVERY = 0;

    public static final String UPDATE_COMMAND = "/update";
    private static final long M = 1000000;

    // Used by send to avoid dead-locking by splitting send and the possible post-send batcher.flush
    private final Object sendLock = new Object();

    protected final String hostWithPort;
    protected final String restCall;
    protected final String fieldID;
    //    private final String updateCommand;
    private final boolean flushOnDelete;
    public final boolean locateBadPayloads;
    public final int acceptableBadPayloads;

    private final PayloadBatcher batcher;
    private final int maxRequests;
    private final Profiler requestProfiler;
    private final int statusEvery;
    private int updatesSinceLastCommit = 0;
    private int updates = 0;
    private int deletesSinceLastCommit = 0;
    private int deletes = 0;
    // Not thread safe, but as we need to process updates sequentially, this is not an issue

    private final Timing timingSend = new Timing("send", null, "update");
    private final RecordStatsCollector statsSend;

    // TODO: Guarantee recordID and recordBase
    public SolrManipulator(Configuration conf) {
        hostWithPort = conf.getString(CONF_SOLR_HOST, DEFAULT_SOLR_HOST);
        restCall = conf.getString(CONF_SOLR_RESTCALL, DEFAULT_SOLR_RESTCALL);
//        updateCommand = "http://" + host + restCall + UPDATE_COMMAND;
        fieldID = conf.getString(CONF_ID_FIELD, DEFAULT_ID_FIELD);
        flushOnDelete = conf.getBoolean(CONF_FLUSH_ON_DELETE, DEFAULT_FLUSH_ON_DELETE);
        maxRequests = conf.getInt(CONF_CONNECTION_MAXREQUESTS, DEFAULT_CONNECTION_MAXREQUESTS);
        requestProfiler = new Profiler(1, maxRequests);
        statusEvery = conf.getInt(CONF_STATUS_EVERY, DEFAULT_STATUS_EVERY);
        locateBadPayloads = conf.getBoolean(CONF_ONERROR_LOCATEBADPAYLOADS, DEFAULT_ONERROR_LOCATEBADPAYLOADS);
        acceptableBadPayloads = conf.getInt(CONF_ONERROR_ACCEPTABLE_FAULTY_PAYLOADS_PER_BATCH,
                                            DEFAULT_ONERROR_ACCEPTABLE_FAULTY_PAYLOADS_PER_BATCH);

        batcher = new PayloadBatcher(conf) {
            @Override
            protected void flush(PayloadQueue queue) {
                List<Payload> payloads = new ArrayList<>(queue.size());
                queue.drainTo(payloads);
                send(payloads);
            }
        };
        setupHttp();
        statsSend = new RecordStatsCollector("out", conf, null, false, "solrdocs");
        log.info("Created " + this);
    }


    @Override
    public synchronized void open(File indexRoot) {
        log.info("Open(" + indexRoot + ") is ignored as this IndexManipulator uses external indexing");
    }

    @Override
    public synchronized void clear() throws IOException {
        log.debug("Attempting to delete all documents in the Solr index");
        send(null, "<delete><query>*:*</query></delete>", false);
        commit();
        log.info("Delete all documents in the Solr index request sent");
    }

    @Override
    public boolean update(Payload payload) {
        try {
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
        } finally {
            if (statusEvery > 0 && updatesSinceLastCommit % statusEvery == 0) {
                log.info("Received update #" + updatesSinceLastCommit + ". Stats: " + getProcessStats());
            }
        }
    }

    // Note: No synchronization in this part. The actual network-delivery is handled by the other send-method
    private void send(List<Payload> payloads) {
        if (payloads.isEmpty()) {
            log.warn("Potential logic failure: send(...) called with empty list of Payloads");
            return;
        }
        final long startNS = System.nanoTime();

        Pair<Integer, Integer> addAndDeletes = new Pair<>(0, 0);
        String commandString = "N/A";
        try {
            // https://wiki.apache.org/solr/UpdateXmlMessages#Add_and_delete_in_a_single_batch
            commandString = createSolrUpdateBatch(payloads, addAndDeletes);

            // FIXME: Why is timingSend.getUpdates()==0 ? It is updated in the finally block!?
            statsSend.process("delivery#" + timingSend.getUpdates(), commandString.length());
            send(payloads.size() + " Payloads", commandString, false);

            if (flushOnDelete && addAndDeletes.getValue() > 0) {
                batcher.flush();
            }
        } catch (NoRouteToHostException e) {
            shutdown(String.format(Locale.ROOT,
                    "NoRouteToHostException sending %d updates (%d adds, %d deletes) to %s. "
                    + "This is likely to be caused by depletion of ports in the ephemeral range. "
                    + "Consider adjusting batch setup from the current %s"
                    + "Payloads: %s. The JVM will be shut down in 5 seconds. First part of command:\n%s",
                                   payloads.size(), addAndDeletes.getKey(), addAndDeletes.getValue(), this, batcher,
                                   Strings.join(payloads, ", "), trim(commandString, 1000)), e);
        } catch (IOException e) {
            if (!locateBadPayloads) {
                shutdown(String.format(Locale.ROOT,
                        "IOException sending %d updates (%d adds, %d deletes) to %s. Payloads: %s. "
                        + "The JVM will be shut down in 5 seconds. First part of command:\n%s",
                        payloads.size(), addAndDeletes.getKey(), addAndDeletes.getValue(), this,
                        Strings.join(payloads, ", "), trim(commandString, 1000)), e);
            } else {
                final List<Payload> bad = probeBadPayloads(payloads);
                if (bad.isEmpty()) {
                    log.warn("IOException received during batch update, but sending all " + payloads.size() +
                             " Payloads one at a time did not raise any Exceptions. Cautiously continuing processing");
                } else if (bad.size() <= acceptableBadPayloads) {
                    log.warn(String.format(Locale.ROOT,
                            "IOException during batch update to Solr. Re-sending all %d Payloads in the batch " +
                            "resulted in %d Payloads that could not be delivered to Solr. This is <= the limit " +
                            "of %d bad Payloads/batch, so processing continues. The bad Payloads and the Exceptions " +
                            "should be visible in the process log for failed Payloads. They are %s",
                            payloads.size(), bad.size(), acceptableBadPayloads, Strings.join(bad, ", ")));
                } else {
                    shutdown(String.format(Locale.ROOT,
                            "IOException sending %d updates (%d adds, %d deletes) to %s. Isolated to offending %d " +
                            "payloads by re-sending one at a time: %s. The JVM will be shut down in 5 seconds",
                            payloads.size(), addAndDeletes.getKey(), addAndDeletes.getValue(), this,
                            bad.size(), Strings.join(bad, ", ")), e);
                }
            }
        } finally {
            timingSend.addNS(System.nanoTime()-startNS);
        }
    }

    private List<Payload> probeBadPayloads(List<Payload> payloads) {
        log.info("Encountered batch which caused Solr to fail. Attempting delivery of problem-Payloads one at a time");
        Pair<Integer, Integer> addAndDeletes = new Pair<>(0, 0);

        List<Payload> bad = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>(); // For later logging
        for (int i = 0; i < payloads.size(); i++) {
            Payload payload = payloads.get(i);
            String message = "Re-sending potentially bad payload " + (i+1) + "/" + payloads.size();
            Logging.logProcess("SolrManipulator", message, Logging.LogLevel.INFO, payload);
            log.info(message + ": " + payload);
            try {
                String commandString = createSolrUpdateBatch(Collections.singletonList(payload), addAndDeletes);
                send("Re-send of single payload " + payload.getId(), commandString, false);
            } catch (Exception e) {
                log.warn("Failed re-sending of bad payload " + i + "/" + payloads.size() + ": " + payload, e);
                exceptions.add(e);
                bad.add(payload);
            }
        }
        if (bad.size() <= acceptableBadPayloads) { // Processing will continue and these are just a few bad apples
            log.info("Probing of " + payloads.size() + " Payloads led to " + bad.size() + " bad ones. " +
                     "Processing is likely to continue");
            for (int i = 0; i < bad.size(); i++) {
                Logging.logProcess("SolrManipulator", "Unrecoverable Solr error when delivering payload",
                                   Logging.LogLevel.FATAL, bad.get(i), exceptions.get(i));
            }
        } else {
            log.info("Probing of " + payloads.size() + " Payloads led to " + bad.size() + " bad ones. " +
                     "Processing is likely to be terminated");
        }
        return bad;
    }

    // Creates XML for updating Solr and updates add/delete statistics
    private String createSolrUpdateBatch(List<Payload> payloads, Pair<Integer, Integer> addAndDeletes) {
        StringWriter solrxml = new StringWriter(payloads.size() * 1000);
        solrxml.append("<update>");
        boolean adding = false;
        for (Payload payload : payloads) {
            Record record = payload.getRecord();
            if (adding) {
                if (record.isDeleted()) {
                    solrxml.append("</add>");
                    adding = false;
                }
            } else {
                if (!record.isDeleted()) {
                    solrxml.append("<add>");
                    adding = true;
                }
            }

            if (record.isDeleted()) {
                solrxml.append("<delete><id>").append(XMLUtil.encode(payload.getId())).append("</id></delete>");
                addAndDeletes.setValue(addAndDeletes.getValue()+1);
            } else {
                solrxml.append(removeHeader(payload.getRecord().getContentAsUTF8()));
                addAndDeletes.setKey(addAndDeletes.getKey()+1);
            }
        }
        if (adding) {
            solrxml.append("</add>");
        }
        solrxml.append("</update>");
        return solrxml.toString();
    }

    private void shutdown(String error, IOException e) {
        Logging.logProcess("SolrManipulator", error, Logging.LogLevel.FATAL, "", e);
        Logging.fatal(log, "SolrManipulator", error, e);
        new DeferredSystemExit(1, 5000);
        fullStop = true;
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
        orderChanged = false;
        log.info("Attempting commit of " + updatesSinceLastCommit + " updates to Solr");
        send(null, "<commit/>", true);
        log.info("Committed " + updatesSinceLastCommit + " updates ("
                 + (updatesSinceLastCommit - deletesSinceLastCommit) + " adds, " + deletesSinceLastCommit
                 + " deletes) to Solr. Total updates: " + updates + " with " + deletes + " deletes");
        updatesSinceLastCommit = 0;
        deletesSinceLastCommit = 0;
    }

    @Override
    public synchronized void consolidate() throws IOException {
        log.info("Consolidate called. Currently there is no implementation of this functionality besides requesting a "
                 + "commit. Note that consolidate is generally not recommended for Solr 4+.");
        commit(); // Just to make sure
    }

    @Override
    public synchronized void close() throws IOException {
        log.debug("Closing down " + this);
        batcher.close();
        commit();
        conn.close();
        log.info("Closed " + this);
    }

    @Override
    public void orderChangedSinceLastCommit() {
        log.debug("orderChangedSinceLastCommit() called. No effect for this index manipulator");
    }

    @Override
    public boolean isOrderChangedSinceLastCommit() {
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

        httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[]{
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

    private boolean fullStop = false;

    private void send(String designation, String command, boolean shutdownOnErrors) throws IOException {
        if (fullStop) {
            log.info("Full stop signalled earlier. Blocking and waiting for JVM shutdown");
            try {
                Thread.sleep(24 * 60 * 60 * 1000);
            } catch (InterruptedException e) {
                log.warn("Full stop interrupted. Continuing");
            }
        }

        if (maxRequests > 0 && requestProfiler.getBps() > maxRequests) {
            try {
                Thread.sleep(maxRequests > 500 ? 2 : 1000 / maxRequests);
            } catch (InterruptedException e) {
                log.warn(
                        "send(" + designation + ") interrupted while taking a timeout to lower requests rate. Continuing");
            }
        }

        long tStart;
        long tBind;
        long tPre;
        long tSend;
        String updateCommand; // For feedback
        HttpResponse response;
        synchronized (sendLock) {
            requestProfiler.beat();
            tStart = System.nanoTime();
            if (!conn.isOpen()) {
                Socket socket = new Socket(host.getHostName(), host.getPort());
                conn.bind(socket, params);
            }
            tBind = System.nanoTime() - tStart;

            BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(
                    "POST", restCall + UPDATE_COMMAND); // "/servlets-examples/servlet/RequestInfoExample");
            request.setEntity(new StringEntity(command, "utf-8"));
            request.setHeader("Content-Type", "application/xml");
//        request.setHeader("Accept", "application/xml");
//        request.addHeader(new BasicHeader("Content-Type", "application/xml"));
//        request.addHeader(new BasicHeader("Accept", "application/xml"));
            updateCommand = hostWithPort + restCall + UPDATE_COMMAND;
            request.setParams(params);
            try {
                httpexecutor.preProcess(request, httpproc, context);
            } catch (HttpException e) {
                String message = String.format(Locale.ROOT,
                        "HttpException while pre-processing the POST request for '%s' to %s. Trimmed request:\n%s",
                        designation, updateCommand, trim(command, 100));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
                throw new IOException(message, e);
            }
            tPre = System.nanoTime() - tStart - tBind;

            try {
                response = httpexecutor.execute(request, conn, context);
            } catch (HttpException e) {
                String message = String.format(Locale.ROOT,
                        "HttpException while executing '%s' at %s. Trimmed request:\n%s",
                        designation, updateCommand, trim(command, 100));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
                throw new IOException(message, e);
            }
            tSend = System.nanoTime() - tStart - tBind - tPre;

            try {
                response.setParams(params);
                httpexecutor.postProcess(response, httpproc, context);
                //if (!connStrategy.keepAlive(response, context)) {
                //System.out.println("No keep alive!");
                // If we keep the connection alive, we sometimes get errors back
                conn.close();
                //}
            } catch (HttpException e) {
                String message = String.format(Locale.ROOT,
                        "HttpException %s while post-processing '%s' to %s. Trimmed request:\n%s\nResponse:\n%s",
                        response.getStatusLine().getStatusCode(), designation, updateCommand, trim(command, 100),
                        getResponse(response));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
                throw new IOException(message, e);
            }
        }

        final long tPost = System.nanoTime() - tStart - tBind - tPre - tSend;

        final String logMessage = String.format(Locale.ROOT,
                "send(command.length=%d) finished in %dms (bind=%d, pre=%d, send=%d, post=%d), total updates: %d, %s",
                command.length(), (tBind + tPre + tSend + tPost) / M, tBind / M, tPre / M, tSend / M, tPost / M, updates,
                getProcessStats());
        if (statusEvery > 0 && updatesSinceLastCommit % statusEvery == 0) {
            log.info(logMessage);
        } else if (log.isDebugEnabled()) {
            log.debug(logMessage);
        }

        int code = response.getStatusLine().getStatusCode();
        if (code != 200) { // Bad Request: Solr did not like the input
            if (shutdownOnErrors) {
                String message = String.format(Locale.ROOT,
                        "Fatal error, JVM will be shut down: Unable to send '%s' to Solr at %s from %s. Error code %d. "
                        + "Trimmed request:\n%s\nResponse:\n%s",
                        designation, updateCommand, this, code, trim(command, 200), getResponse(response));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.FATAL, designation);
                Logging.fatal(log, "SolrManipulator.send", message);
                fullStop = true;
                new DeferredSystemExit(50);
                throw new IOException(message);
            } else {
                // TODO Throw a custom Exception instead
                throw new IOException(String.format(Locale.ROOT,
                        "Unable to send '%s' to Solr at %s from %s. Error code %d. Trimmed request:\n%s\nResponse:\n%s",
                        designation, updateCommand, this, code, trim(command, 200), getResponse(response)));
            }
        }


        /*

        post.setEntity(new StringEntity(command, "utf-8"));
        HttpResponse httpResponse;
        try {
            httpResponse = http.execute(post);
            int code = httpResponse.getStatusLine().getStatusCode();
            if (code == 400) { // Bad Request: Solr did not like the input
                String message = String.format(Locale.ROOT,
                    "Error 400 (Bad Request): Solr did not accept the document '%s' at %s. "
                    + "Trimmed request:\n%s\nResponse:\n%s",
                    designation, updateCommand, trim(command, 100), getResponse(httpResponse));
                Logging.logProcess("SolrManipulator", message, Logging.LogLevel.WARN, designation);
                throw new IOException(message);
            } else if (code != 200) {
                String message = String.format(Locale.ROOT,
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
            String message = String.format(Locale.ROOT,
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

    public String getProcessStats() {
        //noinspection DuplicateStringLiteralInspection
        return "timing(" + timingSend.toString(false, false) + "), size(" + statsSend + ")";
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "SolrManipulator(%s, flushOnDelete=%b, locateBadPayloads=%b, acceptableBadPayloads=%d). " +
                "Processed %d updates including %d deletes. Stats: %s",
                hostWithPort + restCall + UPDATE_COMMAND, flushOnDelete, locateBadPayloads, acceptableBadPayloads,
                updates, deletes, getProcessStats());
    }
}
