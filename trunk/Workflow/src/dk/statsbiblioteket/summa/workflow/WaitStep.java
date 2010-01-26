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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A {@link WorkflowStep} that simply blocks for a configuable amount of time
 * before returning.
 */
public class WaitStep implements WorkflowStep {

    /**
     * Number of seconds to block. Default is {@link #DEFAULT_TIME}
     */
    public static final String CONF_TIME = "summa.workflow.step.wait.time";

    /**
     * Default number of seconds to block
     */
    public static final int DEFAULT_TIME = 60;

    private Log log;
    private int blockTime;

    /**
     * Create a new {@code WaitStep} blocking {@code blockTime} seconds
     * @param blockTime number of seconds to block
     */
    public WaitStep (int blockTime) {
        log = LogFactory.getLog(this.getClass().getName());

        log.debug("Block time " + blockTime + "s");

        this.blockTime = blockTime;
    }

    public WaitStep (Configuration conf) {
        this(conf.getInt(CONF_TIME, DEFAULT_TIME));
    }

    public void run() {
        log.debug("Blocking for " + blockTime + "s");

        try {
            Thread.sleep(blockTime*1000);
        } catch (InterruptedException e) {
            log.warn("Interrupted while blocking");
        }
    }
}

