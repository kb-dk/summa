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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Helper class to instantiate {@link IndexManipulator}s given a
 * {@link Configuration}
 */
public class ManipulatorFactory {

    private static final Log log = LogFactory.getLog(ManipulatorFactory.class);

    /**
     * Create a new {@link IndexManipulator} instance from the given
     * {@link Configuration}, {@code conf}. The class to instantiate will
     * be determined by the {@link IndexManipulator#CONF_MANIPULATOR_CLASS}
     * property which must be defined in {@code conf}.
     *
     * @param conf configuration object to read the
     *        {@link IndexManipulator#CONF_MANIPULATOR_CLASS} property from 
     * @return a newly instantiated IndexManipulator of the type configured
     *         in the supplied configuration
     * @throws Configurable.ConfigurationException there are problems reading
     *                                             the class from the
     *                                             configuration or on problems
     *                                             instantiating the given class
     */
    public static IndexManipulator createManipulator(Configuration conf) {
        log.trace("createManipulator called");

        Class<? extends IndexManipulator> manipulatorClass;
        try {
            manipulatorClass = Configuration.getClass(
                                        IndexManipulator.CONF_MANIPULATOR_CLASS,
                                        IndexManipulator.class,
                                        conf);
        } catch (Exception e) {
            throw new Configurable.ConfigurationException(
                                      "Could not get manipulator"
                                      + " class from property "
                                      + IndexManipulator.CONF_MANIPULATOR_CLASS
                                      + ": "
                                      + e.getMessage(), e);
        }

        log.debug("Instantiating manipulator class " + manipulatorClass);
        try {
            return Configuration.create(manipulatorClass, conf);
        } catch (Exception e) {
            throw new Configurable.ConfigurationException(
                                            "Failed to instantiate manipulator "
                                            + manipulatorClass + ": "
                                            + e.getMessage(),
                                            e);
        }
    }
}

