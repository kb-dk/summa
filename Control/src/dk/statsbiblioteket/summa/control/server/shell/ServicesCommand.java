package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * {@link dk.statsbiblioteket.summa.common.shell.Command} for listing the
 * deployed services in a given {@link dk.statsbiblioteket.summa.control.client.Client}
 * directly from the Control shell
 */
public class ServicesCommand extends RemoteCommand<ControlConnection> {

    private String controlAddress;

    public ServicesCommand(ConnectionManager<ControlConnection> connMgr,
                         String controlAddress) {
        super("services", "List the services deployed on a given client",
              connMgr);

        setUsage("services [options] <clientId> [clientId]");
        installOption("s", "status", false, "Print the status of each service");
        installOption("b", "bundle", false,
                      "Print the bundle id of each service");

        this.controlAddress = controlAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ControlConnection control = getConnection(controlAddress);

        String[] clients = getArguments();

        String header = "\tClient/Service";
        if (hasOption("bundle")) {
            header += "\tBundle";
        }
        if (hasOption("status")) {
            header += "\tStatus";
        }
        ctx.info(header);

        try {
            if (clients.length == 0) {
                ctx.error("No arguments. Please give at least one client id");
            } else {
                for (String clientId : clients) {
                    printServices(ctx, control, clientId);
                }
            }
        } finally {
            releaseConnection();
        }
    }

    private void printServices(ShellContext ctx, ControlConnection control,
                                    String clientId) throws IOException {

        String msg = null;
        List<String> services = null;
        ClientConnection client = null;

        try {
            client = control.getClient(clientId);
            services = client.getServices();
        } catch (InvalidClientStateException e) {
            msg = "\t" + clientId + ": Not running";
        } catch (NoSuchClientException e) {
            msg = "\t" + clientId + ": No such client";
        }

        if (services == null) {
            ctx.info(msg);
            return;
        }

        for (String serviceId : services) {
            msg = "\t" + clientId + "/" + serviceId;

            if (hasOption("bundle")) {
                String bdl;
                try {
                    String bdlSpec = client.getBundleSpec(serviceId);
                    BundleSpecBuilder spec = BundleSpecBuilder.open(
                            new ByteArrayInputStream(bdlSpec.getBytes()));
                    bdl = spec.getBundleId();
                } catch (Exception e) {
                    bdl = "Unknown";
                }
                msg += "\t" + bdl;
            }
            if (hasOption("status")) {
                try {
                    Service service = client.getServiceConnection(serviceId);
                    msg += "\t" + service.getStatus().toString();
                } catch (InvalidServiceStateException e){
                    msg += "\tNot running";
                } catch (NoSuchServiceException e) {
                    msg += "\tNo such service '" + serviceId + "'";
                }
            }
            
            ctx.info(msg);
        }

    }
}