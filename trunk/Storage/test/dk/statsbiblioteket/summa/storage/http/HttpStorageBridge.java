package dk.statsbiblioteket.summa.storage.http;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.util.RecordUtil;

import dk.statsbiblioteket.summa.storage.api.*;

import java.io.PrintWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.StringWriter;
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

    public static final String CONF_PUBLISHED_METHODS =
            "summa.storage.http.publishedmethods";
    public static final String DEFAULT_PUBLISHED_METHODS = "";

    private Storage storage;
    private Set<Method> publishedMethods;
    private DateFormat dateFormat;

    public enum Method {
        ERROR,     // 500 Internal Error
        FORBIDDEN, // 403 Forbidden
        BAD,       // 400 Bad request
        RECORD,  // 200 or 404
        MTIME,   // 200 OK
        CHANGES, // 200 OK
        FLUSH,   // 201 Created?
        CLOSE,   // 204 No Content or 200 OK?
        CLEAR    // 204 No Content or 200 OK?
    }

    public HttpStorageBridge(Configuration conf) throws IOException {
        storage = StorageFactory.createStorage(conf, CONF_STORAGE);

        publishedMethods = new HashSet<Method>();
        for (String method : conf.getStrings(
                CONF_PUBLISHED_METHODS, new String[0])) {
            publishedMethods.add(Method.valueOf(method.toUpperCase()));
        }
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");

        log.info("Published methods: " + Strings.join(publishedMethods, ", "));
    }

    /**
     * Process a HTTP GET request, printing the returned stream to {@code out}
     * and passing the HTTP return code in the return value.
     *
     * @param out a {@code Writer} to write the feedback to
     * @param path the full tokenized path from the requested URL
     * @param query tokenized query part of the requested URL
     */
    public int doGet(Writer out, String[] path, QueryTokenizer query) {
        Method method = parseMethod(path);
        log.info("GET:\n\tMethod:" + method + "\n\tPath:"+ Strings.join(path, "|") + "\n\tQuery:" + query.toString());
        switch (method) {
            case FORBIDDEN:
                return dispatchForbidden(out, path, query);
            case BAD:
                return dispatchBad(out, path, query);
            case MTIME:
                return dispatchMtime(out, path, query);
            case RECORD:
                return dispatchRecord(out, path, query);
            case CHANGES:
                return 500;//dispatchChanges(out, path, query);
            case FLUSH:
            case CLEAR:
            case CLOSE:
                return 500;//dispatchPleasePost(out, path, query); // 405 Method Not Allowed
            default:
                return dispatchError(out, path, query, Method.ERROR,
                                     new RuntimeException("Unexpected method: "
                                                          + method));
        }
    }

    public int doPost(Writer out, String[] path, QueryTokenizer query) {
        Method method = parseMethod(path);

        switch (method) {
            case FORBIDDEN:
                return dispatchForbidden(out, path, query);
            case BAD:
                return dispatchBad(out, path, query);
            case FLUSH:
                return 500; //dispatchFlush(out, path, query);
            case CLEAR:
                return 500; //dispatchClear(out, path, query);
            case CLOSE:
                return 500; //dispatchClose(out, path, query);
            case RECORD:
            case MTIME:
            case CHANGES:
                return 500; //dispatchPleaseGet(out, path, query); // 405 Method Not Allowed
            default:
                return dispatchError(out, path, query, Method.ERROR,
                                     new RuntimeException("Unexpected method: "
                                                          + method));
        }
    }

    public Method parseMethod(String[] path) {
        if (path.length >= 1) {
            return parseMethod(path[0]);
        } else {
            log.debug("No method defined");
            return Method.BAD;
        }
    }

    public Method parseMethod(String root) {
        try {
            Method method = Method.valueOf(root.toUpperCase());
            if (!publishedMethods.contains(method)) {
                method = Method.FORBIDDEN;
            }
            return method;
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

    private PrintWriter prepareResponse(
            Writer out, String[] path, QueryTokenizer query,
            Method type, long reponseTime, String message) {
        PrintWriter w = new PrintWriter(out);
        w.println("<reponse type=\"" + type + "\">");

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

    private int closeReponse(PrintWriter w, int returnValue) {
        w.println("</response>");
        return returnValue;
    }

    // Return 403 Forbidden
    private int dispatchForbidden(
                              Writer out, String[] path, QueryTokenizer query) {
        PrintWriter w = prepareResponse(
                out, path, query, Method.FORBIDDEN, 0,
                "Forbidden Request - The requested method is not public");
        return closeReponse(w, 403);
    }

    // Return 400 Bad Request
    private int dispatchBad(Writer out, String[] path, QueryTokenizer query) {
        return dispatchBad(out, path, query,
                           "The server was unable to parse the request");
    }


    // Return 400 Bad Request
    private int dispatchBad(
            Writer out, String[] path, QueryTokenizer query, String message) {
        PrintWriter w = prepareResponse(
                out, path, query, Method.BAD, 0, "Bad Request - " + message);
        return closeReponse(w, 400);
    }

    // Return 500 Internal Error
    private int dispatchError(
            Writer out, String[] path, QueryTokenizer query, Method method, Throwable t) {
        // Write a log statement
        StringWriter toLog = new StringWriter();
        PrintWriter w = prepareResponse(
                toLog, path, query, method, 0,
                "Internal Error - Please consult the server logs for details");
        closeReponse(w, 500);
        log.error("Internal error:\n"
                  + toLog.toString() + "\n"
                  + "Error was: " + t.getMessage(), t);

        // Prepare response to client
        w = prepareResponse(
                out, path, query, method, 0,
                "Internal Error - Please consult the server logs for details");
        return closeReponse(w, 500);
    }

    private int dispatchMtime(Writer out, String[] path, QueryTokenizer query) {
        try {
            long responseTime = System.nanoTime();
            String base = null;
            if (path.length >= 2) {
                base = path[1];
            }
            long mtime = storage.getModificationTime(base);
            responseTime = System.nanoTime() - responseTime;

            PrintWriter w = prepareResponse(
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

    private int dispatchRecord(Writer out, String[] path, QueryTokenizer query) {
        try {
            long responseTime = System.nanoTime();
            if (path.length < 2) {
                return dispatchBad(out, path, query,
                                   "No record id specified in URL");
            }
            String[] ids = path[1].split(";");
            QueryOptions opts = parseQuery(query);
            List<Record> recs = storage.getRecords(Arrays.asList(ids), opts);
            responseTime = System.nanoTime() - responseTime;

            PrintWriter w = prepareResponse(
                    out, path, query, Method.RECORD, responseTime, null);

            if (recs.isEmpty()) {
                w.println("  <records/>");
                return closeReponse(w, 404);
            } else {
                w.println("  <records>");
                for (Record rec : recs) {
                    w.println(RecordUtil.toXML(rec));
                }
                w.println("  </records>");
                return closeReponse(w, 200);
            }
        } catch (Exception e) {
            return dispatchError(out, path, query, Method.MTIME, e);
        }
    }
}
