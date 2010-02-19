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

/**
 * Exception thrown by the {@link Command} class if the command
 * wants to terminate a shell.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class AbortNotification extends Notification {

    private int returnVal;

    public AbortNotification (Command cmd, String msg, int returnVal) {
        super(cmd, msg);
        this.returnVal = returnVal;
    }

    public AbortNotification (String msg, int returnVal) {
        super(msg);
        this.returnVal = returnVal; 
    }

    public AbortNotification (Command cmd, String msg) {
        this (cmd, msg, 0);
    }

    public AbortNotification (Command cmd) {
        super(cmd);
        returnVal = 0;
    }

    public int getReturnValue () {
        return returnVal;
    }

}




