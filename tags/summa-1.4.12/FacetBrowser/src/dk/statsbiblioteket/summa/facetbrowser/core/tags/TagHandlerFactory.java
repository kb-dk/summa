/* $Id: TagHandlerFactory.java,v 1.9 2007/10/04 13:28:18 te Exp $
 * $Revision: 1.9 $
 * $Date: 2007/10/04 13:28:18 $
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
 * CVS:  $Id: TagHandlerFactory.java,v 1.9 2007/10/04 13:28:18 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.core.tags;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.Structure;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;


/**
 * A simple factory for TagHandlers. The purpose is to make it possible to
 * select TagHandlers based on properties.
 */
@QAInfo(state=QAInfo.State.IN_DEVELOPMENT)
public class TagHandlerFactory {
    private static Logger log = Logger.getLogger(TagHandlerFactory.class);

    /**
     * The class of the TagHandler to use.
     * <p><p>
     * Optional. Default is {@link TagHandlerImpl}.
     */
    public static final String CONF_TAG_HANDLER = "summa.facet.taghandler";
    public static final String DEFAULT_TAG_HANDLER =
            TagHandlerImpl.class.toString();

    /**
     * Construct a TagHandler. Note that the TagHandler does not open any
     * persistent state from storage as part of construction.
     * @param conf      setup for the TagHandler.
     * @param structure definition of Facets et al.
     * @param readOnly  if true, the TagHandler is opened as read only.
     * @return a TagHandler ready for use.
     * @throws RemoteException if the TagHandler could not be created.
     */
    public static TagHandler getTagHandler(Configuration conf,
                                           Structure structure,
                                           Boolean readOnly)
                                                        throws RemoteException {
        Class<? extends TagHandler> tagHandlerClass =
                Configuration.getClass(CONF_TAG_HANDLER, TagHandler.class,
                                       TagHandlerImpl.class, conf);
        log.debug(String.format(
                "Got TagHandler class %s. Creating instance", tagHandlerClass));

        Constructor<? extends TagHandler> con;
        try {
            con = tagHandlerClass.getConstructor(
                    Configuration.class, Structure.class, Boolean.class);
        } catch (NoSuchMethodException e) {
            throw new RemoteException(String.format(
                    "Constructor(Configuration, Structure, Boolean) not found "
                    + "in '%s'", tagHandlerClass), e);
        }
        Exception ex;
        try {
            return con.newInstance(conf, structure, readOnly);
        } catch (InstantiationException e) {
            ex = e;
        } catch (IllegalAccessException e) {
            ex = e;
        } catch (InvocationTargetException e) {
            ex = e;
        }
        //noinspection DuplicateStringLiteralInspection
        throw new RemoteException(String.format(
                "Could not instantiate '%s'", tagHandlerClass), ex);
    }

}



