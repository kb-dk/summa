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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.search.api.SummaSearcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class used to instantiate the correct {@link SummaSearcher}
 * given a {@link Configuration}
 */
public class SummaSearcherFactory {
    private static final Log log = LogFactory.getLog (SummaSearcherFactory.class);

    /**
     * Create a {@link SummaSearcher} instance of the class specified by the
     * {@link SummaSearcher#CONF_CLASS} in {@code conf}. If this property
     * is not defined the method will default to a {@link SummaSearcherImpl}
     *
     * @param conf the configuration used to look up
     *             {@link SummaSearcher#CONF_CLASS}
     * @param defaultClass the class to instantiate if
     *                     {@link SummaSearcher#CONF_CLASS} is not set in
     *                     {@code conf}
     * @return a newly created {@code SummaSearcher}
     * @throws Configurable.ConfigurationException if there is an error reading
     *         {@code conf} or there is an error creating the searcher instance.
     */
    public static SummaSearcher createSearcher (
            Configuration conf, Class<? extends SummaSearcher> defaultClass) {
        log.trace("createSeacher called");

        Class<? extends SummaSearcher> seacherClass;
        try {
            seacherClass = conf.getClass(
                    SummaSearcher.CONF_CLASS, SummaSearcher.class,
                    defaultClass);
        } catch (Exception e) {
            throw new Configurable.ConfigurationException(String.format(
                    "Could not get searcher class from property %s",
                    SummaSearcher.CONF_CLASS), e);
        }
        
        log.debug("Instantiating searcher class: " + seacherClass);

        try {
            return Configuration.create(seacherClass, conf);
        } catch (Exception e) {
            throw new Configurable.ConfigurationException(String.format(
                    "Failed to instantiate seacher class %s", seacherClass), e);
        }
    }

    /**
     * Call {@link #createSearcher(Configuration, Class)} with the default class
     * set to {@link SummaSearcherImpl}.
     *
     * @param conf The configuration to use
     * @return newly instantiated searcher
     */
    public static SummaSearcher createSearcher (Configuration conf) {
        return createSearcher (conf, SummaSearcherImpl.class);
    }
}




