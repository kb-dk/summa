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
package dk.statsbiblioteket.summa.facetbrowser.build;

import java.rmi.RemoteException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.map.CoreMap;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.lucene.LuceneFacetBuilder;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.log4j.Logger;

/**
 * Creates a Builder based on the given arguments.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class BuilderFactory {
    private static Logger log = Logger.getLogger(BuilderFactory.class);

    /**
     * The Builder-implementating class that {@link #getBuilder} returns.
     * </p><p>
     * Optional. Default is {@link LuceneFacetBuilder}.
     */
    public static final String CONF_BUILDER_CLASS =
            "summa.facet.builder.class";
    public static final Class<? extends Builder> DEFAULT_CLASS =
            LuceneFacetBuilder.class;

    public static Builder getBuilder(Configuration conf, Structure structure,
                                     CoreMap coreMap, TagHandler tagHandler)
                                                        throws RemoteException {
        Class<? extends Builder> builderClass =
                Configuration.getClass(CONF_BUILDER_CLASS, Builder.class,
                                       DEFAULT_CLASS, conf);
        log.debug(String.format(
                "Got Builder class %s. Creating instance", builderClass));

        Constructor<? extends Builder> con;
        try {
            con = builderClass.getConstructor(
                    Configuration.class, Structure.class, CoreMap.class,
                    TagHandler.class);
        } catch (NoSuchMethodException e) {
            throw new RemoteException(String.format(
                    "Constructor(Configuration, Structure, CoreMap, TagHandler)"
                    + " not found in '%s'", builderClass), e);
        }
        Exception ex;
        try {
            return con.newInstance(conf, structure, coreMap, tagHandler);
        } catch (InstantiationException e) {
            ex = e;
        } catch (IllegalAccessException e) {
            ex = e;
        } catch (InvocationTargetException e) {
            ex = e;
        }
        //noinspection DuplicateStringLiteralInspection
        throw new RemoteException(String.format(
                "Could not instantiate '%s'", builderClass), ex);
    }

}




