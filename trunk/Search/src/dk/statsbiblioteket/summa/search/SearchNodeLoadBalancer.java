/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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

import java.util.List;
import java.util.ArrayList;
import java.rmi.RemoteException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Treats a list of SearchNodes as equal in functionality and distributes
 * searches among them. The load-balancer favors nodes with the highest
 * amount of free slots and gives no guarantees as to selection among nodes
 * with an equal number of free slots.
 * </p><p>
 * The list of SearchNodes is specified as per
 * {@link SearchNodeFactory#createSearchNodes(Configuration)}. Normally only a
 * single SearchNode will be specified here and
 * {@link #CONF_SEARCHER_INSTANCES} will determine how many instances that
 * should be created of the node.
 * </p><p>
 * Open and warmup is performed for all nodes in sequence, which ensures
 * that queries can still be performed during open, as long as there are at
 * least 2 searchers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchNodeLoadBalancer implements SearchNode {
    private static Log log = LogFactory.getLog(SearchNodeLoadBalancer.class);

    /**
     * The number of underlying searchers to instantiate. The optimum number is
     * highly depending on the underlying search engine and the hardware.
     * Experiments with Lucene at Statsbiblioteket suggests that the best value
     * for this searcher is around 1½ * the number of CPUs, but as always YMMW.
     * Note that the official recommendation from the Lucene people is to have
     * a single searcher, no matter the number of CPUs.
     * </p><p>
     * This is optional. The default is 2.
     */
    public static final String CONF_SEARCHER_INSTANCES =
            "summa.search.searcherinstances";
    public static final int DEFAULT_SEARCHER_INSTANCES = 2;

    private int instances = DEFAULT_SEARCHER_INSTANCES;
    private List<SearchNode> nodes;

    public SearchNodeLoadBalancer(Configuration conf) throws RemoteException {
        instances = conf.getInt(CONF_SEARCHER_INSTANCES, instances);
        log.trace(String.format(
                "Constructing SearchNodeLoadBalancer with %d instances",
                instances));
        for (int i = 0 ; i < instances ; i++) {
            List<SearchNode> baseNodes =
                    SearchNodeFactory.createSearchNodes(conf);
            if (nodes == null) {
                nodes = new ArrayList<SearchNode>(instances * baseNodes.size());
            }
            log.trace(String.format("Adding %d nodes", baseNodes.size()));
            nodes.addAll(baseNodes);
        }
        if (nodes == null) {
            log.warn("No SearchNodes defined in configuration. This is probably"
                     + " an error. Specify SearchNodes under '"
                     + SearchNodeFactory.CONF_NODES + "'");
            nodes = new ArrayList<SearchNode>(0);
        }
        log.debug(String.format(
                "Balancer created with a total of %d SearchNodes",
                nodes.size()));
    }

    public void open(String location) throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.debug(String.format("open(%s) called", location));
        for (SearchNode node: nodes) {
            node.open(location);
        }
    }

    // TODO: Consider quarantine for exception-throwing
    // FIXME: We have a race-condition from calculation of slots to search
    // FIXME: Handle open
    // TODO: Try another searcher upon exception
    public void search(Request request, ResponseCollection responses) throws
                                                               RemoteException {
        SearchNode bestCandidate = null;
        for (SearchNode node: nodes) {
            //noinspection AssignmentToNull
            bestCandidate = bestCandidate == null ?
                            node.getFreeSlots() > 0 ? node : null :
                            bestCandidate.getFreeSlots() > node.getFreeSlots() ?
                            bestCandidate : node;
        }
        if (bestCandidate == null) {
            log.warn("No free slots in any SearchNodes, trying first node");
            bestCandidate = nodes.get(0);
        }
        bestCandidate.search(request, responses);
    }

    public void warmup(String request) {
        log.debug(String.format("warmup(%s) called", request));
        for (SearchNode node: nodes) {
            node.warmup(request);
        }
    }

    public void close() throws RemoteException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("close() called");
        RemoteException re = null;
        for (SearchNode node: nodes) {
            try {
                node.close();
            } catch (RemoteException e) {
                log.warn("Encountered an exception during close. "
                         + "Continuing close of the remaining nodes", e);
                // Problem: Previous exceptions are discarded 
                re = e;
            }
        }
        if (re != null) {
            throw new RemoteException("Exception encountered during close", re);
        }
    }

    /**
     * The number of free slots is the sum of free slots on the underlying
     * nodes. In a multi-threaded environment, this will be an approximation.
     * @return the total number of free slots.
     */
    public int getFreeSlots() {
        int slots = 0;
        for (SearchNode node: nodes) {
            slots += node.getFreeSlots();
        }
        return slots;
    }
}



