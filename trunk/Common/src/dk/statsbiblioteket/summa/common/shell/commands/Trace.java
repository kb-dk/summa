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
package dk.statsbiblioteket.summa.common.shell.commands;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.ShellContext;
import dk.statsbiblioteket.summa.common.shell.notifications.TraceNotification;
import dk.statsbiblioteket.summa.common.shell.Core;

/**
 * Command used to display the last stack trace recorded by the shell {@link Core}.
 */
public class Trace extends Command {

    public Trace() {
        super("trace", "Print the last recorded stack trace");
    }

    public void invoke(ShellContext ctx) throws Exception {
        throw new TraceNotification(this);
    }
}




