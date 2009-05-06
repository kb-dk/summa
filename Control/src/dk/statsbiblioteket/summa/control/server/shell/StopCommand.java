package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.ClientDeployer;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.InvalidClientStateException;
import dk.statsbiblioteket.summa.control.api.feedback.rmi.RemoteFeedback;
import dk.statsbiblioteket.summa.control.api.feedback.rmi.RemoteConsoleFeedback;
import dk.statsbiblioteket.summa.control.server.ControlUtils;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 *
 */
public class StopCommand extends Command {
    private Log log = LogFactory.getLog(StartCommand.class);

    private ConnectionManager<ControlConnection> cm;
    private String controlAddress;
    private String hostname;

    public StopCommand (ConnectionManager<ControlConnection> cm,
                         String controlAddress) {
        super ("stop", "Stop a client instance, halting its JVM and any " +
                       "running services");

        setUsage("stop <client-instance-id>");

        this.cm = cm;
        this.controlAddress = controlAddress;

        hostname = RemoteHelper.getHostname();
    }

    public void invoke(ShellContext ctx) throws Exception {
        /* Extract and validate arguments */
        log.debug("Invoking StopCommand");
        String[] args = getArguments();
        if (args.length != 1) {
            ctx.error("You must provide exactly 1 argument. Found "
                      + args.length);
            return;
        }

        String instanceId = args[0];

        if (!isClientRunning(instanceId)) {
            ctx.info("Client '" + instanceId + "' is not running");
            return;
        }

        ConnectionContext<ControlConnection> connCtx = null;

        try {
            log.trace("Stopping client '" + instanceId +"'");
            connCtx = cm.get(controlAddress);
            if (connCtx == null) {
                ctx.error("Failed to connect to Control server at '"
                           + controlAddress + "'");
                return;
            }

            ControlConnection control = connCtx.getConnection();
            control.stopClient(instanceId);
            log.trace("Client '"+instanceId+"' stopped");
            ctx.info("OK");
        } finally {
            if (connCtx != null) {
                cm.release (connCtx);
            }
        }

    }

    private boolean isClientRunning(String instanceId) {
        ConnectionContext<ControlConnection> connCtx = cm.get(controlAddress);
        ControlConnection control = connCtx.getConnection();
        try {
            ClientConnection client = control.getClient(instanceId);

            if (client != null) {
                log.debug("Client is already running");
                return true;
            }
        } catch (InvalidClientStateException e){
            return false;
        } catch (IOException e) {
            throw new InvalidClientStateException(instanceId,
                                                  "Connection to '" + instanceId
                                                  + "' is broken: "
                                                  + e.getMessage(), e);
        } finally {
            connCtx.unref();
        }
        return false;
    }
}
