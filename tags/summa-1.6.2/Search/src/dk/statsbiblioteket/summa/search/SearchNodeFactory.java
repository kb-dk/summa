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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

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
     */
    public static SearchNode createSearchNode(
            Configuration conf, Class<?extends SearchNode> defaultClass) {
        log.trace("createSearchNode called");

        Class<? extends SearchNode> searchNodeClass =
                Configuration.getClass(CONF_NODE_CLASS, SearchNode.class,
                                       defaultClass, conf);
        log.debug("Creating SearchNode '" + searchNodeClass.getName() + "'");
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
        } catch (SubConfigurationsNotSupportedException e) {
            throw new RemoteException(
                    "Storage doesn't support sub configurations");
        } catch (NullPointerException e) {
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
     * </p><p>
     * If index descriptor setup is present in the configuration, it will be
     * copied to all sub search nodes.
     * @return a list of SearchNodes based on the configuration.
     * @throws RemoteException if the nodes could not be created.
     * @see IndexDescriptor#CONF_DESCRIPTOR
     * @see IndexDescriptor#copySetupToSubConfigurations
     */
    // TODO: Better JavaDoc
    public static List<SearchNode> createSearchNodes(Configuration conf) throws
                                                               RemoteException {
        List<Configuration> nodeConfs;
        try {
            nodeConfs = conf.getSubConfigurations(CONF_NODES);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new Configurable.ConfigurationException(
                    "Storage doesn't support sub configurations");
        } catch (NullPointerException e) {
            throw new Configurable.ConfigurationException(String.format(
                    "Could not extract a list of XProperties for SearchNodes "
                    + "from configuration with key '%s'", CONF_NODES), e);
        }
        IndexDescriptor.copySetupToSubConfigurations(conf, nodeConfs);
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




