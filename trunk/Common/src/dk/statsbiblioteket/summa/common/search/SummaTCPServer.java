/* $Id: SummaTCPServer.java,v 1.2 2007/10/04 13:28:21 te Exp $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: SummaTCPServer.java,v 1.2 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.common.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.Observer;

import dk.statsbiblioteket.summa.common.lucene.search.RPC.StreamingRequestHandler;
import dk.statsbiblioteket.summa.common.lucene.search.RPC.StreamingResponseHandler;
import dk.statsbiblioteket.util.qa.QAInfo;


/**
 * Wapper for LowLevelSearch that provides proper Server-behavior over TCP/IP.
 * The server listens to incoming requests and spawns a
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SummaTCPServer extends SearchServer {

    private static final Log log = LogFactory.getLog(SummaTCPServer.class);
    private int port;
    private LowLevelSearch searcher;
    ServerSocket tcp_server;
    boolean running;
    Executor pool;


    public static final int DEFAULT_TCP_PORT = 15000;

    public SummaTCPServer(int port, LowLevelSearch searcher, Executor pool) throws IOException {
        this.port = port;
        this.searcher = searcher;
        tcp_server = new ServerSocket(port);
        running = false;
        this.pool = pool;
    }

    /**
     * Closes any open connections and stops listening for requests.
     */
    public void close() {
        this.running = false;
    }

    public void start() {
        this.running = true;
    }


   public  boolean isRunning() {
        return running;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public void run() {
        while (running) {
            StreamingRequestHandler in;
            StreamingResponseHandler out;
            try {

                Socket s = tcp_server.accept();

                out = new StreamingResponseHandler(s.getOutputStream());
                in = new StreamingRequestHandler(s.getInputStream(), new Observer[]{out});

                pool.execute(in);
                pool.execute(out);

            } catch (IOException e) {
                log.error("Error in creating socket", e);
                if (running) {
                    cleanrun();
                }
            }
        }

    }


    private synchronized void cleanrun() {
        try {
            tcp_server.close();
            tcp_server = null;
            tcp_server = new ServerSocket(this.port);
            run();
        } catch (IOException e) {
            log.fatal("Unable to reinit socket server Server useless - exiting now", e);
            ((ThreadPoolExecutor) pool).shutdown();
            System.exit(-1);
        }


    }
}
