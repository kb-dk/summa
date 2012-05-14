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

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class HelpNotification extends Notification {
    private static final long serialVersionUID = 36846838864L;
    private String targetCmd;

    /**
     * Request generic help instructions.
     * @param cmd the command that requested the help
     */
    public HelpNotification (Command cmd) {
        super (cmd);
        targetCmd = null;
    }

    /**
     * Request help regarding a specific command.
     * @param cmd the command requesting the help
     * @param targetCmd the command for which to display a help message
     */
    public HelpNotification (Command cmd, String targetCmd) {
        super(cmd);
        this.targetCmd = targetCmd;
    }

    /**
     * <p>Return the name of the command for which help should be
     * displayed.</p>
     * <p>If the return value is {@code null} a generic help
     * message should be displayed.</p>
     * @return The name of the command for which help should be displayed.
     */
    public String getTargetCommand () {
        return targetCmd;
    }

}




