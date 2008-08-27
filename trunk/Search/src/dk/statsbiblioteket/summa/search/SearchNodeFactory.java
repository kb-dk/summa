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

import java.util.List;
import java.util.ArrayList;
import java.rmi.RemoteException;
import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.search.api.SearchNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Constructs a tree of SearchNodes from properties. It is expected that
 * SearchNodes themselves will call this factory in order to create sub-nodes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SearchNodeFactory {
    private static Log log = LogFactory.getLog(SearchNodeFactory.class);
    /**
     * The class for the SearchNode to construct.
     */
    public static final String CONF_NODE_CLASS = "summa.search.node.class";

    /**
     * The key for a list of sub-properties, each containing the configuration
     * for a SearchNode.
     */
    public static final String CONF_NODES = "summa.search.nodes";

    /**
     * The recommended key for a single sub-property containing setup for a
     * SearchNode.
     */
    public static final String CONF_NODE = "summa.search.node";

    /**
     * Constructs a SearchNode with the class given in the property
     * {@link #CONF_NODE_CLASS}. conf itself is given to the constructor for
     * the class.
     * @param conf the configuration for the SearchNode-tree.
     * @return a SearchNode based on the configuration.
     * @throws RemoteException if the SearchNode could not be created.
     */
    public static SearchNode createSearchNode(Configuration conf) throws
                                                               RemoteException {
        String searchNodeClassName;
        try {
            searchNodeClassName = conf.getString(CONF_NODE_CLASS);
        } catch (NullPointerException e) {
            throw new RemoteException(String.format(
                    "The property '%s' could not be located in properties",
                    CONF_NODE_CLASS));
        }
        return createSearchNode(searchNodeClassName, conf);
    }

    /**
     * Like {@link #createSearchNode(Configuration)} but fall back to using
     * a {@code defaultClass} to instantiate the search node if
     * {@link #CONF_NODE_CLASS} is not defined in {@code conf}
     *
     * @param conf The configuration used to look up {@link #CONF_NODE_CLASS}
     * @param defaultClass Fallback class if {@link #CONF_NODE_CLASS} is not
     *                     found in {@code conf}
     * @return a newly instantiated {@link SearchNode}
     * @throws RemoteException if the SearchNode could not be created.
     */
    public static SearchNode createSearchNode(Configuration conf,
                                              Class<?extends SearchNode> defaultClass)
                                              {

        Class<? extends SearchNode> searchNodeClass =
                Configuration.getClass(CONF_NODE_CLASS, SearchNode.class,
                                       defaultClass, conf);

        return Configuration.create (searchNodeClass, conf);
    }

    /**
     * Extracts the sub Configuration with the key key from properties and
     * calls {@link #createSearchNode(Configuration)} with that configuration.
     * @param conf the configuration that contains the wanted sub configuration.
     * @param key  the key for the wanted sub configuration.
     * @return a SearchNode created from the sub configuration at key.
     * @throws RemoteException if the SearchNode could not be created.
     */
    public static SearchNode createSearchNode(Configuration conf, String key)
                                                        throws RemoteException {
        Configuration sub;
        try {
            sub = conf.getSubConfiguration(key);
        } catch (IOException e) {
            throw new RemoteException(String.format(
                    "Could not extract the sub configuration '%s'", key));
        }
        return createSearchNode(sub);
    }


    /**
     * Constructs a list of nodes from all the sub-properties in
     * {@link #CONF_NODES}. The CONF_NODES-property must contain a list of
     * properties, each one containing at least {@link #CONF_NODE_CLASS}.
     * @param conf the configuration for the SearchNodes.
     * @return a list of SearchNodes based on the configuration.
     * @throws RemoteException if the nodes could not be created.
     */
    // TODO: Better JavaDoc
    public static List<SearchNode> createSearchNodes(Configuration conf) throws
                                                               RemoteException {
        List<Configuration> nodeConfs;
        try {
            nodeConfs = conf.getSubConfigurations(CONF_NODES);
        } catch (IOException e) {
            throw new Configurable.ConfigurationException(String.format(
                    "Could not extract a list of XProperties for SearchNodes "
                    + "from configuration with key '%s'", CONF_NODES), e);
        }
        List<SearchNode> nodes =
                new ArrayList<SearchNode>(nodeConfs.size());
        for (Configuration nodeConf: nodeConfs) {
            nodes.add(createSearchNode(nodeConf));
        }
        return nodes;
    }

    private static SearchNode createSearchNode(String searchNodeClassName,
                                               Configuration conf) {
        log.trace("Getting SearchNode class '" + searchNodeClassName + "'");
        Class<? extends SearchNode> searchNodeClass =
                Configuration.getClass(CONF_NODE_CLASS, SearchNode.class, conf);
        log.debug("Got SearchNode class " + searchNodeClass
                  + ". Creating instance");
        return Configuration.create(searchNodeClass, conf);
    }

}
