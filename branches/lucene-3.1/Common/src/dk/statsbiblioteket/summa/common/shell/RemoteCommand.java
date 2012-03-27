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
package dk.statsbiblioteket.summa.common.shell;

import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.net.SocketException;

/**
 * Abstract helper class to facilitate command implementations that need
 * a connection to a remote RPC service to work.
 */
abstract public class RemoteCommand<E> extends Command {

    protected ConnectionManager<? extends E> connMgr;
    private ConnectionContext<? extends E> connCtx;

    public RemoteCommand (String name, String description,
                          ConnectionManager<? extends E> connMgr) {
        super (name, description);
        this.connMgr = connMgr;
    }

    protected synchronized E getConnection (String connectionId) {

        if (connCtx == null) {
            connCtx = connMgr.get(connectionId);
        }

        return connCtx.getConnection();
    }

    protected synchronized void releaseConnection() {
        if (connCtx == null) {
            return;
        }

        connCtx.unref();
        connCtx = null;
    }

    protected synchronized void connectionError(Throwable t) {
        if (connCtx != null) {
            connMgr.reportError(connCtx, t);
        }
        connCtx = null;
    }

    protected synchronized void connectionError(String msg) {
        if (connCtx != null) {
            connMgr.reportError(connCtx, msg);
        }
        connCtx = null;
    }

}




