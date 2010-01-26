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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A generic helper class to instantiate and run a chain of
 * {@link WorkflowStep}s.
 * <p/>
 * Since this class is itself a {@code WorkflowStep} it is possible to
 * write nested workflows in the configuration passed to the topmost
 * workflow manager.
 */
public class WorkflowManager implements WorkflowStep {

    /**
     * Property defining whether exceptions from workflow steps should cause
     * a re-run of the workflow or if it should abort the workflow
     * (cascading the exception upwards). Default value is
     * {@link #DEFAULT_FAILURE_TOLERANT}.
     */
    public static final String CONF_FAILURE_TOLERANT =
                                               "summa.workflow.failuretolerant";

    /**
     * Default value for {@link #CONF_FAILURE_TOLERANT}
     */
    public static final boolean DEFAULT_FAILURE_TOLERANT = false;

    /**
     * Property defining whether the workflow should be looped continously.
     * Default value is {@link #DEFAULT_LOOP}
     */
    public static final String CONF_LOOP = "summa.workflow.loop";

    /**
     * Default value for the {@link #CONF_LOOP} property
     */
    public static final boolean DEFAULT_LOOP = false;

    /**
     * Property defining the amount of seconds to sleep before retrying a failed
     * workflow. This property is only used if {@link #CONF_FAILURE_TOLERANT}
     * is set to {@code true}. The default value of this property is
     * {@link #DEFAULT_FAILURE_GRACE_TIME}
     */
    public static final String CONF_FAILURE_GRACE_TIME =
                                              "summa.workflow.failuregracetime";

    /**
     * Default value for the {@link #CONF_FAILURE_GRACE_TIME} property
     */
    public static final int DEFAULT_FAILURE_GRACE_TIME = 60;

    /**
     * Property containing a list of sub configurations, one for each
     * workflow step to configure. This property <i>must</i> be defined if the
     * {@link #WorkflowManager(Configuration)} constructor is used. If not
     * a {@link ConfigurationException} will be thrown.
     * <p/>
     * Each sub configuration <i>must</i> define the {@link #CONF_STEP_CLASS}
     * property. The sub configuration will be passed as-is to the relevant
     * {@link WorkflowStep} subclass defined in {@link #CONF_STEP_CLASS}
     */
    public static final String CONF_STEPS = "summa.workflow.steps";

    /**
     * Property that <i>must</i> be defined in each sub configuration listed in
     * {@link #CONF_STEPS}. This value contains the fully qualified class name
     * for the {@link WorkflowStep} subclass to instantiate for this step in
     * the workflow
     */
    public static final String CONF_STEP_CLASS = "summa.workflow.step.class";

    private List<WorkflowStep> steps;
    private Log log;
    private boolean loop;
    private boolean failureTolerant;
    private int graceTime;

    public WorkflowManager (boolean loop, boolean failureTolerant,
                            int graceTime) {
        log = LogFactory.getLog(this.getClass().getName());
        log.debug("Creating WorkflowManager");

        steps = new ArrayList<WorkflowStep>();

        this.loop = loop;
        log.debug("Loop workflow: " + loop);

        this.failureTolerant = failureTolerant;
        log.debug("Failure tolerant workflow: " + failureTolerant);

        if (failureTolerant) {
            this.graceTime = graceTime;
            log.debug("Failure gracetime " + graceTime + "s");
        }

        log.debug("WorkflowManager ready");
    }

    public WorkflowManager(Configuration conf) {
        log = LogFactory.getLog(this.getClass().getName());
        log.debug("Creating WorkflowManager");

        steps = new ArrayList<WorkflowStep>();

        List<Configuration> stepConfs;

        try {
            stepConfs = conf.getSubConfigurations(CONF_STEPS);
        } catch (IOException e) {
            throw new ConfigurationException("Sub configurations not supported "
                                             + "by configuration instance, or "
                                             + "key " + CONF_STEPS
                                             + " not defined");
        }

        int subConfNum = 0;
        for (Configuration stepConf : stepConfs) {
            Class<? extends WorkflowStep> stepClass;

            try {
                stepClass = Configuration.getClass(CONF_STEP_CLASS,
                                                   WorkflowStep.class,
                                                   stepConf);
            } catch (NullPointerException e) {
                throw new ConfigurationException("Missing " + CONF_STEP_CLASS
                                                 + " in sub configuration "
                                                 + "number " + subConfNum);
            }
            log.debug("Configuring workflow step "
                      + subConfNum + " for " + stepClass.getName());

            WorkflowStep step = Configuration.create(stepClass, stepConf);
            steps.add(step);

            subConfNum++;
        }

        loop = conf.getBoolean(CONF_LOOP, DEFAULT_LOOP);
        log.debug("Loop workflow: " + loop);

        failureTolerant = conf.getBoolean(CONF_FAILURE_TOLERANT, DEFAULT_FAILURE_TOLERANT);
        log.debug("Failure tolerant workflow: " + failureTolerant);

        if (failureTolerant) {
            graceTime = conf.getInt(CONF_FAILURE_GRACE_TIME,
                                    DEFAULT_FAILURE_GRACE_TIME);
            log.debug("Failure gracetime " + graceTime + "s");
        }

        log.debug("WorkflowManager ready");
    }

    public void addStep(WorkflowStep step) {
        log.debug("Adding step " + step.getClass().getName());
        steps.add(step);
    }

    public void run() {
        log.info("Running workflow");

        boolean inRetry = false;

        do {
            inRetry = false;
            int num = 0;

            for (WorkflowStep step : steps) {
                log.info("Running step " + num + ": "
                         + step.getClass().getName());

                if (failureTolerant) {
                    /* Try running the step and sleep for graceTime before
                     * restarting the workflow on errors. If interrupted
                     * during sleep, abort the workflow */
                    try {
                        step.run();
                    } catch (Exception e) {
                        log.error("Workflow step " + num + ", "
                                  + step.getClass().getName() + ", failed. "
                                  + "Restarting workflow in "
                                  + graceTime + "s");

                        /* Schedule us for retrying */
                        inRetry = true;

                        try {
                            Thread.sleep(graceTime*1000);
                        } catch (InterruptedException e1) {
                            log.warn("Interruped during failure recovery "
                                     + "gracetime. Aborting workflow loop");
                            inRetry = false;
                        }

                        break;
                    }
                } else {
                    /* Workflow not failure tolerant. Just hit it */
                    step.run();
                }
                num++;
            }

            if (loop) {
                log.info("Workflow iteration complete. Looping");
            }
        } while (loop || inRetry);

        log.info("Workflow complete");
    }
}

