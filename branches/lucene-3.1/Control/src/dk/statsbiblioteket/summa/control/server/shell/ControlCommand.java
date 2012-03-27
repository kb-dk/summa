/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.control.server.shell;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.control.api.*;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.rpc.ConnectionContext;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: mikkel Date: Jan 31, 2008 Time: 11:09:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class ControlCommand extends Command {

    private ConnectionManager<ControlConnection> cm;
    private String controlAddress;

    public ControlCommand (ConnectionManager<ControlConnection> cm,
                           String controlAddress) {
        super ("control", "Inspect deployment metadata for clients");

	setUsage("control <clientId>");
        //installOption("D", "set", true, "Set a deployment property, fx. '-Dsumma.configuration=config/new.xml'");
        //installOption("e", "extended", false, "Get deployment metadata about each client");

        this.cm = cm;
        this.controlAddress = controlAddress;
    }

    public void invoke(ShellContext ctx) throws Exception {
        /* Connect to the Control and request the control file
	 * for the requested client */
        
        String[] args = getArguments();
	
	if (args.length == 0) {
	    ctx.error("No client specified");
	    return;
	}
	
	String client = args[0];

        ConnectionContext<ControlConnection> connCtx = null;
        try {
            connCtx = cm.get (controlAddress);
            if (connCtx == null) {
                ctx.error ("Failed to connect to Control server at '"
                           + controlAddress + "'");
                return;
            }

            ControlConnection control = connCtx.getConnection();
            Configuration conf = control.getDeployConfiguration(client);

            /* Header */
            ctx.info("Control data for '" + client + "':");

            /* Generate report, with properties sorted by name */
	    Map<String,Serializable> sortedProps = 
	                             new TreeMap<String,Serializable>();
	    for (Map.Entry<String,Serializable> prop : conf) {
	        sortedProps.put(prop.getKey(), prop.getValue());
            }
            for (Map.Entry<String,Serializable> prop : sortedProps.entrySet()) {
	        ctx.info("\t" + prop.getKey() + " = " + prop.getValue());
            }

        } finally {
            if (connCtx != null) {
                cm.release (connCtx);
            }
        }
    }
}




