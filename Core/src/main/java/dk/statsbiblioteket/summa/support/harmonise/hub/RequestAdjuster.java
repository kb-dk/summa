/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.statsbiblioteket.summa.support.harmonise.hub;

import dk.statsbiblioteket.summa.support.harmonise.hub.core.HubAggregatorBase;
import org.apache.solr.common.params.SolrParams;

import java.util.List;

/**
 * Modifies incoming requests.
 */
public interface RequestAdjuster {

    /**
     * @param params     original parameters for the calling HubComposite.
     * @param components the components that will be called, including component-specific parameters.
     * @return the adjusted component list.
     */
    List<HubAggregatorBase.ComponentCallable> adjustRequests(
            SolrParams params, List<HubAggregatorBase.ComponentCallable> components);
}
