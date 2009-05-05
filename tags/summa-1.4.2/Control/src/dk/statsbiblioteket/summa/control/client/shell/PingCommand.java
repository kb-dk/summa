package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Aug 5, 2008 Time: 8:13:20 AM To
 * change this template use File | Settings | File Templates.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Unfinished")
public class PingCommand extends RemoteCommand<ClientConnection> {

    private String clientAddress;

    public PingCommand (ConnectionManager<ClientConnection> cm,
                        String clientAddress) {
        super("ping", "Test the connection to the client server", cm);
        this.clientAddress = clientAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);

        ctx.prompt ("Pinging client server at '" + clientAddress + "'...");

        try {
            client.getStatus();
            ctx.info ("OK");
        } finally {
            releaseConnection();
        }


    }
}



