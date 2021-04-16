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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.h2.H2Storage;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Experimental class used to play around with the HTTPServer shipped in
 * Java 1.6
 */
public class HttpExperiment extends TestCase {

    public static class Handler implements HttpHandler {

        private HttpStorageBridge bridge;

        public Handler(HttpStorageBridge bridge) {
            this.bridge = bridge;
        }

        @Override
        public void handle (HttpExchange t) throws IOException {
            CharBuffer buf = CharBuffer.allocate(1024);

            InputStream is = t.getRequestBody();
            new InputStreamReader(is, StandardCharsets.UTF_8).read(buf);
            URI uri = t.getRequestURI();


            //StringWriter out = new StringWriter();
            String[] path = uri.getPath().substring(1).split("/");
            ByteArrayOutputStream resp = new ByteArrayOutputStream();

            int result;
            if ("GET".equals(t.getRequestMethod())) {
                result = bridge.doGet(resp, path,
                                      new QueryTokenizer(uri.getQuery()));
            } else if ("POST".equals(t.getRequestMethod())) {
                result = bridge.doPost(
                        t.getRequestBody(), resp, path,
                        new QueryTokenizer(uri.getQuery()));
            } else {
                System.err.println("Unsupported HTTP method: "
                                   + t.getRequestMethod());
                result = 500;
            }
            t.sendResponseHeaders(result, resp.size());
            resp.writeTo(t.getResponseBody());
            t.getResponseBody().close();
            
        }
    }

    public void testDummy() {
        assertTrue(true);
    }

    public static void main (String[] args) throws Exception {
        // New server on port 8000 with a max backlog of 10 connections
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 10);
        HttpStorageBridge bridge = new HttpStorageBridge(
                Configuration.newMemoryBased(
                        HttpStorageBridge.CONF_STORAGE, H2Storage.class,
                        HttpStorageBridge.CONF_GET_METHODS, "record,mtime",
                        HttpStorageBridge.CONF_POST_METHODS, "record"
                )
        );

        server.createContext("/", new Handler(bridge));
        server.setExecutor(null); // creates a default executor
        server.start();
    }

}

