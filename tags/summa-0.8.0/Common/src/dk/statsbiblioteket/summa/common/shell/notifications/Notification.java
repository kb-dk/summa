/* $Id: Notification.java,v 1.2 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.2 $
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

/**
 * Generic base class for shell {@link Command} callbacks.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Notification extends RuntimeException {

    private Command cmd;

    public Notification (Command cmd, String msg) {
        super(msg);
        this.cmd = cmd;
    }

    public Notification (Command cmd) {
        this(cmd, "");
    }

    public Notification (String msg) {
        this (null, msg);
    }

    public Command getCommand () {
        return cmd;
    }

}
