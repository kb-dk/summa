package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.control.api.Status;
import dk.statsbiblioteket.summa.control.api.InvalidServiceStateException;
import dk.statsbiblioteket.summa.control.client.Client;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.io.ByteArrayInputStream;

/**
 * A {@link Command} to list the services deployed in a {@link Client}.
 * Used in {@link ClientShell}.
 */
public class ServicesCommand extends RemoteCommand<ClientConnection> {

    private String clientAddress;

    public ServicesCommand(ConnectionManager<ClientConnection> connMgr,
                           String clientAddress) {
        super("services", "List and query all deployed services", connMgr);
        this.clientAddress = clientAddress;

        installOption("s", "status", false,
                      "Include service status for each service");
        installOption("b", "bundle", false,
                      "Display bundle version for each service");

    }

    public void invoke(ShellContext ctx) throws Exception {
        ClientConnection client = getConnection(clientAddress);

        try {
            List<String> services = client.getServices();
            boolean listStatus = hasOption("status");
            boolean listBundle = hasOption("bundle");

            String header = "\tService";
            if (listBundle) {
                header += "\t" + "Bundle";
            }
            if (listStatus) {
                header += "\t" + "Status";
            }
            ctx.info (header);

            /* List services sorted alphabetically */
            SortedSet<String> sortedServices = new TreeSet<String>(services);
            for (String service : sortedServices) {
                String msg = "\t" + service;
                if (listBundle) {
                    String bdl;
                    try {
                        String bdlSpec = client.getBundleSpec(service);
                        BundleSpecBuilder spec = BundleSpecBuilder.open(
                                  new ByteArrayInputStream(bdlSpec.getBytes()));
                        bdl = spec.getBundleId();
                    } catch (Exception e) {
                        bdl = "Unknown";
                    }
                    msg += "  " + bdl;
                }
                if (listStatus) {
                    String status;
                    try {
                         status = "" + client.getServiceStatus(service);
                    } catch (InvalidServiceStateException e) {
                        status = "Not running";
                    } catch (Exception e) {
                        status = "Error connecting: " + e.getMessage();
                        client.reportError(service);
                    }
                    msg += "  " + status;
                }
                ctx.info(msg);
            }
        } finally {
            releaseConnection();
        }
    }
}



