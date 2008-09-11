/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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



