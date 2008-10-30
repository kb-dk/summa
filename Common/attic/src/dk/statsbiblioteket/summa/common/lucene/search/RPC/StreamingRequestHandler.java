/* $Id: StreamingRequestHandler.java,v 1.3 2007/10/04 13:28:22 te Exp $
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

import dk.statsbiblioteket.summa.common.search.response.ResponseReader;
import dk.statsbiblioteket.summa.common.search.response.ResponseInputStream;
import dk.statsbiblioteket.summa.common.search.request.RequestReaderImpl;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.InputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class StreamingRequestHandler extends Observable implements Runnable {


    private InputStream stream;
    private ResponseReader reader;
    private boolean running;

    public StreamingRequestHandler(InputStream s, Observer[] responsehandlers){
        super();
        for(Observer obs : responsehandlers ){
              super.addObserver(obs);
        }
        stream = s;
        reader = new ResponseInputStream(stream);
        running = true;

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
        while(running){
            try {
                int i = stream.read();
                this.setChanged();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            notifyObservers(new RequestReaderImpl());
            this.clearChanged();
        }




    }
}



