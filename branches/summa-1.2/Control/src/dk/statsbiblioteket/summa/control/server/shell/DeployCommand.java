package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.summa.control.api.ClientDeployer;
import dk.statsbiblioteket.summa.control.server.ControlUtils;
import dk.statsbiblioteket.summa.control.api.feedback.rmi.RemoteFeedback;
import dk.statsbiblioteket.summa.control.api.feedback.rmi.RemoteConsoleFeedback;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class DeployCommand extends Command {

    private Log log = LogFactory.getLog (DeployCommand.class);

    private ConnectionManager<ControlConnection> cm;
    private String controlAddress;
    private String hostname;

    public DeployCommand(ConnectionManager<ControlConnection> cm,
                         String controlAddress) {
        super("deploy", "Deploy a client bundle");

        setUsage("deploy [options] <bundle-id> <instance-id> <target-host>");

        installOption ("t", "transport", true,
                       "Which deployment transport to use. Allowed values are"
                       + " 'ssh'. Default is ssh");

        installOption ("b", "basepath", true,
                       "What basepath to use for the client installation "
                       + "relative to the client user's home directory. "
                       + "Default is 'summa-control'");

        installOption ("c", "configuration", true,
                       "Url, RMI address or file path where the client can"
                       + " find its configuration. Default points at the "
                       + "Control configuration server");

        this.cm = cm;
        this.controlAddress = controlAddress;
        hostname = RemoteHelper.getHostname();
    }

    public void invoke(ShellContext ctx) throws Exception {
        log.trace("invoke called");
        /* Extract and validate arguments */
        String[] args = getArguments();
        if (args.length != 3) {
            ctx.error("You must provide exactly 3 arguments. Found "
                      + args.length);
            return;
        }
        String bundleId = args[0];
        String instanceId = args[1];
        String target = args[2];

        log.trace("invoke called with bundleId '" + bundleId
                  + "', instanceId '"
                  + instanceId + "' and target '" + target + "'");
        String transport = getOption("t") != null ? getOption("t") : "ssh";
        transport = ControlUtils.getDeployerClassName(transport);

        String basePath =
                getOption("b") != null ? getOption("b") : "summa-control";
        String confLocation = getOption("c"); // This is allowed to be unset
                                    // - see ClientDeployer#CONF_CLIENT_CONF

        /* Set up a configuration for the deployment request */
        Configuration conf =
                Configuration.newMemoryBased(
                        ClientDeployer.CONF_BASEPATH,
                        basePath,
                        ClientDeployer.CONF_CLIENT_CONF,
                        confLocation,
                        ClientDeployer.CONF_DEPLOYER_BUNDLE,
                        bundleId,
                        ClientDeployer.CONF_INSTANCE_ID,
                        instanceId,
                        ClientDeployer.CONF_DEPLOYER_CLASS,
                        transport,
                        ClientDeployer.CONF_DEPLOYER_TARGET,
                        target,
                        ClientDeployer.CONF_DEPLOYER_FEEDBACK,
                "dk.statsbiblioteket.summa.control.api.feedback.rmi.RemoteFeedbackClient",
                        RemoteFeedback.CONF_REGISTRY_HOST,
                        hostname);

        log.trace ("Created deployment config:\n" + conf.dumpString());

        /* Connect to the Control and send the deployment request */
        ctx.prompt ("Deploying '" + instanceId + "' on '" + target + "' using "
                    + "transport '" + transport + "'... ");
        ConnectionContext<ControlConnection> connCtx = null;
        RemoteConsoleFeedback remoteConsole = null;
        try {
            log.trace("invoke: Creating remoteConsole");
            remoteConsole = Configuration.create(RemoteConsoleFeedback.class,
                                                 conf);
            log.trace("invoke: Getting connCtx for controlAddress '"
                      + controlAddress + "'");
            connCtx = cm.get (controlAddress);
            if (connCtx == null) {
                ctx.error ("Failed to connect to Control server at '"
                           + controlAddress + "'");
                return;
            }

            log.trace("invoke: Getting control connection");
            ControlConnection control = connCtx.getConnection();
            log.trace("invoke: Calling deployClient");
            control.deployClient(conf);
            ctx.info ("OK");
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



