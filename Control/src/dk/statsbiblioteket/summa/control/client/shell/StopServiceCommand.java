package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.ClientException;
import dk.statsbiblioteket.summa.control.api.Service;
import dk.statsbiblioteket.summa.control.client.Client;

/**
 * A shell command to launch a {@link Service} deployed in a {@link Client}.
 */
public class StopServiceCommand extends Command {

    private ClientConnection client;

    public StopServiceCommand(ClientConnection client) {
        super("stop", "Stop a service given by id");
        this.client = client;

        setUsage("stop <service-id> [service-id] ...");
    }

    public void invoke(ShellContext ctx) throws Exception {

        if (getArguments().length == 0) {
            ctx.error ("At least one package id should be specified.");
            return;
        }

        for (String id : getArguments()) {

            ctx.prompt ("Stopping service '" + id + "' ... ");

            try {
                client.stopService(id);
            } catch (ClientException e){
                // A ClientException is a controlled exception, we don't print
                // the whole stack trace
                ctx.info ("FAILED");
                ctx.error(e.getMessage());
                continue;
            } catch (Exception e) {
                throw new RuntimeException ("Stopping of service failed: "
                                            + e.getMessage(), e);
            }            

            ctx.info("OK");
        }
    }
}
