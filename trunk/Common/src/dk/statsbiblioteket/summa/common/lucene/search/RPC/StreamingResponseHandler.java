/* $Id: StreamingResponseHandler.java,v 1.3 2007/10/04 13:28:22 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:22 $
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
package dk.statsbiblioteket.summa.common.lucene.search.RPC;

import dk.statsbiblioteket.summa.common.search.response.ResponseOutputStream;
import dk.statsbiblioteket.summa.common.search.request.RequestReader;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.OutputStream;
import java.util.Observable;
import java.util.Observer;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class StreamingResponseHandler implements Runnable, Observer {


    private OutputStream stream;
    private boolean running;
    private RequestReader currentTask;
    private ResponseOutputStream writer;


    public StreamingResponseHandler(OutputStream out){
        this.stream = out;
        this.writer = new ResponseOutputStream(this.stream);
        this.running = false;
        currentTask = null;
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
       running = true;
       RequestReader runningReader = this.currentTask;
       int i = 0;
       while(running){
           if (runningReader != null && runningReader == this.currentTask){
                //process request;
               System.out.println("processing request");
               this.currentTask = null;
           } else if (this.currentTask != null){
               synchronized(this){
                    i = 0;
                    System.out.println("new request to process");
                    runningReader = this.currentTask;
               }
           } else {
               //System.out.println("nothing to do");
               Thread.yield();
           }
       }
    }

    public void close(){
        running = false;
    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>
     *            method.
     */
    public void update(Observable o, Object arg) {
           if (arg  instanceof RequestReader){
                 this.currentTask = (RequestReader)arg;
           } else {
               throw new IllegalArgumentException("update needs a RequestReader got:"  + arg.getClass().getName());
           }
    }
}



