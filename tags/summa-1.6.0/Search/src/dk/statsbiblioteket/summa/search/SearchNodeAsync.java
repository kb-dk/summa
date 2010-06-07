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
package dk.statsbiblioteket.summa.search;

import java.util.concurrent.Callable;
import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.Request;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple wrapper for a SearchNode that makes it a Callable and Runnable,
 * useful for wrapping in FutureTask or similar.
 * </p><p>
 * Note: While it is possible to re-use the wrapper, remember that it is not
 *       thread-safe.
 * </p><p>
 * Note: getFreeSlots is not done asynchronously.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "JavaDoc needed")
public class SearchNodeAsync implements Callable<Object>, Runnable, SearchNode {
    private static Log log = LogFactory.getLog(SearchNodeAsync.class);

    private SearchNode node;
    private Request request;
    private String warmupRequest;
    private String location;
    private ResponseCollection responses;
    private CALL call = CALL.uninitialized;
    private Exception e = null;


    private enum CALL {uninitialized, search, warmup, open, close}

    public SearchNodeAsync(SearchNode node) {
        this.node = node;
    }

    public Object call() throws Exception {
        switch (call) {
            case search:
                node.search(request, responses);
                break;
            case warmup:
                node.warmup(warmupRequest);
                break;
            case open:
                node.open(location);
                break;
            case close:
                node.close();
                break;
            case uninitialized:
                throw new IllegalStateException("Not initialized");
        }
        return null;
    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            log.warn("Encountered xception in run for task " + call, e);
            this.e = e;
        }
    }

    /**
     * @return The last throws Exception during run, if any.
     */
    public Exception getE() {
        return e;
    }

    /* SearchNode interface */

    public void search(Request request,
                       ResponseCollection responses) throws RemoteException {
        this.request = request;
        this.responses = responses;
        call = CALL.search;
    }

    public void warmup(String request) {
        warmupRequest = request;
        call = CALL.warmup;
    }

    public void open(String location) throws RemoteException {
        this.location = location;
        call = CALL.open;

    }

    public void close() throws RemoteException {
        call = CALL.close;
    }

    public int getFreeSlots() {
        return node.getFreeSlots();
    }

}




