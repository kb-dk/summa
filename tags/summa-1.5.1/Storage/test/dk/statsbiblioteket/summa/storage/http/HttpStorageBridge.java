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
package dk.statsbiblioteket.summa.storage.http;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.util.RecordUtil;

import dk.statsbiblioteket.summa.storage.api.*;

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class acts as a bridge between a {@link Storage}
 * instance and a web front end such as a servlet or simple
 * http server.
 *
 * @author mke
 * @since Sep 10, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class HttpStorageBridge implements Configurable {
    private static final Log log = LogFactory.getLog(HttpStorageBridge.class);


    public static final String CONF_STORAGE = "summa.storage.http.backend";

    public static final String CONF_GET_METHODS =
            "summa.storage.http.getmethods";
    public static final String DEFAULT_GET_METHODS = "";

    public static final String CONF_POST_METHODS =
            "summa.storage.http.postmethods";
    public static final String DEFAULT_POST_METHODS = "";

    private Storage storage;
    private Set<Method> getMethods;
    private Set<Method> postMethods;
    private DateFormat dateFormat;

    public enum Method {
        ERROR,     // 500 Internal Error
        FORBIDDEN, // 403 Forbidden
        BAD,       // 400 Bad request
        RECORD,  // 200 or 404
        MTIME,   // 200 OK
        CHANGES, // 200 OK
        CLOSE,   // 204 No Content or 200 OK?
        CLEAR    // 204 No Content or 200 OK?
    }

    public enum HttpMethod {
        GET,
        POST
    }

    public HttpStorageBridge(Configuration conf) throws IOException {
        storage = StorageFactory.createStorage(conf, CONF_STORAGE);

        getMethods = new HashSet<Method>();
        postMethods = new HashSet<Method>();
        for (String method : conf.getStrings(
                CONF_GET_METHODS, new String[0])) {
            getMethods.add(Method.valueOf(method.toUpperCase()));
        }
        for (String method : conf.getStrings(
                CONF_POST_METHODS, new String[0])) {
            postMethods.add(Method.valueOf(method.toUpperCase()));
        }

        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");

        log.info("Published GET methods: " + Strings.join(getMethods, ", "));
        log.info("Published POST methods: " + Strings.join(postMethods, ", "));
    }

    /**
     * Process a HTTP GET request, printing the returned stream to {@code out}
     * and passing the HTTP return code in the return value.
     *
     * @param out a {@code Writer} to write the feedback to
     * @param path the full tokenized path from the requested URL
     * @param query tokenized query part of the requested URL
     */
    public int doGet(OutputStream out, String[] path, QueryTokenizer query) {
        Method method = parseMethod(path, HttpMethod.GET);
        log.info("GET:\n\tMethod:" + method + "\n\tPath:"+ Strings.join(path, "|") + "\n\tQuery:" + query.toString());
        switch (method) {
            case FORBIDDEN:
                return dispatchForbidden(out, path, query);
            case BAD:
                return dispatchBad(out, path, query);
            case MTIME:
                return dispatchMtime(out, path, query);
            case RECORD:
                return dispatchGetRecord(out, path, query);
            case CHANGES:
                return 500;//dispatchChanges(out, path, query);
            case CLEAR:
            case CLOSE:
                return 500;//dispatchPleasePost(out, path, query); // 405 Method Not Allowed
            default:
                return dispatchError(out, path, query, Method.ERROR,
                                     new RuntimeException("Unexpected method: "
                                                          + method));
        }
    }

    public int doPost(InputStream in, OutputStream out,
                      String[] path, QueryTokenizer query) {
        Method method = parseMethod(path, HttpMethod.POST);

        switch (method) {
            case FORBIDDEN:
                return dispatchForbidden(out, path, query);
            case BAD:
                return dispatchBad(out, path, query);
            case CLEAR:
                return 500; //dispatchClear(out, path, query);
            case CLOSE:
                return 500; //dispatchClose(out, path, query);
            case RECORD:
                return dispatchPostRecord(in, out, path, query);
            case MTIME:
            case CHANGES:
                return 500; //dispatchPleaseGet(out, path, query); // 405 Method Not Allowed
            default:
                return dispatchError(out, path, query, Method.ERROR,
                                     new RuntimeException("Unexpected method: "
                                                          + method));
        }
    }

    public Method parseMethod(String[] path, HttpMethod httpMethod) {
        if (path.length >= 1) {
            return parseMethod(path[0], httpMethod);
        } else {
            log.debug("No method defined");
            return Method.BAD;
        }
    }

    public Method parseMethod(String root, HttpMethod httpMethod) {
        try {
            Method method = Method.valueOf(root.toUpperCase());
            Set allowedMethods;
            switch (httpMethod) {
                case GET:
                    allowedMethods = getMethods;
                    break;
                case POST:
                    allowedMethods = postMethods;
                    break;
                default:
                    log.error("Unexpected http method '" + httpMethod + "'");
                    return Method.ERROR;
            }

            if (!allowedMethods.contains(method)) {
                log.info("Attempt to access forbidden method '" + method
                         + "' via HTTP " + httpMethod);
                return Method.FORBIDDEN;
            } else {
                return method;
            }

        } catch (IllegalArgumentException e) {
            // No such enum value
            log.debug("No such method '" + root + "'");
            return Method.BAD;
        }
    }

    public QueryOptions parseQuery(QueryTokenizer query) {
        log.debug("FIXME: Parse query options");
        return new QueryOptions();
    }

    private PrintStream prepareResponse(
            OutputStream out, String[] path, QueryTokenizer query,
            Method type, long responseTime, String message) {

        PrintStream w = new PrintStream(out, true);
        w.println("<reponse type=\""
                  + type + "\" time=\"" + responseTime/1000000D + "\">");

        // Print URL path
        w.println("  <path>" + Strings.join(path, "/")+ "</path>");

        // Print URL query args
        while (query.hasNext()) {
            QueryToken tok = query.next();
            w.println(String.format(
                  "  <arg name=\"%s\" value=\"%s\"/>",
                  tok.getKey(), tok.getValue()));
        }
        query.reset();

        // Print message
        if (message != null) {
            w.println("  <message>");
            w.println("    " + message);
            w.println("  </message>");
        }

        return w;
    }

    private int closeReponse(PrintStream w, int returnValue) {
        w.println("</response>");
        w.flush();
        return returnValue;
    }

    // Return 403 Forbidden
    private int dispatchForbidden(
                        OutputStream out, String[] path, QueryTokenizer query) {
        PrintStream w = prepareResponse(
                out, path, query, Method.FORBIDDEN, 0,
                "Forbidden Request - The requested method is not public");
        return closeReponse(w, 403);
    }

    // Return 400 Bad Request
    private int dispatchBad(
                        OutputStream out, String[] path, QueryTokenizer query) {
        return dispatchBad(out, path, query,
                           "The server was unable to parse the request");
    }


    // Return 400 Bad Request
    private int dispatchBad(OutputStream out, String[] path,
                            QueryTokenizer query, String message) {
        PrintStream w = prepareResponse(
                out, path, query, Method.BAD, 0, "Bad Request - " + message);
        return closeReponse(w, 400);
    }

    // Return 500 Internal Error
    private int dispatchError(OutputStream out, String[] path,
                              QueryTokenizer query, Method method, Throwable t) {
        // Write a log statement
        ByteArrayOutputStream toLog = new ByteArrayOutputStream();
        PrintStream w = prepareResponse(
                toLog, path, query, method, 0,
                "Internal Error - Please consult the server logs for details");
        closeReponse(w, 500);
        log.error("Internal error:\n"
                  + new String(toLog.toByteArray()) + "\n"
                  + "Error was: " + t.getMessage(), t);

        // Prepare response to client
        w = prepareResponse(
                out, path, query, method, 0,
                "Internal Error - Please consult the server logs for details");
        return closeReponse(w, 500);
    }

    private int dispatchMtime(
                        OutputStream out, String[] path, QueryTokenizer query) {
        try {
            long responseTime = System.nanoTime();
            String base = null;
            if (path.length >= 2) {
                base = path[1];
            }
            long mtime = storage.getModificationTime(base);
            responseTime = System.nanoTime() - responseTime;

            PrintStream w = prepareResponse(
                    out, path, query, Method.MTIME, responseTime, null);
            w.println("  <mtime base=\"" + (base != null ? base : "*") + "\">");
            w.println("    <epoch>" + mtime + "</epoch>");
            w.println("    <iso>" +dateFormat.format(new Date(mtime))+"</iso>");
            w.println("  </mtime>");
            return closeReponse(w, 200);
        } catch (Exception e) {
            return dispatchError(out, path, query, Method.MTIME, e);
        }
    }

    private int dispatchGetRecord(
                        OutputStream out, String[] path, QueryTokenizer query) {
        try {
            long responseTime = System.nanoTime();
            if (path.length < 2) {
                return dispatchBad(out, path, query,
                                   "No record id specified in URL");
            }
            String id = path[1];
            QueryOptions opts = parseQuery(query);
            Record rec = storage.getRecord(id, opts);
            responseTime = System.nanoTime() - responseTime;

            PrintStream w = prepareResponse(
                    out, path, query, Method.RECORD, responseTime,
                    rec != null ? null : "No such record");
            System.out.println("11111111111111111111");
            if (rec != null) {
                w.println(RecordUtil.toXML(rec));
                return closeReponse(w, 200);
            } else {
                return closeReponse(w, 404);
            }
        } catch (Exception e) {
            return dispatchError(out, path, query, Method.MTIME, e);
        }
    }

    // Return 201 Created on success
    private int dispatchPostRecord(InputStream in, OutputStream out,
                                   String[] path, QueryTokenizer query) {
        try {
            long responseTime = System.nanoTime();
            if (path.length < 2) {
                return dispatchBad(out, path, query,
                                   "No record id specified in URL");
            }
            String id = path[1];
            QueryOptions opts = parseQuery(query);
            Record rec = RecordUtil.fromXML(new InputStreamReader(in));

            if (!id.equals(rec.getId())) {
                return dispatchBad(out, path, query,
                                   "Record id in URL does not match the id" +
                                   " in the posted body");
            }

            storage.flush(rec);
            responseTime = System.nanoTime() - responseTime;
            PrintStream w = prepareResponse(
                    out, path, query, Method.RECORD, responseTime,
                    "Record '" + id + "' updated");
            return closeReponse(w, 201);
        } catch (Exception e) {
            return dispatchError(out, path, query, Method.RECORD, e);
        }
    }
}

