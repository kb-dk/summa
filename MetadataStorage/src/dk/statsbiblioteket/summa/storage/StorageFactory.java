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

import java.io.IOException;
import java.rmi.RemoteException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
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

    private static final String DEFAULT_CONTROLLER =
            ControlDerby.class.toString();

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
/*        log.trace("createController created");
        String controllerName;
        try {
            if (configuration.valueExists(PROP_CONTROLLER)) {
                controllerName = configuration.getString(PROP_CONTROLLER);
            } else {
                log.info("No controller specified for createController. "
                         + "Defaulting to " + DEFAULT_CONTROLLER);
                controllerName = DEFAULT_CONTROLLER;
                //noinspection DuplicateStringLiteralInspection,ThrowCaughtLocally
                throw new RemoteException("The property " + PROP_CONTROLLER
                                          + " does not exist in the "
                                          + "properties");
            }
        } catch (RemoteException e) {
            throw new RemoteException("Remote exception requesting property "
                                      + PROP_CONTROLLER, e);
        } catch (IOException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new RemoteException("Exception requesting property "
                                      + PROP_CONTROLLER, e);
        }

        log.debug("Attempting to create controller " + controllerName);
        return configuration.
        if (!Control.class.isAssignableFrom(controllerName)) {
            throw new IllegalArgumentException("Class " + configurable
                                               + " is not a Configurable");
        }

        try {
            Constructor<T> con = configurable.getConstructor(Configuration.class);
            return con.newInstance(this);

        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException ("The class " + configurable.getSimpleName()
                                                        + " does not have a constructor taking a Configuration"
                                                        + " as its sole argument", e);
        } catch (IllegalAccessException e) {
            throw new Configurable.ConfigurationException(e);
        } catch (InvocationTargetException e) {
            throw new Configurable.ConfigurationException(e);
        } catch (InstantiationException e) {
            throw new Configurable.ConfigurationException(e);
        }
  */
        return null;
    }

}
