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
package dk.statsbiblioteket.summa.ingest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.StateThread;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The ingester creates a given number of filter chains and pumps contents
 * through them until they are all empty. The ingester allows for chains to be
 * executed either sequentially or in parallel, depending on configuration.
 */
// TODO: Add scheduling capabilities
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Ingester extends StateThread implements Configurable {
    private final Log log = LogFactory.getLog(Ingester.class);

    private List<FilterPump> pumps;

    /**
     * The a list of names of chains to create.
     * </p><p>
     * A sub-configuration will be requested from the configuration with the
     * name as key and that configuration will be used for creating a
     * FilterPump.
     */
    public static final String CONF_CHAINS = "Ingester.Chains";

    /**
     * If true, the chains will be started in order of appearance, the Ingester
     * will wait for any previous chain to finish, before starting the next one.
     * If false, all chains will be started simultaneously.
     * </p><p>
     * Default: True.
     */
    public static final String CONF_SEQUENTIAL = "Ingester.Sequential";

    private boolean sequential = true;

    /**
     * The Ingester sets up the Filter Chains defines by the configuration.
     * The chains aren't pumped before {@link #start} is called.
     * @param configuration setup for the Ingester's underlying filter chains.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public Ingester(Configuration configuration) {
        log.trace("Creating Ingester");
        List<String> chains = configuration.getStrings(CONF_CHAINS);
        pumps = new ArrayList<FilterPump>(chains.size());
        for (String chain: chains) {
            log.info("Creating chain '" + chain + "'");
            try {
                Configuration subConfiguration =
                        configuration.getSubConfiguration(chain);
                log.trace("Got configuration for chain '" + chain + "'");
                FilterPump pump = new FilterPump(subConfiguration);
                pumps.add(pump);
            } catch (IOException e) {
                log.error("IOException while creating chain '" + chain
                          + "'. Skipping", e);
            } catch (Exception e) {
                log.error("Could not create chain '" + chain + "'. Skipping",
                          e);
            }
        }
        try {
            sequential = configuration.getBoolean(CONF_SEQUENTIAL);
            log.info("Sequential ingest set to " + sequential);
        } catch (Exception e) {
            log.info(CONF_SEQUENTIAL + " not specified. Defaulting to true");
        }
    }

    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void run() {
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
}
