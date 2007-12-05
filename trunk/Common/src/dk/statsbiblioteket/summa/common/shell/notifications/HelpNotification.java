/* $Id: HelpNotification.java,v 1.3 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:20 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.shell.notifications;

import dk.statsbiblioteket.summa.common.shell.Command;
import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class HelpNotification extends Notification {

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
     * @return
     */
    public String getTargetCommand () {
        return targetCmd;
    }

}
