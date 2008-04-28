/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.storage;

import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.storage.database.derby.ControlDerby;
import dk.statsbiblioteket.summa.storage.io.Control;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * te forgot to document this class.
 */
public class StorageFactory {
    private static Log log = LogFactory.getLog(StorageFactory.class);

    /**
     * The fully classified class name for the wanted Controller.
     */
    public static final String PROP_CONTROLLER = "summa.storage.controller";

    private static final Class<? extends Control> DEFAULT_CONTROLLER =
            ControlDerby.class;

    /**
     * Construct a metadata storage controller based on the given properties.
     * The properties are also passed to the constructor for the controller.
     * @param configuration setup for the wanted controller along with the
     *        property {@link #PROP_CONTROLLER} which should hold the class-name
     *        for the wanted DatabaseControl. If no controller is specified,
     *        the StorageFactory defaults to {@link ControlDerby}.
     * @return a metadata storage controller.
     * @throws RemoteException if the controller could not be created.
     */
    public static Control createController(Configuration configuration) throws
                                                               RemoteException {
        log.trace("createController called");

        Class<? extends Control> controllerClass;
        try {
            controllerClass = configuration.getClass(PROP_CONTROLLER,
                                                     Control.class,
                                                     DEFAULT_CONTROLLER);
        } catch (Exception e) {
            throw new RemoteException("Could not get metadata storage control"
                                      + " class from property "
                                      + PROP_CONTROLLER, e);
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Got controller class " + controllerClass
                  + ". Commencing creation");
        try {
            // FIXME: This forces a RMI call when packing as a service. Not good 
            return Configuration.create(controllerClass, configuration);
//            return Configuration.newMemoryBased().create(controllerClass);
        } catch (Exception e) {
            throw new RemoteException("Could not create controller class "
                                      + controllerClass, e);
        }
    }

}
