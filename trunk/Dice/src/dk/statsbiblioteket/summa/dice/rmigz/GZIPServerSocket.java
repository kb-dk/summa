/* $Id: GZIPServerSocket.java,v 1.2 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:21 $
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
package dk.statsbiblioteket.summa.dice.rmigz;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 30/08/2006
 * Time: 08:47:44
 * To change this template use File | Settings | File Templates.
 */
public class GZIPServerSocket extends ServerSocket {

    public GZIPServerSocket(int port) throws IOException {
        super(port);
    }

    public GZIPServerSocket (int port, int backlog) throws IOException {
        super (port, backlog);
    }

    public GZIPServerSocket (int port, int backlog, InetAddress bindAddr) throws IOException {
        super (port, backlog, bindAddr);
    }


    public Socket accept() throws IOException {
        Socket socket = new GZIPSocket();
        implAccept(socket);
        return socket;
    }
}



