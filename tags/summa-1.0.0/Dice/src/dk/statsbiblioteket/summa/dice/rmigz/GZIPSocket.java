/* $Id: GZIPSocket.java,v 1.2 2007/10/04 13:28:21 te Exp $
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

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 30/08/2006
 * Time: 08:44:12
 * To change this template use File | Settings | File Templates.
 */
public class GZIPSocket extends Socket {

    private InputStream in;
    private OutputStream out;

    public GZIPSocket() { super(); }

    public GZIPSocket(String host, int port)
        throws IOException {
            super(host, port);
    }

    public InputStream getInputStream()
            throws IOException {
        if (in == null) {
            in = new GZIPInputStream(super.getInputStream());
        }
        return in;
    }

    public OutputStream getOutputStream()
            throws IOException {
        if (out == null) {
            out = new GZIPOutputStream(super.getOutputStream());
        }
        return out;
    }

    public synchronized void close() throws IOException {
        OutputStream o = getOutputStream();
        o.flush();
        super.close();
    }
}



