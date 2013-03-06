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
package dk.statsbiblioteket.summa.support.harmonise.hub.core;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class HubComponentImpl implements HubComponent, Configurable {
    private static Log log = LogFactory.getLog(HubComponentImpl.class);

    /**
     * The ID for this node.
     * </p><p>
     * Highly recommended. Default is class name.
     */
    public static final String CONF_ID = "node.id";

    private final String id;

    public HubComponentImpl(Configuration conf) {
        this.id = conf.getString(CONF_ID, getClass().getSimpleName());
        log.info("Created " + toString());
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public String toString() {
        return "HubComponentImpl(id='" + getID()+ ')';
    }
}
