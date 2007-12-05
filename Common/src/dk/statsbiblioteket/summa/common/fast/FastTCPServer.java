/* $Id: FastTCPServer.java,v 1.4 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.4 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: FastTCPServer.java,v 1.4 2007/10/04 13:28:20 te Exp $
 */
package dk.statsbiblioteket.summa.common.fast;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A TCP-oriented implementation of FastServer.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te",
        comment = "Possibly deprecated (or rather: never used)")
public class FastTCPServer implements FastServer, Runnable {
    public Log log = LogFactory.getLog(FastTCPServer.class);

    public static final String PROPERTY_PORT = "summa.fastserver.serverport";
    private static final int DEFAULT_PORT = 17001;

    private ServerSocket socket;
    private Configuration configuration;
    private Thread thread;
    private int port = DEFAULT_PORT;

    public FastTCPServer(Configuration configuration) throws IOException {
        log.trace("Creating Fast server");
        this.configuration = configuration;
        try {
            port = configuration.getInt(PROPERTY_PORT);
        } catch (Exception e) {
            log.warn("Could not retrieve port from property " + PROPERTY_PORT
                     + ". Defaulting to " + DEFAULT_PORT);
        }
        log.debug("Connecting Fast server to port " + port);
        socket = new ServerSocket(port);
        log.debug("Connection to port " + port + " successfull. "
                  + "Waiting for requests");
        thread = new Thread(this);
        thread.run();
        log.trace("Finished construction of Fast server on port " + port
                  + ". The listener is now running");
    }

    public void handleRequest(Telegram request) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void sendResponse(Telegram response) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void run() {
        log.trace("Beginning run loop for Fast server on port " + port);
        while (true) {
            Socket s;
            try {
                s = socket.accept();
            } catch (IOException e) {
                log.error("Could not accept connection on socket for port "
                          + port + ". Attempting reconnect...");
                reconnect();
            }
        }
//        log.trace("Ending run loop for Fast server on port " + port);
    }

    // TODO: What to do if the thread dies?
    private void reconnect() {
        while (true) {
            try {
                socket.close();
                socket = new ServerSocket(port);
                break;
            } catch (IOException e1) {
                log.fatal("Could not reconnect Fast server to port "
                          + port
                          + ". Sleeping for 5 seconds before retry");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e2) {
                    log.info("Fast server interrupted, while waiting "
                             + "5 seconds to reconnect. Attempting "
                             + "reconnection immediately");
                }
            }
        }
    }
}
