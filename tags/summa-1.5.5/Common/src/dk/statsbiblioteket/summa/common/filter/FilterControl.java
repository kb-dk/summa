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
package dk.statsbiblioteket.summa.common.filter;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.util.StateThread;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
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
    private static final Log log = LogFactory.getLog(FilterControl.class);

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
        log.debug("Constructing");
        List<Configuration> chainConfs;
        try {
            chainConfs = configuration.getSubConfigurations(CONF_CHAINS);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException(
                    "Storage doesn't support sub configurations");
        } catch (NullPointerException e) {
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
        log.info("Constructed and ready");
    }

    @Override
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    protected void runMethod() {
        log.info("Activating filter pump(s) " +
                 (sequential ? "sequentially" : "in parallel"));
        
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
                    log.info("Filter chain " + pump.getChainName()
                             + " completed");
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
                    log.debug("Waiting for filter chain '" + pump.getChainName()
                              + "' to finish");
                    pump.waitForFinish();
                    log.info("Filter pump " + pump.getChainName()
                             + " completed");
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

    @Override
    public void waitForFinish(long timeout) throws InterruptedException {
        super.waitForFinish(timeout);

        for (FilterPump pump : pumps) {
            log.debug("Waiting for " + pump.getChainName());
            pump.waitForFinish(timeout);
        }
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

    /**
     * Run a FilterControl instance detected its configuration
     * via {@link Configuration#getSystemConfiguration(boolean true)}.
     * <p/>
     * The arguments to this method will be ignored.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(
                                              new LoggingExceptionHandler(log));

        Configuration conf = Configuration.getSystemConfiguration(true);


        FilterControl filterControl = new FilterControl(conf) {
                    protected void finishedCallback() {
                        switch (getStatus()) {
                            case error:
                                log.fatal("Crashed");
                                break;
                            case ready:
                            case stopping:
                            case stopped:
                                log.info("Stopped with status "
                                         + getStatus());
                                break;
                            default:
                                log.warn("Stopped with unknown state "
                                         + getStatus());
                        }
                    }
                };

        try {
            filterControl.start();
            filterControl.waitForFinish();
            log.info("Filter chain completed");
        } catch (Throwable t) {
            log.fatal("Caught toplevel exception: " + t.getMessage(), t);
            t.printStackTrace();
            System.exit(1);
        }

    }
}

