package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.feedback.RemoteConsoleFeedback;
import dk.statsbiblioteket.summa.control.feedback.RemoteFeedback;
import dk.statsbiblioteket.summa.control.server.ClientDeployer;
import dk.statsbiblioteket.summa.control.server.ControlUtils;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Control shell command for starting a Client.
 */
public class StartCommand extends Command {
    private Log log = LogFactory.getLog(StartCommand.class);

    private ConnectionManager<ControlConnection> cm;
    private String controlAddress;
    private String hostname;

    public StartCommand (ConnectionManager<ControlConnection> cm,
                         String controlAddress) {
        super ("start", "Start a client instance");

        setUsage("start <client-instance-id>");

        installOption ("t", "transport", true, "Which deployment transport to"
                                             + " use. Allowed values are 'ssh'."
                                             + "Default is ssh");

        installOption ("c", "configuration", true,
                       "Url, RMI address or file path where the client can"
                       + " find its configuration. Default points at the"
                       + " Control configuration server");

        this.cm = cm;
        this.controlAddress = controlAddress;

        hostname = RemoteHelper.getHostname();
    }

    public void invoke(ShellContext ctx) throws Exception {
        /* Extract and validate arguments */
        log.debug("Invoking StartCommand");
        String[] args = getArguments();
        if (args.length != 1) {
            ctx.error("You must provide exactly 1 argument. Found "
                      + args.length);
            return;
        }

        String instanceId = args[0];

        String transport = getOption("t") != null ? getOption("t") : "ssh";
        transport = ControlUtils.getDeployerClassName(transport);

        String confLocation = getOption("c"); // This is allowed to be unset
                                              // - see ClientDeployer#CLIENT_CONF_PROPERTY

        /* Set up a configuration for the startClient request */
        Configuration conf =
                Configuration.newMemoryBased(
                        ClientDeployer.CLIENT_CONF_PROPERTY,
                        confLocation,
                        ClientDeployer.INSTANCE_ID_PROPERTY,
                        instanceId,
                        ClientDeployer.DEPLOYER_CLASS_PROPERTY,
                        transport,
                        ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY,
                        "dk.statsbiblioteket.summa.control.feedback.RemoteFeedbackClient",
                        RemoteFeedback.REGISTRY_HOST_PROPERTY,
                        hostname);

        log.trace("Configuration initialized");
        /* Connect to the Control and send the deployment request */
        ctx.prompt ("Starting client '" + instanceId + "' using "
                    + "transport '" + transport + "'... ");
        ConnectionContext<ControlConnection> connCtx = null;
        RemoteConsoleFeedback remoteConsole = null;
        try {
            log.trace("Creating remote console");
            remoteConsole = Configuration.create(RemoteConsoleFeedback.class,
                                                 conf);
            log.trace("Calling get on ConnectionManager with " + controlAddress);
            connCtx = cm.get(controlAddress);
            if (connCtx == null) {
                ctx.error("Failed to connect to Control server at '"
                           + controlAddress + "'");
                return;
            }
            log.trace("Calling getConnection on connCtx");
            ControlConnection control = connCtx.getConnection();
            log.trace("Starting client");
            control.startClient(conf);
            log.trace("All OK");
            ctx.info("OK");
        } finally {
            if (remoteConsole != null) {
                remoteConsole.close();
            }
            if (connCtx != null) {
                cm.release (connCtx);
            }
        }

    }
}
