/* $Id: Monitor.java,v 1.4 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.tools.monitor;

import dk.statsbiblioteket.summa.tools.schedule.Schedulable;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.Observable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public abstract class Monitor extends Observable implements Schedulable, MonitorMBean {

    protected PersistentHashMap recordIndexClusters = null;

    public static final String persistFileName = "IndexMonitor.instance";

    protected static String persist = null;

    protected static final Log log = LogFactory.getLog(Monitor.class);

    protected static Monitor _instance = null;

    protected Monitor(){

    }



    public abstract void perform();

    protected void finalize() throws Throwable {
        super.finalize();
        store();
    }


    protected void init(String persist) throws FileNotFoundException, IOException, ClassNotFoundException {
        log.info("Initializing monitor from: " + persist + File.separator + persistFileName);
        recordIndexClusters = new PersistentHashMap(persist);
        this.persist = persist;
       /* File f = new File(persist + File.separator + persistFileName);
        if (f.exists()) {


            final ObjectInputStream in = new ObjectInputStream(new FileInputStream(persist + File.separator + persistFileName));
            recordIndexClusters = (HashMap) in.readObject();
            in.close();
        } else {
            recordIndexClusters = new HashMap();
            this.persist = persist;
        }*/

    }

    protected void store() throws IOException {
       /*
        log.debug("Trying to store the monitor state at:" + persist + File.separator + persistFileName);
        final ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(persist + File.separator + persistFileName));
        out.writeObject(recordIndexClusters);
        out.close();
      */
    }

    protected void clear(){
        File f = new File(persist + File.separator + persistFileName);
        if (f.exists()) f.delete();
        try {
            recordIndexClusters = new PersistentHashMap(persist);
        } catch (IOException e) {
            log.error(e);
        }
    }

    public String getPersistFileName(){
        return persistFileName;
    }

    public String getPersistentDirectory(){
        return persist;
    }


}
