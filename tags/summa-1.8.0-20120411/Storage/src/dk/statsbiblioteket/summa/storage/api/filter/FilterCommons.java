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

package dk.statsbiblioteket.summa.storage.api.filter;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionFactory;
import dk.statsbiblioteket.util.rpc.RMIConnectionFactory;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.summa.storage.api.Storage;
import dk.statsbiblioteket.summa.storage.api.rmi.RemoteStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for Storage-related filters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class FilterCommons {
    private static final Log log = LogFactory.getLog(FilterCommons.class);

    /* FIXME: Remove this hard coded hack to RMI */
    private static ConnectionFactory<? extends Storage> storageConnectionFactory =
            new RMIConnectionFactory<RemoteStorage>();
    private static ConnectionManager<Storage> accessConnectionManager =
            new ConnectionManager<Storage>(storageConnectionFactory);

    /**
     * Creates a ConnectionContext for the Storage specified in accessKey.
     * After use, resources are released with {@link #releaseAccess}.
     * @param conf contains the Storage address.
     * @param accessKey     the key for the Storage address. The Storage address
     *                      is an standard RMI address, such as
     *                      "//localhost:6789/storage".
     * @return a Connection Context to access.
     *         See {@link ConnectionContext} for usage.
     */
    public static synchronized ConnectionContext<Storage> getAccess(
                                         Configuration conf, String accessKey) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("getAccess(..., " + accessKey + ") called");
        String accessPoint;
        accessPoint = conf.getString(accessKey,
                                     "//localhost:28000/summa-storage");

        log.debug("Connecting to the access point '" + accessPoint + "'");
        return accessConnectionManager.get(accessPoint);
    }

    /**
     * Release the resources taken by the Storage-context. Call this when the
     * context aren't used anymore.
     * @param context the context for Storage.
     */
    public static synchronized void releaseAccess(
                                           ConnectionContext<Storage> context) {
        accessConnectionManager.release(context);
    }

    /**
     * Notify the underlying connection manager that the connection wrapped
     * by {code context} is broken.
     * @param context the connection context which is broken
     * @param t a throwable indicating the error cause
     */
    public static void reportError (ConnectionContext<Storage> context,
                                    Throwable t) {
        accessConnectionManager.reportError(context, t);
    }

    /**
     * Notify the underlying connection manager that the connection wrapped
     * by {code context} is broken.
     * @param context the connection context which is broken
     * @param msg a message indicating the error cause
     */
    public static void reportError (ConnectionContext<Storage> context,
                                    String msg) {
        accessConnectionManager.reportError(context, msg);
    }
}




