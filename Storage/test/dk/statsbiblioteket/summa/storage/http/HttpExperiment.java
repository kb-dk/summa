package dk.statsbiblioteket.summa.storage.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.nio.CharBuffer;
import java.net.InetSocketAddress;
import java.net.URI;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.Streams;

/**
 * Experimental class used to play around with the HTTPServer shipped in
 * Java 1.6
 */
public class HttpExperiment {

    public static class Handler implements HttpHandler {

        private HttpStorageBridge bridge;

        public Handler(HttpStorageBridge bridge) {
            this.bridge = bridge;
        }

        public void handle (HttpExchange t) throws IOException {
            CharBuffer buf = CharBuffer.allocate(1024);

            InputStream is = t.getRequestBody();
            new InputStreamReader(is).read(buf);
            URI uri = t.getRequestURI();


            StringWriter out = new StringWriter();
            String[] path = uri.getPath().substring(1).split("/");
            int result = bridge.doGet(out, path,
                                      new QueryTokenizer(uri.getQuery()));
            t.sendResponseHeaders(result, out.getBuffer().length());

            // FIXME: Argh, horribly inefficient but this is a hack, so wth
            OutputStream resp = t.getResponseBody();
            resp.write(out.toString().getBytes());
            resp.close();

        }
    }

    public static void main (String[] args) throws Exception {
        // New server on port 8000 with a max backlog of 10 connections
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 10);
        HttpStorageBridge bridge = new HttpStorageBridge(
                Configuration.newMemoryBased(
                        HttpStorageBridge.CONF_STORAGE, H2Storage.class,
                        HttpStorageBridge.CONF_PUBLISHED_METHODS, "record,mtime"
                )
        );

        server.createContext("/", new Handler(bridge));
        server.setExecutor(null); // creates a default executor
        server.start();
    }

}
