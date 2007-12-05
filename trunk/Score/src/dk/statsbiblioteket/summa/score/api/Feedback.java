/* $Id: Feedback.java,v 1.4 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/11 12:56:25 $
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
package dk.statsbiblioteket.summa.score.api;

import dk.statsbiblioteket.summa.score.api.Message;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.List;

/**
 * Provides feedback with optional requests for text input.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface Feedback {
    /**
     * Display the given messages to the user and collect responses. This
     * could be sequentially on a prompt or all at a time at a webpage.
     * This method can be called more than once.
     * @param messages the messages to display and collect responses for.
     */
    public void putMessages(List<Message> messages);

    /**
     * Convenience method for {@link #putMessages}.
     * @param message the message to display and collect a response for.
     */
    public void putMessage(Message message);
}
