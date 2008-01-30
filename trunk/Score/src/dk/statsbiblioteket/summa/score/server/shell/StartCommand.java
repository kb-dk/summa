package dk.statsbiblioteket.summa.score.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.score.api.ScoreConnection;
import dk.statsbiblioteket.summa.score.server.ScoreUtils;
import dk.statsbiblioteket.summa.score.server.ClientDeployer;
import dk.statsbiblioteket.summa.score.feedback.RemoteFeedback;
import dk.statsbiblioteket.summa.score.feedback.RemoteConsoleFeedback;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

/**
 * Score shell command for starting a Client.
 */
public class StartCommand extends Command {

    private ConnectionManager<ScoreConnection> cm;
    private String scoreAddress;
    private String hostname;

    public StartCommand (ConnectionManager<ScoreConnection> cm,
                         String scoreAddress) {
        super ("start", "Start a client instance");

        setUsage("start <client-instance-id>");

        installOption ("t", "transport", true, "Which deployment transport to"
                                             + " use. Allowed values are 'ssh'."
                                             + "Default is ssh");

        installOption ("c", "configuration", true, "Url, RMI address or file"
                                                 + " path where the client can"
                                                 + " find its configuration."
                                                 + " Default points at the "
                                                 + "Score configuration server");

        this.cm = cm;
        this.scoreAddress = scoreAddress;

        hostname = RemoteHelper.getHostname();

    }

    public void invoke(ShellContext ctx) throws Exception {
        /* Extract and validate arguments */
        String[] args = getArguments();
        if (args.length != 1) {
            ctx.error("You must provide exactly 1 argument. Found " + args.length);
            return;
        }

        String instanceId = args[0];

        String transport = getOption("t") != null ? getOption("t") : "ssh";
        transport = ScoreUtils.getDeployerClassName(transport);

        String confLocation = getOption("c"); // This is allowed to be unset
                                              // - see ClientDeployer#CLIENT_CONF_PROPERTY

        /* Set up a configuration for the startClient request */
        Configuration conf =
                Configuration.newMemoryBased(ClientDeployer.CLIENT_CONF_PROPERTY,
                                             confLocation,
                                             ClientDeployer.INSTANCE_ID_PROPERTY,
                                             instanceId,
                                             ClientDeployer.DEPLOYER_CLASS_PROPERTY,
                                             transport,
                                             ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY,
                                             "dk.statsbiblioteket.summa.score.feedback.RemoteFeedbackClient",
                                             RemoteFeedback.REGISTRY_HOST_PROPERTY,
                                             hostname);

        /* Connect to the Score and send the deployment request */
        ctx.prompt ("Starting client '" + instanceId + "' using "
                    + "transport '" + transport + "'... ");
        ConnectionContext<ScoreConnection> connCtx = null;
        RemoteConsoleFeedback remoteConsole = null;
        try {
            remoteConsole = conf.create (RemoteConsoleFeedback.class);
            connCtx = cm.get (scoreAddress);
            if (connCtx == null) {
                ctx.error ("Failed to connect to Score server at '"
                           + scoreAddress + "'");
                return;
            }

            ScoreConnection score = connCtx.getConnection();
            score.startClient(conf);
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
