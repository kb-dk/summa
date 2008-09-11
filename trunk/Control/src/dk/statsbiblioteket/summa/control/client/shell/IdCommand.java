package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

/**
 * Print the client id
 */
public class IdCommand extends RemoteCommand<ClientConnection> {

    private ConnectionManager<ClientConnection> cm;
    private String clientAddress;

    public IdCommand(ConnectionManager<ClientConnection> cm,
                        String clientAddress) {
        super("id", "Print the instance id of the client", cm);
        this.clientAddress = clientAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);

        /* Get and print the client id  */
        try {
            String id = client.getId();
            ctx.info(id);
        } finally {
            releaseConnection();
        }
    }

}


