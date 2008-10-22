package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * Main class for running workflows. It will instantiate a
 * {@link WorkflowManager} based on a configuration obtained from
 * {@link Configuration#getSystemConfiguration(boolean)}
 * with {@code allowUnset=true}
 */
public class Main {

    public static void main (String[] args) {
        Configuration conf = Configuration.getSystemConfiguration(true);

        WorkflowStep core = new WorkflowManager(conf);
        core.run();
    }

}
