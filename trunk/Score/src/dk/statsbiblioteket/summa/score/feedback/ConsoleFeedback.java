/* $Id: ConsoleFeedback.java,v 1.4 2007/10/11 12:56:25 te Exp $
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
package dk.statsbiblioteket.summa.score.feedback;

import dk.statsbiblioteket.summa.score.api.Feedback;
import dk.statsbiblioteket.summa.score.api.Message;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Simple implementation of Feedback, that writes and reads from the
 * console.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Some methods neeeds Javadoc")
public class ConsoleFeedback implements Feedback {
    private static BufferedReader in =
            new BufferedReader(new InputStreamReader(System.in));

    /**
     * Output all the messages sequentially to the console and collect
     * requested responses.
     * @param messages the messages to display and get responses for.
     */
    public void putMessages(List<Message> messages) {
        for (Message message: messages) {
            putMessage(message);
        }
    }

    public void putMessage(Message message) {
        switch (message.getMessageType()) {
            case Message.MESSAGE_PLAIN:
                System.out.println(message);
                System.out.println("");
                break;
            case Message.MESSAGE_ALERT:
                System.err.print("[ALERT] ");
                System.err.println(message);
                System.err.println("");
                break;
            case Message.MESSAGE_REQUEST:
                System.out.println(message);
                try {
                    message.setResponse(in.readLine());
                } catch (IOException e) {
                    System.err.println("Exception reading input: "
                            + e.getMessage());
                    System.err.println("Defaulting to null response");
                    message.setResponse(null);
                }
                break;
            case Message.MESSAGE_SECRET_REQUEST:
                System.out.println(message);
                System.out.println("*** Warning ***");
                System.out.println("This implementation does not hide the "
                        + "input characters!");
                System.out.println("*** Warning end ***");
                try {
                    message.setResponse(in.readLine());
                } catch (IOException e) {
                    System.err.println("Exception reading input: "
                            + e.getMessage());
                    System.err.println("Defaulting to null response");
                    message.setResponse(null);
                }
                break;
            default:
                System.err.println("Error: Unknown message type: "
                        + message.getMessageType());
        }
    }
}
