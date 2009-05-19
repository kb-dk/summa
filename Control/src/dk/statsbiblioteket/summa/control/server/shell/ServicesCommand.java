package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.control.bundle.BundleSpecBuilder;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.Layout;
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
        installOption("f", "formatted", false, "Print strictly formatted output"
                                               + " for machine parsing. The "
                                               + "format is: "
                                               + "'client/service | bundle | statusCode | message'");

        this.controlAddress = controlAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ControlConnection control = getConnection(controlAddress);
        Layout layout = new Layout();
        String[] clients = getArguments();
        boolean listBundles = hasOption("bundle");
        boolean listStatus = hasOption("status");
        boolean formatted = hasOption("formatted");

        if (formatted) {
            layout.appendColumns("ClientService", "Bundle",
                                 "StatusCode", "Message");
            layout.setPrintHeaders(false);
            layout.setDelimiter(" | ");
        } else {
            layout.appendColumns("ClientService");
            if (listBundles) {
                layout.appendColumns("Bundle");
            }
            if (listStatus) {
                layout.appendColumns("Message");
            }
        }

        try {
            if (clients.length == 0) {
                ctx.error("No arguments. Please give at least one client id");
            } else {
                for (String clientId : clients) {
                    listServices(layout, control, clientId);
                }
                ctx.info(layout.toString());
            }
        } finally {
            releaseConnection();
        }
    }

    private void listServices(Layout layout, ControlConnection control,
                              String clientId)            throws IOException {

        String msg = "";
        List<String> services = null;
        ClientConnection client = null;

        try {
            client = control.getClient(clientId);
            services = client.getServices();
        } catch (InvalidClientStateException e) {
            msg = "Not running";
        }

        if (services == null) {
            layout.appendRow("ClientService", clientId,
                             "Message", msg);
            return;
        }

        for (String serviceId : services) {
            String clientService = clientId + "/" + serviceId;
            String bundleId = "", statusCode = "";
            msg = "";
            if (hasOption("bundle")) {
                try {
                    String bdlSpec = client.getBundleSpec(serviceId);
                    BundleSpecBuilder spec = BundleSpecBuilder.open(
                            new ByteArrayInputStream(bdlSpec.getBytes()));
                    bundleId = spec.getBundleId();
                } catch (Exception e) {
                    bundleId = "Unknown";
                }
            }
            if (hasOption("status")) {
                try {
                    Service service = client.getServiceConnection(serviceId);
                    Status status = service.getStatus();
                    statusCode = status.getCode().toString();
                    msg = status.toString();
                } catch (InvalidServiceStateException e){
                    statusCode = Status.CODE.not_instantiated.toString();
                    msg = "Not running";
                } catch (NoSuchServiceException e) {
                    statusCode = Status.CODE.not_instantiated.toString();
                    msg = "No such service";
                }
            }
            
            layout.appendRow("ClientService", clientService,
                             "Bundle", bundleId,
                             "StatusCode", statusCode,
                             "Message", msg);
        }
    }
}