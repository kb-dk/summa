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

import java.util.List;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class HubFactory implements Configurable {
    private static Log log = LogFactory.getLog(HubFactory.class);

    /**
     * The class for the component at this point in the HubComponent tree.
     * </p><p>
     * Mandatory.
     */
    public static final String CONF_COMPONENT = "hubfactory.componentclass";

    /**
     * A list of sub-configurations for components that should be children of the component at this point in the
     * HubComponent tree.
     * </p><p>
     * Optional. Default is none.
     */
    public static final String CONF_SUB = "hubfactory.subcomponents";

    public static HubComponent createComponent(Configuration conf) {
        if (!conf.containsKey(CONF_COMPONENT)) {
            throw new IllegalArgumentException("The property " + CONF_COMPONENT + " must be defined");
        }
        Class<? extends HubComponent> componentClass = Configuration.getClass(CONF_COMPONENT, HubComponent.class, conf);
        log.debug("Creating HubComponent of class " + componentClass.toString());
        HubComponent component = Configuration.create(componentClass, conf);
        log.debug("Created " + component);
        if (conf.containsKey(CONF_SUB)) {
            if (!(component instanceof HubComposite)) {
                throw new IllegalStateException(
                        "Sub-components are specified with " + CONF_SUB + " but the current component " + component
                        + " is not a HubComposite");
            }
            HubComposite composite = (HubComposite)component;
            // TODO: Can we trick this so it accepts both a list and a direct sub configuration?
            List<Configuration> subs = conf.getSubConfigurations(CONF_SUB);
            for (Configuration sub: subs) {
                 composite.addComponent(createComponent(sub));
            }
        }
        return component;
    }
}
