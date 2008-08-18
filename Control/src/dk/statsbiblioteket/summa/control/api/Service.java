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
package dk.statsbiblioteket.summa.control.api;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Management interface exposed by ClientManager services. A {@code Service} is managed
 * by a {@link Client}. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface Service extends Configurable, Remote, Monitorable {

    /** <p>Property defining the id under which the service should report itself
     * via {@link #getId}. </p>
     *
     * <p>The service's RMI service will also run under this name.</p>
     * */
    public static final String SERVICE_ID = "summa.control.service.id";

    /**
     * Property defining the relative path under which the service
     * should install itself. The path is relative to the system property
     * {@code user.home} of the service's jvm<br/>
     */
    public static final String SERVICE_BASEPATH = "summa.control.service.basepath";

    /**
     * Property defining the port on which the service's rmi service should
     * communicate. Default should be 28003.
     */
    public static final String SERVICE_PORT = "summa.control.service.port";

    /**
     * Property defining the port on which the service should contact or create
     * an rmi registry. Default should be 27000. */
    public static final String REGISTRY_PORT = "summa.control.service.registry.port";

    /**
     * Start the service. All relevant properties should be read from the
     * configuration here, rather than in the constructor, as they can be
     * changed between {@link #stop()} and {@link #start()}.
     *
     * @throws RemoteException if there is an error communicating with the
     *                         service.
     */
    public void start() throws RemoteException;
// TODO: JUnit-test changes to configuration between stop and start
// FIXME : Do we need a way to tell the service's JVM to exit
    /**
     * <p>Stop the service. After the service has stopped, calling {@link #start()}
     * should reinitialise the service, based on the configuration.<p>
     *
     * <p>Calling stop on a service should result in it closing all connections
     * and file handles. The RMI connection exposing this interface may be
     * dropped too in which case the service must close its JVM.</p>
     *
     * <p>Generally stand alone services should close the JVM all together
     * to make sure the service can release all resources to the system.
     * Services running in application containers should naturally not
     * close the JVM and should enter the {@link Status.CODE#stopped} state
     * instead.</p>
     *
     * @throws RemoteException if there is an error communicating with the
     *                         service.
     */
    public void stop() throws RemoteException;

    /**
     * @return the status for this service.
     * @throws RemoteException if there is an error communicating with the
     *                         service.
     */
    public Status getStatus() throws RemoteException;

    /**
     * Introspect the unique id of the service as assigned by its parent
     * {@link Client}
     * @return the id
     * @throws RemoteException if there is an error communicating with the
     *                         service.
     */
    public String getId() throws RemoteException;
}
