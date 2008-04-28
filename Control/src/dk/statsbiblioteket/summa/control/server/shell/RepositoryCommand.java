package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.ControlConnection;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.util.List;
import java.util.regex.Pattern;
import java.rmi.RemoteException;

/**
 * Shell command for the Control shell to manage the repository
 */
public class RepositoryCommand extends Command {

    private ConnectionManager<ControlConnection> cm;
    private String controlAddress;

    public RepositoryCommand (ConnectionManager<ControlConnection> cm,
                              String controlAddress) {
        super ("repo", "Manage the Control repository");

        this.cm = cm;
        this.controlAddress = controlAddress;

        setUsage("repo <action>");

        installOption("l", "list", false, "List all available bundles");
        installOption("r", "regex", true, "List all available bundles matching"
                                           + " a regular expression");

    }


    public void invoke(ShellContext ctx) throws Exception {
        String[] args = getArguments();

        if (hasOption("l")) {
            doListBundles (ctx, null);
        } else if (hasOption("r")) {
            doListBundles (ctx, getOption("r"));
        } else {
            ctx.error ("No valid action switches, nothing to do."
                       + " Try 'help " + getName() + "'");
        }
    }

    private void doListBundles(ShellContext ctx, String filterRegex)
                                                              throws Exception {
        ConnectionContext<ControlConnection> conn = null;
        ControlConnection control = null;
        Pattern regex = null;

        if (filterRegex != null) {
            regex = Pattern.compile(filterRegex);
        }

        try {
            /* Get connection and retrieve a list of bundles */
            conn = cm.get (controlAddress);
            if (conn == null) {
                ctx.error ("Failed to connect to Control server on '"
                           + controlAddress + "'");
                return;
            }
            control = conn.getConnection();
            List<String> bundleIds = control.getBundles();

            /* Header message */
            if (regex != null) {
                ctx.info ("Known bundles matching '" + filterRegex + "':");
            } else {
                ctx.info ("Known bundles:");
            }

            /* List the matching bundles */
            for (String bundleId : bundleIds) {
                if (regex != null) {
                    if (regex.matcher(bundleId).matches()) {
                        ctx.info ("\t" + bundleId);
                    }
                } else {
                    ctx.info ("\t" + bundleId);
                }
            }
        } catch (RemoteException e) {
            cm.reportError (conn, e); // conn != null always true here            
        } finally {
            if (conn != null) {
                cm.release (conn);
            }
        }

    }
}
