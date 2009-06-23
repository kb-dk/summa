package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.Layout;
import dk.statsbiblioteket.util.rpc.ConnectionManager;

import java.io.IOException;

/**
 *
 */
public class StatusCommand extends RemoteCommand<ControlConnection> {

    private String controlAddress;

    public StatusCommand(ConnectionManager<ControlConnection> connMgr,
                         String controlAddress) {
        super("status", "Print the status of the control server, query the " +
              "status of deployed clients, or query the status of deployed " +
              "services",
              connMgr);

        installOption("f", "formatted", false, "Print strictly formatted output"
                                               + " for machine parsing. Format "
                                               + "is: "
                                               + "'client/service | statusCode | message'");

        setUsage("status [clientId|clientId/serviceId]...");

        this.controlAddress = controlAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        ControlConnection control = getConnection(controlAddress);
        Layout layout = new Layout("Client/Service");
        String[] targets = getArguments();

        if (hasOption("formatted")) {
            layout.appendColumns("StatusCode", "Message");
            layout.setPrintHeaders(false);
            layout.setDelimiter(" | ");
        } else {
            layout.appendColumns("Message");
        }

        try {
            if (targets.length == 0) {
                Status status = control.getStatus();
                ctx.info("Control status: " + status.toString());
            } else {
                for (String target : targets) {
                    String[] parsedTarget = target.split("/");

                    if (parsedTarget.length == 1) {
                        listClientStatus(layout, control, parsedTarget[0]);
                    } else if (parsedTarget.length == 2){
                        listServiceStatus(layout, control,
                                          parsedTarget[0], parsedTarget[1]);
                    } else {
                        ctx.warn("Malformed client or service id: " + target);
                    }
                }
                ctx.info(layout.toString());
            }
        } finally {
            releaseConnection();
        }
    }

    private void listClientStatus(Layout layout, ControlConnection control,
                                   String clientId) throws IOException {
        String statusCode;
        String msg;
        try {
            ClientConnection client = control.getClient(clientId);
            Status status =  client.getStatus();
            statusCode = status.getCode().toString();
            msg = status.toString();
        } catch (InvalidClientStateException e) {
            statusCode = Status.CODE.not_instantiated.toString();
            msg = "Not running";
        } catch (NoSuchClientException e) {
            statusCode = Status.CODE.not_instantiated.toString();
            msg = "No such client";
        }

        layout.appendRow("Client/Service", clientId,
                         "StatusCode", statusCode,
                         "Message", msg);
    }

    private void listServiceStatus(Layout layout, ControlConnection control,
                                    String clientId, String serviceId)
                                                            throws IOException {
        String statusCode;
        String msg;

        try {
            ClientConnection client = control.getClient(clientId);
            Service service = client.getServiceConnection(serviceId);
            Status status = service.getStatus();
            statusCode = status.getCode().toString();
            msg = status.toString();
        } catch (InvalidServiceStateException e){
            statusCode = Status.CODE.not_instantiated.toString();
            msg = e.getMessage();
        } catch (NoSuchServiceException e) {
            statusCode = Status.CODE.not_instantiated.toString();
            msg = "No such service '" + serviceId + "'";
        } catch (InvalidClientStateException e) {
            statusCode = Status.CODE.not_instantiated.toString();
            msg = "Client not running";
        } catch (NoSuchClientException e) {
            statusCode = Status.CODE.not_instantiated.toString();
            msg = "No such client";
        }

        layout.appendRow("Client/Service", clientId + "/" + serviceId,
                         "StatusCode", statusCode,
                         "Message", msg);

    }
}



