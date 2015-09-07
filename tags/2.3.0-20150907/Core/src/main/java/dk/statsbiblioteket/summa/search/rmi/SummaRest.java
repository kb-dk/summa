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
package dk.statsbiblioteket.summa.search.rmi;

import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;
import dk.statsbiblioteket.summa.storage.api.ReadableStorage;
import dk.statsbiblioteket.summa.storage.api.WritableStorage;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * A singleton REST service for searchers and storages. Instances of Searchers and Storages should connect to this
 * class in order to get exposed externally. The interface exposes both Object-based calls and JSON/XML-based.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT)
@Path("/rest")
public class SummaRest {
    private static final Log log = LogFactory.getLog (SummaRest.class);

    public static final String ID = "id";
    public static final String REQUEST = "request";
    
    private static SummaRest singleton = null;
    private final Map<String, SummaSearcher> searchers = new HashMap<>();
    private final Map<String, ReadableStorage> readStorages = new HashMap<>();
    private final Map<String, WritableStorage> writeStorages = new HashMap<>();

    public SummaRest() {
        singleton = this;
        log.info("Created " + this);
    }

    public static synchronized SummaRest getInstance() {
        if (singleton == null) {
            new SummaRest();
        }
        return singleton;
    }

    /**
     * If the code is not running in a servlet container, create one!
     */
    public void checkContainer() {
        // http://stackoverflow.com/questions/2976884/detect-if-running-in-servlet-container-or-standalone
        try {
          new InitialContext().lookup("java:comp/env");
            log.info("Running inside servlet container. Explicit server creation skipped");
            return;
        } catch (NamingException ex) {
            // Outside of container
        }

    }

    /**
     * Adds a searcher to the service.
     * @param id       used when calling the searcher.
     * @param searcher must be fully initialized and ready for use.
     * @return the searcher previously assigned to the id, if any.
     */
    public synchronized SummaSearcher register(String id, SummaSearcher searcher) {
        log.info("Registered " + id + ": " + searcher);
        return searchers.put(id, searcher);
    }
    
    /**
     * Adds a readable storage to the service.
     * @param id       used when calling the searcher.
     * @param storage  must be fully initialized and ready for use.
     * @return the storage previously assigned to the id, if any.
     */
    public synchronized ReadableStorage register(String id, ReadableStorage storage) {
        log.info("Registered " + id + ": " + storage);
        return readStorages.put(id, storage);
    }
    
    /**
     * Adds a writable storage to the service.
     * @param id       used when calling the searcher.
     * @param storage  must be fully initialized and ready for use.
     * @return the storage previously assigned to the id, if any.
     */
    public synchronized WritableStorage register(String id, WritableStorage storage) {
        log.info("Registered " + id + ": " + storage);
        return writeStorages.put(id, storage);
    }

    /**
     * Remove the handler with the given ID from the SummaRest server.
     * @param id the id for a searcher or storage.
     * @return true if a handler for the given ID was registered.
     */
    public synchronized boolean unregister(String id) {
        return searchers.remove(id) != null ||
               readStorages.remove(id) != null ||
               writeStorages.remove(id) != null;
    }

    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public ResponseCollection searchBinary(@PathParam(ID) String id, @PathParam(REQUEST) Request request)
            throws RemoteException {
        try {
            return getHandler(id, searchers, "searcher").search(request);
        } catch (IOException e) {
            throw new RemoteException(
                    "Unable to perform binary search for " + request + " with searcher '" + id + "'", e);
        }
    }

    @GET
    @Produces({ MediaType.TEXT_XML})
    public String searchJSON(@PathParam(ID) String id, @PathParam(REQUEST) String json) throws RemoteException {
        try {
            Request request = new Request();
            request.addJSON(json);
            return getHandler(id, searchers, "searcher").search(request).toXML();
        } catch (IOException e) {
            throw new RemoteException("Unable to perform JSON search for " + json + " with searcher '" + id + "'", e);
        }
    }

    // TODO: Mirror methods from ReadableStorage and WritableStorage

    private <H> H getHandler(String id, Map<String, H> handlers, String designation) throws RemoteException {
        if (id == null || id.isEmpty()) {
            if (handlers.size() == 1) {
                return handlers.values().iterator().next();
            }
            throw new RemoteException("No ID stated and #" + designation + " was " + handlers.size() + ", not 1");
        }
        H handler = handlers.get(id);
        if (handler == null) {
            throw new RemoteException("No " + designation + " with ID '" + id + "'");
        }
        return handler;
    }

    @Override
    public String toString() {
        return String.format("SummaRest(searchers=[%s], readable storages=[%s], writable storages=[%s])",
                             toString(searchers), toString(readStorages), toString(writeStorages));
    }
    private <O> String toString(Map<String, O> handlers) {
        StringBuilder sb = new StringBuilder(handlers.size() * 100);
        for (Map.Entry<String, O> entry: handlers.entrySet()) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(entry.getValue()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}
