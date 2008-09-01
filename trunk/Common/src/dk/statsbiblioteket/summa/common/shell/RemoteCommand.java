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
