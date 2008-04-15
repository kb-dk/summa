package dk.statsbiblioteket.summa.score.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.configuration.ConfigurationStorage;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.storage.MemoryStorage;
import dk.statsbiblioteket.summa.common.rpc.RemoteHelper;
import dk.statsbiblioteket.summa.score.api.ScoreConnection;
import dk.statsbiblioteket.summa.score.api.BadConfigurationException;
import dk.statsbiblioteket.summa.score.server.ClientDeployer;
import dk.statsbiblioteket.summa.score.server.ScoreUtils;
import dk.statsbiblioteket.summa.score.client.Client;
import dk.statsbiblioteket.summa.score.feedback.RemoteFeedback;
import dk.statsbiblioteket.summa.score.feedback.RemoteConsoleFeedback;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.Strings;
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

    private ConnectionManager<ScoreConnection> cm;
    private String scoreAddress;
    private String hostname;

    public DeployCommand(ConnectionManager<ScoreConnection> cm,
                         String scoreAddress) {
        super("deploy", "Deploy a client bundle");

        setUsage("deploy [options] <bundle-id> <instance-id> <target-host>");

        installOption ("t", "transport", true,
                       "Which deployment transport to use. Allowed values are"
                       + " 'ssh'. Default is ssh");

        installOption ("b", "basepath", true,
                       "What basepath to use for the client installation "
                       + "relative to the client user's home directory. "
                       + "Default is 'summa-score'");

        installOption ("c", "configuration", true,
                       "Url, RMI address or file path where the client can"
                       + " find its configuration. Default points at the "
                       + "Score configuration server");

        this.cm = cm;
        this.scoreAddress = scoreAddress;
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
        transport = ScoreUtils.getDeployerClassName(transport);

        String basePath =
                getOption("b") != null ? getOption("b") : "summa-score";
        String confLocation = getOption("c"); // This is allowed to be unset
                                    // - see ClientDeployer#CLIENT_CONF_PROPERTY

        /* Set up a configuration for the deployment request */
        Configuration conf =
                Configuration.newMemoryBased(
                        ClientDeployer.BASEPATH_PROPERTY,
                        basePath,
                        ClientDeployer.CLIENT_CONF_PROPERTY,
                        confLocation,
                        ClientDeployer.DEPLOYER_BUNDLE_PROPERTY,
                        bundleId,
                        ClientDeployer.INSTANCE_ID_PROPERTY,
                        instanceId,
                        ClientDeployer.DEPLOYER_CLASS_PROPERTY,
                        transport,
                        ClientDeployer.DEPLOYER_TARGET_PROPERTY,
                        target,
                        ClientDeployer.DEPLOYER_FEEDBACK_PROPERTY,
                "dk.statsbiblioteket.summa.score.feedback.RemoteFeedbackClient",
                        RemoteFeedback.REGISTRY_HOST_PROPERTY,
                        hostname);

        log.trace ("Created deployment config:\n" + conf.dumpString());

        /* Connect to the Score and send the deployment request */
        ctx.prompt ("Deploying '" + instanceId + "' on '" + target + "' using "
                    + "transport '" + transport + "'... ");
        ConnectionContext<ScoreConnection> connCtx = null;
        RemoteConsoleFeedback remoteConsole = null;
        try {
            log.trace("invoke: Creating remoteConsole");
            remoteConsole = Configuration.create(RemoteConsoleFeedback.class,
                                                 conf);
            log.trace("invoke: Getting connCtx for scoreAddress '"
                      + scoreAddress + "'");
            connCtx = cm.get (scoreAddress);
            if (connCtx == null) {
                ctx.error ("Failed to connect to Score server at '"
                           + scoreAddress + "'");
                return;
            }

            log.trace("invoke: Getting score connection");
            ScoreConnection score = connCtx.getConnection();
            log.trace("invoke: Calling deployClient");
            score.deployClient(conf);
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
