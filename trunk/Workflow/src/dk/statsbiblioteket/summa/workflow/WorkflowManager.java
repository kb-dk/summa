package dk.statsbiblioteket.summa.workflow;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;

/**
 * A generic helper class to instantiate and run a chain of
 * {@link WorkflowStep}s.
 * <p/>
 * Since this class is itself a {@code WorkflowStep} it is possible to
 * write nested workflows in the configuration passed to the topmost
 * workflow manager.
 */
public class WorkflowManager implements WorkflowStep {

    public WorkflowManager(Configuration conf) {
        // TODO: Extract a list of subconfs and instantiate children
    }

    public void run() {
        
    }
}
