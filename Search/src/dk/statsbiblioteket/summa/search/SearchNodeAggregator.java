/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.SearchNode;
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
        log.debug(String.format(
                "Constructing %s SearchNodeAggregator with %d SearchNodes",
                sequential ? "sequential" : "parallel", nodes.size()));
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
