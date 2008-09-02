/* $Id: FastClient.java,v 1.2 2007/10/04 13:28:20 te Exp $
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

import dk.statsbiblioteket.util.qa.QAInfo;

/*
* The State and University Library of Denmark
* CVS:  $Id: FastClient.java,v 1.2 2007/10/04 13:28:20 te Exp $
*/
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Possibly deprecated (or rather: never used)")
public interface FastClient {
    /**
     * Send a request and return the response. Note that neither the request,
     * nor the response needs to be fully formed.
     * @see Telegram#isFullyFormed().
     * @param request a request for action.
     * @return a response to the request. Depending on the implementation,
     *         this can be null.
     * @throws IOException in case of communication problems.
     */
    public Telegram sendRequest(Telegram request) throws IOException;
}
