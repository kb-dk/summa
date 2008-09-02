/* $Id: CoreMapFactory.java,v 1.5 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/05 10:20:22 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: CoreMapFactory.java,v 1.5 2007/10/05 10:20:22 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.map;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandler;
import dk.statsbiblioteket.summa.facetbrowser.core.tags.TagHandlerImpl;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CoreMapFactory {
    private static Logger log = Logger.getLogger(CoreMapFactory.class);

    /**
     * The CoreMap-implementating class that {@link #getCoreMap} returns.
     * </p><p>
     * Optional. Default is {@link CoreMapBitStuffed}.
     */
    public static final String CONF_COREMAP_CLASS =
            "summa.facet.core-map.class";
    public static final Class<? extends CoreMap> DEFAULT_CLASS =
            CoreMapBitStuffed.class;

    public static CoreMap getCoreMap(Configuration conf, Structure structure)
                                                        throws RemoteException {
        Class<? extends CoreMap> coreMapClass =
                Configuration.getClass(CONF_COREMAP_CLASS, CoreMap.class,
                                       DEFAULT_CLASS, conf);
        log.debug(String.format(
                "Got CoreMap class %s. Creating instance", coreMapClass));

        Constructor<? extends CoreMap> con;
        try {
            con = coreMapClass.getConstructor(
                    Configuration.class, Structure.class);
        } catch (NoSuchMethodException e) {
            throw new RemoteException(String.format(
                    "Constructor(Configuration, Structure) not found "
                    + "in '%s'", coreMapClass), e);
        }
        Exception ex;
        try {
            return con.newInstance(conf);
        } catch (InstantiationException e) {
            ex = e;
        } catch (IllegalAccessException e) {
            ex = e;
        } catch (InvocationTargetException e) {
            ex = e;
        }
        //noinspection DuplicateStringLiteralInspection
        throw new RemoteException(String.format(
                "Could not instantiate '%s'", coreMapClass), ex);
    }


}
