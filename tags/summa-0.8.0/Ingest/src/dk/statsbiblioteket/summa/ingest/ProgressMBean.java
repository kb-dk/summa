/* $Id: ProgressMBean.java,v 1.5 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.5 $
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
package dk.statsbiblioteket.summa.ingest;

import java.util.ArrayList;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The Manageable interface for the Ingest process.<br>
 * Current implementation is limited to monitoring the process.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public interface ProgressMBean {

    /**
     * How many records has been successfully ingested in this process.
     * @return number of records.
     */
    public int getRecordCount();

    /**
     * How many records by now contains errors, the made the ingest process reject them.
     * @return number of bad records
     */
    public int getBadRecords();

    /**
     * A list of files scheduled for ingesting.
     * @return  the files in queue for processing.
     */
    public ArrayList getTodo();

    /**
     * A list of files that has been processed in this run.
     * @return  what have been completed.
     */
    public ArrayList getDone();

    /**
     * Current number of records, that has been passed and waiting in IO wait for the Storage.
     * @return in memory records ready to write to storage.
     */
    public int getCurrentQueueSize();

    /**
     * The current speed of ingest measured as  ∆R∕∆T.<br>
     * ∆ values are calculated from successive requests.
     * To be usable the monitor therefore should query this method frequently or not at all.
     *
     * @return the current speed defined as the mean speed from last time this was requested until now.
     */
    public int getCurrentIngestRate();

    /**
     * The global mean ingest rate.<br>
     * defined as getBadRecords+getRecordCount/processTime
     * @return the mean speed.
     */
    public int getMeanIngestRate();
    
}
