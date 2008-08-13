package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.Strings;

import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: Aug 13, 2008
 * Time: 2:12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class RepositoryCommand extends RemoteCommand<ClientConnection> {

    private ConnectionManager<ClientConnection> cm;
    private String clientAddress;

    public RepositoryCommand(ConnectionManager<ClientConnection> cm,
                        String clientAddress) {
        super("repo", "Inspect the contents of the service repository", cm);
        this.clientAddress = clientAddress;

        setUsage("repo [filter_regex]");
    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);

        String filter = Strings.join (Arrays.asList(getArguments()), " ").trim();

        if (filter.equals("")) {
            filter = ".*";
        }

        /* Get and print the client id  */
        try {
            List<String> bundles = client.getRepository().list(filter);

            if (filter.equals(".*")) {
                ctx.info ("Bundles in repository:");
            } else {
                ctx.info ("Bundles in repository, matching '" + filter + "':");
            }
            for (String bdlId : bundles) {
                ctx.info("\t" + bdlId);
            }
        } finally {
            releaseConnection();
        }
    }

}