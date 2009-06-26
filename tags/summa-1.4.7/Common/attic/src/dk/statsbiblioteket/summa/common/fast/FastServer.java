/* $Id: FastServer.java,v 1.2 2007/10/04 13:28:20 te Exp $
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
package dk.statsbiblioteket.summa.common.fast;

import java.io.IOException;
import java.io.InputStream;

import dk.statsbiblioteket.util.qa.QAInfo;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Possibly deprecated (or rather: never used)")
public interface FastServer {
    /**
     * Performs actions based on the request. Note that the request might not
     * have been fully formed, depending on implementation, as it is streamed.
     * @see   Telegram#isFullyFormed().
     * @param request a request for action.
     * @throws IOException in case of communication problems.
     */
    public void handleRequest(Telegram request) throws IOException;

    /**
     * Send a response to the request given in {@link #handleRequest(Telegram)}.
     * Note that the response does not need to be fully formed, but that is
     * must be possible to call {@link Telegram#toStream(InputStream)}.
     * @param response a response to a request.
     * @throws IOException in case of communication problems.
     */
    public void sendResponse(Telegram response) throws IOException;
}



