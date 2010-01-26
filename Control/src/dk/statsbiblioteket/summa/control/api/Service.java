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
     */
    public static final String CONF_SERVICE_ID = "summa.control.service.id";

    /**
     * Property defining the relative path under which the service
     * should install itself. The path is relative to the system property
     * {@code user.home} of the service's jvm<br/>
     */
    public static final String CONF_SERVICE_BASEPATH = "summa.control.service.basepath";

    /**
     * Property defining the port on which the service's rmi service should
     * communicate. Default should be 28003.
     */
    public static final String CONF_SERVICE_PORT = "summa.control.service.port";

    /**
     * Property defining the port on which the service should contact or create
     * an rmi registry. Default should be 27000. */
    public static final String CONF_REGISTRY_PORT = "summa.control.service.registry.port";

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
     * and file handles. <i>The RMI connection exposing this interface may be
     * dropped too in which case the service must close its JVM.</i></p>
     *
     * @see #kill
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

    /**
     * Exit the JVM of the service. If the service has not been stopped
     * the service should try to shut itself down cleanly before
     * exiting
     * @throws RemoteException if there is an error communicating with the
     *                         service.
     */
    public void kill() throws RemoteException;
}




