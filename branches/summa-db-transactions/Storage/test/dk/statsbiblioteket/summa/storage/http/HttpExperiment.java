package dk.statsbiblioteket.summa.storage.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Experimental class used to play around with the HTTPServer shipped in
 * Java 1.6
 */
public class HttpExperiment {

    public static class Handler implements HttpHandler {

        public void handle (HttpExchange t) throws IOException {
            CharBuffer buf = CharBuffer.allocate(1024);

            InputStream is = t.getRequestBody();
            new InputStreamReader(is).read(buf);
            URI uri = t.getRequestURI();

            System.out.println("Got request: " + buf.toString());
            System.out.println("Method: " + t.getRequestMethod());
            System.out.println("URI: " + uri);
            System.out.println("Fragment: " + uri.getFragment());
            System.out.println("Query: " + uri.getQuery());

            String response = "This is the response to: " + uri.getQuery();// + buf.toString();

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();

        }
    }

    public static void main (String[] args) throws Exception {
        // New server on port 8000 with a max backlog of 10 connections
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 10);

        server.createContext("/myapp", new Handler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

}
