/* $Id: ClientException.java,v 1.5 2007/10/11 12:56:25 te Exp $
 * $Revision: 1.5 $
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

import dk.statsbiblioteket.summa.score.client.Client;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;

/**
 * Generic base exception for {@link Client} objects
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke",
        comment="Some methods needs Javadoc")
public class ClientException extends RuntimeException {

    private String id;

    public ClientException (String clientId, String message) {
        super ("For client id '" + clientId + "': " + message);
        id = clientId;
    }

    public ClientException (Client client, String message) {
        super ("Client[" + getClientId(client) + "] " + message);
        id = client.getId();
    }

    public ClientException (Client client, String message, Throwable cause) {
        super ("Client[" + getClientId(client) + "] " + message, cause);
        id = client.getId();
    }

    public ClientException (Client client, Throwable cause) {
        super ("Client[" + getClientId(client) + "]", cause);
        id = client.getId();
    }

    public String getClientId () {
        return id;
    }

    private static String getClientId (Client client) {
        return client.getId();
    }
}
