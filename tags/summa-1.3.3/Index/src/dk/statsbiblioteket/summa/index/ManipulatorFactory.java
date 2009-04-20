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
