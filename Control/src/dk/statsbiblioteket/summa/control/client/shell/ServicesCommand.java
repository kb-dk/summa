package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.client.Client;

import java.util.List;

/**
 * A {@link Command} to list the services deployed in a {@link Client}.
 * Used in {@link ClientShell}.
 */
public class ServicesCommand extends Command {

    ClientConnection client;

    public ServicesCommand(ClientConnection client) {
        super("services", "List and query all deployed services");
        this.client = client;

        installOption("s", "status", false,
                      "Include service status for each service");

    }

    public void invoke(ShellContext ctx) throws Exception {
        List<String> services = client.getServices();
        boolean listStatus = hasOption("s");

        ctx.info ("Known services:");

        for (String service : services) {
            String msg = "\t" + service;
            if (listStatus) {
                msg += " " + client.getServiceStatus(service);
            }
            ctx.info(msg);            
        }
    }
}
