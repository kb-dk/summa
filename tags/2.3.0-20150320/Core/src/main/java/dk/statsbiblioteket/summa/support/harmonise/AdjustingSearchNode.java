/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.search.SearchNode;
import dk.statsbiblioteket.summa.search.SearchNodeFactory;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;

/**
 * Pipes requests and responses to and from a SearchNode through an {@link InteractionAdjuster}.
 * The property {@link #CONF_INNER_SEARCHNODE} must be specified and appropriate properties from
 * {@link InteractionAdjuster} should be specified.
 * </p><p>
 * Note that rewriting of warmup queries is not performed as there is no standard for the nature of these queries.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class AdjustingSearchNode implements SearchNode {
    private static Log log = LogFactory.getLog(AdjustingSearchNode.class);

    /**
     * A sub-configuration with the setup for the SearchNode that is to be  created and used for all calls.
     * The configuration must contain the property
     * {@link dk.statsbiblioteket.summa.search.SearchNodeFactory#CONF_NODE_CLASS} as
     * {@link dk.statsbiblioteket.summa.search.SearchNodeFactory} is used for creating the single inner node.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_INNER_SEARCHNODE = "adjuster.inner.searchnode";

    private final SearchNode inner;
    private final InteractionAdjuster adjuster;

    public AdjustingSearchNode(Configuration conf) {
        if (!conf.valueExists(CONF_INNER_SEARCHNODE)) {
            throw new ConfigurationException(
                "No inner search node defined. A proper sub-configuration must exist for key " + CONF_INNER_SEARCHNODE);
        }
        try {
            inner = SearchNodeFactory.createSearchNode(conf.getSubConfiguration(CONF_INNER_SEARCHNODE));
        } catch (RemoteException e) {
            throw new ConfigurationException(
                "Unable to create inner search node, although a value were present for key " + CONF_INNER_SEARCHNODE,
                e);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException(
                "A configuration with support for sub configurations must be provided for the adjuster and must "
                + "contain a sub configuration with key " + CONF_INNER_SEARCHNODE, e);
        }
        adjuster = new InteractionAdjuster(conf);
        log.debug("Created AdjustingSearchNode with inner SearchNode " + inner);
    }

    @Override
    public void search(Request request, ResponseCollection responses) throws RemoteException {
        log.debug("Rewriting request, performing search and adjusting responses");
        long startTime = System.currentTimeMillis();
        Request adjusted = adjuster.rewrite(request);
        inner.search(adjusted, responses);
        adjuster.adjust(adjusted, responses);
        responses.addTiming("adjustingsearchnode.total", System.currentTimeMillis() - startTime);
    }

    @Override
    public void warmup(String request) {
        inner.warmup(request);
    }

    @Override
    public void open(String location) throws RemoteException {
        inner.open(location);
    }

    @Override
    public void close() throws RemoteException {
        inner.close();
    }

    @Override
    public int getFreeSlots() {
        return inner.getFreeSlots();
    }

    @Override
    public String toString() {
        return "AdjustingSearchNode(" + adjuster.getId() + " for " + super.toString() + ")";
    }
}
