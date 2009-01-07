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
package dk.statsbiblioteket.summa.common.filter;

import java.io.StringWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.StateThread;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class creates a given number of filter chains and pumps contents
 * through them until they are all empty. The class allows for chains to be
 * executed either sequentially or in parallel, depending on configuration.
 */
// TODO: Add scheduling capabilities
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FilterControl extends StateThread implements Configurable,
                                                          FilterChainHandler {
    private final Log log = LogFactory.getLog(FilterControl.class);

    private List<FilterPump> pumps;

    /**
     * The a list of Configurations for the chains. Each configuration will be
     * used for the constructor in {@link FilterPump}.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_CHAINS = "filtercontrol.chains";

    /**
     * If true, the chains will be started in order of appearance: the
     * FilterControl will wait for any previous chain to finish, before starting
     * the next one.
     * If false, all chains will be started simultaneously.
     * <p/>
     * Optional. Default is true.
     */
    public static final String CONF_SEQUENTIAL = "filtercontrol.sequential";
    public static final boolean DEFAULT_SEQUENTIAL = true;

    private boolean sequential = DEFAULT_SEQUENTIAL;

    /**
     * The FilterControl sets up the Filter Chains defines by the configuration.
     * The chains aren't pumped before {@link #start} is called.
     * @param configuration setup for the FilterControl's underlying filter
     *                      chains.
     * @throws ConfigurationException if the construction could not be
     *                      completed.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public FilterControl(Configuration configuration) throws
                                                      ConfigurationException {
        log.trace("Creating FilterControl");
        List<Configuration> chainConfs;
        try {
            chainConfs = configuration.getSubConfigurations(CONF_CHAINS);
        } catch (IOException e) {
            throw new ConfigurationException(String.format(
                    "Could not locate a list of chain-Configurations at key %s",
                    CONF_CHAINS), e);
        }
        pumps = new ArrayList<FilterPump>(chainConfs.size());
        for (Configuration chainConf : chainConfs) {
            try {
                FilterPump pump = new FilterPump(chainConf);
                log.info("Created chain '" + pump + "'");
                pumps.add(pump);
            } catch (Exception e) {
                throw new ConfigurationException(String.format(
                         "Error creating chain '%s': " + e.getMessage(),
                         chainConf), e);
            }
        }
        try {
            sequential = configuration.getBoolean(CONF_SEQUENTIAL);
            log.info("Sequential ingest set to " + sequential);
        } catch (Exception e) {
            log.info(CONF_SEQUENTIAL + " not specified. Defaulting to "
                     + sequential);
        }
    }

    @Override
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    protected void runMethod() {
        log.info("Activating filter pump(s)");
        for (FilterPump pump: pumps) {
            if (getStatus() != STATUS.running) {
                break;
            }
            try {
                pump.start();
                if (sequential) {
                    log.debug("Waiting for filter chain '" + pump.getChainName()
                              + "' to finish");
                    try {
                        pump.waitForFinish();
                    } catch (InterruptedException e) {
                        log.warn("run: Interrupted while waiting for '"
                                 + pump.getChainName() + "' to finish");
                    }
                }
            } catch (Exception e) {
                log.error("Unable to start pump for filter chain '"
                          + pump.getChainName() + "'");
            }
        }
        if (!sequential) {
            log.info("Waiting for chains to finish");
            for (FilterPump pump: pumps) {
                try {
                    pump.waitForFinish();
                } catch (InterruptedException e) {
                    log.warn("run: Interrupted while waiting for '"
                             + pump.getChainName() + "' to finish");
                }
            }
        }
    }

    /**
     * Expansion of stop that calls stop on all filter pumps.
     */
    @Override
    public void stop() {
        if (pumps == null) {
            log.warn("stop called for uninitialized pumps");
            return;
        }
        log.info("Stopping all pumps");
        super.stop();
        for (FilterPump pump: pumps) {
            pump.stop();
        }
        log.trace("Pumps stopped");
        // TODO: Add graceful timeout for pumping so cached data are processed
    }

    /**
     * @return a human-readable status consisting of overall status plus
     *         status for all pumps.
     */
    public String getVerboseStatus() {
        StringWriter sw = new StringWriter(500);
        sw.append(getStatus().toString()).append(": ");
        for (int i = 0 ; i < pumps.size() ; i++) {
            sw.append(pumps.get(i).getChainName()).append("(");
            sw.append(pumps.get(i).getStatus().toString()).append(")");
            if (i < pumps.size() - 1) {
                sw.append(", ");
            }
        }
        return sw.toString();
    }

    // TODO: Consider if this should not be accessible
    public List<FilterPump> getPumps() {
        return pumps;
    }
}
