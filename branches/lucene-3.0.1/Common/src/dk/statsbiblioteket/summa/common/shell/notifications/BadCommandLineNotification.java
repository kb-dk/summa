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
package dk.statsbiblioteket.summa.common.shell.notifications;

import org.apache.commons.cli.Options;
import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.summa.common.shell.Core;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * <p>Thrown by {@link Command} implementations when the command line
 * is malformed or insufficient parameters has been supplied.</p>
 *
 * <p>Throwing a {@code BadCommandLineNotification} will normally result
 * in the shell {@link Core} printing usage instructions based
 * on the {@link Command}s {@link Options}.</p>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class BadCommandLineNotification extends Notification {

    private Command cmd;

    /**
     * Create a new bad command line exception for a given command
     * with a given message.
     * @param cmd the command for which the error occured
     * @param msg a message to print, if this is {@code null} or the empty
     *            string, a generic message will be printed.
     */
    public BadCommandLineNotification(Command cmd, String msg) {
        super(cmd, msg);
    }
}




