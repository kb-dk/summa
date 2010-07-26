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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * All interaction with this SearchNode results in the same interaction
 * performed over all the contained SearchNodes. Depending on setup this
 * will either be done sequentially or in parallel.
 * </p><p>
 * The underlying Search-nodes are specified by the property
 * {@link SearchNodeFactory#CONF_NODES} and are constructed using
 * {@link SearchNodeFactory}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class SearchNodeAggregator extends ArrayList<SearchNode> implements
                                                                    SearchNode {
    private static final long serialVersionUID = 8974858541L; 
    private static Log log = LogFactory.getLog(SearchNodeAggregator.class);

    /**
     * If true, all requests are performed in sequence for the underlying
     * searchers. If false, all requests are performed in parallel.
     * </p><p>
     * Optional. default is true.
     */
    public static final String CONF_SEQUENTIAL =
                                           "summa.search.aggregator.sequential";
    public static final boolean DEFAULT_SEQUENTIAL = true;

    private boolean sequential = DEFAULT_SEQUENTIAL;

    public SearchNodeAggregator(Configuration conf) throws RemoteException {
        sequential = conf.getBoolean(CONF_SEQUENTIAL, DEFAULT_SEQUENTIAL);
        List<SearchNode> nodes = SearchNodeFactory.createSearchNodes(conf);
        log.info(String.format(
                "Constructed %s SearchNodeAggregator with %d SearchNodes",
                sequential ? "sequential" : "parallel", nodes.size()));
        for (SearchNode node: nodes) {
            log.trace("Sub-node: " + node.getClass().getName());
        }
        addAll(nodes);
    }

    public void search(final Request request,
                       final ResponseCollection responses) throws
                                                              RemoteException {
        log.trace("Starting search");
        doTask(new Closure() {
            public void action(SearchNode node) throws RemoteException {
                node.search(request, responses);
            }
        });
    }

    public void warmup(final String request) {
        if (log.isTraceEnabled()) {
            log.trace("Performing warmup with " + request);
        }
        try {
            doTask(new Closure() {
                public void action(SearchNode node) throws RemoteException {
                    node.warmup(request);
                }
            });
        } catch (RemoteException e) {
            log.warn("encountered an exception during warmup. Continuing", e);
        }
    }

    public void open(final String location) throws RemoteException {
        log.debug(String.format("open(%s) called", location));
        doTask(new Closure() {
            public void action(SearchNode node) throws RemoteException {
                node.open(location);
            }
        });
    }
    public void close() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("close() called");
        try {
            doTask(new Closure() {
                public void action(SearchNode node) throws RemoteException {
                    node.close();
                }
            });
        } catch (RemoteException e) {
            log.error("Got a RemoteException during close. This should not "
                      + "happen", e);
        }
    }

    /**
     * @return the minimum free slots for the underlying searchers.
     */
    public int getFreeSlots() {
        log.trace("Calculating free slots");
        int maxC = 0;
        for (SearchNode node: this) {
            maxC = maxC == 0 ? node.getFreeSlots() :
                   Math.min(maxC, node.getFreeSlots());
        }
        return maxC;
    }

    /**
     * a closure for performing an action on a SearchNode.
     */
    private static interface Closure {
        public void action(SearchNode node) throws RemoteException;
    }
    private void doTask(Closure closure) throws RemoteException {
        if (sequential) {
            for (SearchNode node: this) {
                closure.action(node);
            }
        } else {
            log.trace("Creating and starting " + size() + " future tasks");
            List<FutureTask<Object>> futures =
                    new ArrayList<FutureTask<Object>>(size());
            for (SearchNode node: this) {
                SearchNodeAsync aNode = new SearchNodeAsync(node);
                closure.action(aNode);
                FutureTask<Object> future = new FutureTask<Object>(aNode);
                future.run();
                futures.add(future);
            }
            log.trace("Waiting for future tasks to finish");
            for (FutureTask<Object> future: futures) {
                // TODO: Consider using a timeout here
                try {
                    future.get();
                } catch (InterruptedException e) {
                    throw new RemoteException("Interrupted while waiting for "
                                              + "future task to finish", e);
                } catch (ExecutionException e) {
                    throw new RemoteException("Interrupted while executing "
                                              + "future tasks", e);
                }
            }
        }

    }
}




