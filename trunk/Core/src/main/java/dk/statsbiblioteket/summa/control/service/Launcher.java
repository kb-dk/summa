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
package dk.statsbiblioteket.summa.control.service;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.LoggingExceptionHandler;
import dk.statsbiblioteket.summa.common.util.MachineStats;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Simple class used to help launching a {@link Service}.</p>
 *
 * <p>The service class to instantiate will be read from the
 * system configuration's {@code control.launcher.service.class} property</p>
 *
 * <p>The typical usage is to specify this class as the main class
 * in the bundle specs.</p>
 * 
 * @see Configuration#getSystemConfiguration()
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_OK,
        author = "mke")
public class Launcher {
    /** The Control service class. Default value is
     * {@link #DEFAULT_SERVICE_CLASS}. */
    public static final String CONF_SERVICE_CLASS =
            "control.launcher.service.class";
    /** Default value for {@link #CONF_SERVICE_CLASS}. */
    public static final Class<? extends Service> DEFAULT_SERVICE_CLASS =
                                                                  Service.class;

    private static MachineStats stats;

    /**
     * Main method for launching a service.
     * @param args Commandline arguments, these are not used.
     */
    public static void main(String[] args) {
        Log log = LogFactory.getLog(Launcher.class);
        Thread.setDefaultUncaughtExceptionHandler(new LoggingExceptionHandler(
                log));

        log.debug("Getting system configuration ");
        log.debug("SecurityManager: " + System.getSecurityManager());

        try {
            Configuration conf = Configuration.getSystemConfiguration(true);

            log.trace ("Got system configuration");

            Class<? extends Service> serviceClass = null;
            try {
                serviceClass = Configuration.getClass(
                        CONF_SERVICE_CLASS, DEFAULT_SERVICE_CLASS, conf);
            } catch (NullPointerException e) {
                log.fatal(String.format(
                        "Property '%s' not defined in configuration. Config "
                      + "was:\n\n%s", CONF_SERVICE_CLASS, conf.dumpString()));
                System.exit(2);
            }

            try {
                stats = new MachineStats(conf, "Launcher");
            } catch (Exception e) {
                log.warn("Failed to create machine stats. Not critical, but "
                         + "memory stats will not be logged", e);
            }

            log.debug("Using service class " + CONF_SERVICE_CLASS
                      + " = " + serviceClass);

            Configuration.create(serviceClass, conf);
            log.debug("Created service. The launch has completed.");
        } catch (Throwable t) {
            String message = "Service launcher caught top level exception. "
                               + "Bailing out: " + t.getMessage();
            log.fatal(message, t);
            System.err.println(message);
            System.exit (1);
        }
    }
}