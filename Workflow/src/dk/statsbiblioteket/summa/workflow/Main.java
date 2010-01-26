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
package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.XStorage;

import java.io.IOException;

/**
 * Main class for running workflows. It will instantiate a
 * {@link WorkflowManager} based on a configuration obtained from:
 * <ul>
 *   <li>The system property {@link Configuration#CONF_CONFIGURATION_PROPERTY}, or</li>
 *   <li>The first command line argument (only one argument allowed). A workflow
 *       defined this way may be specified as described in
 *       {@link Configuration#load}</li>
 * </ul>
 * If a workflow is specified both ways the program will exit with an error.
 */
public class Main {

    public static final String CONF_CONFIG =
                                      Configuration.CONF_CONFIGURATION_PROPERTY;

    public static void main (String[] args) {
        if (args.length > 1) {
            System.err.println("Too many arguments. Only one or zero arguments "
                               + "allowed. Bailing out");
            System.exit(1);
        }

        Configuration conf = null;

        if (args.length == 1) {
            if (System.getProperty(CONF_CONFIG) != null) {
                System.err.println("Both system property "
                                   + CONF_CONFIG
                                   + " and command line argument present. "
                                   + "Bailing out");
                System.exit(3);
            }

            try {
                conf = Configuration.load(args[0]);
            } catch (Exception e) {
                System.err.println("Error reading workflow '" + args[0] + "': "
                                   +e.getMessage() +". Bailing out");
                System.exit(2);
            }
        } else {
            if (System.getProperty(CONF_CONFIG) == null) {
                System.err.println("System property " + CONF_CONFIG + " must be"
                                   + " set when running summa-workflow without "
                                   + "arguments.");
                System.exit(4);
            }

            conf  = Configuration.getSystemConfiguration();
        }

        WorkflowStep core = new WorkflowManager(conf);
        core.run();
    }

}

