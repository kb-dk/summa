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
package dk.statsbiblioteket.summa.control.client.shell;

import dk.statsbiblioteket.summa.control.api.ClientConnection;
import dk.statsbiblioteket.summa.common.shell.RemoteCommand;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.util.rpc.ConnectionManager;
import dk.statsbiblioteket.util.Strings;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link Command} for the {@link ClientShell} used to list the contents
 * of the configured repository.
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

            /* Sort the bundles before displaying them  */
            SortedSet<String> sortedBundles = new TreeSet<String>(bundles);
            for (String bdlId : sortedBundles) {
                ctx.info("\t" + bdlId);
            }
        } finally {
            releaseConnection();
        }
    }

}



