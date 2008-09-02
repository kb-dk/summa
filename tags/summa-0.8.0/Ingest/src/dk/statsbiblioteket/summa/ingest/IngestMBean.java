/* $Id: IngestMBean.java,v 1.5 2007/10/05 10:20:22 te Exp $
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

import dk.statsbiblioteket.util.qa.QAInfo;


/**
 * manageable interface for the Ingest.
 * 
 * No current implementations exists.
 * @deprecated
 * @see dk.statsbiblioteket.summa.ingest.ProgressMBean
 */
@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public interface IngestMBean {

    /**
     * Gets the current list of files parsed through a {@link dk.statsbiblioteket.summa.ingest.ParserTask}
     * @return                  a list of files.
     */
    public String[] getTransformedFiles();


    /**
     * All records from these files has been submitted to Metadata Storage or rejected due to errors.
     * @return                  an other list of files
     */
    public String[] getIngestedFiles();

    /**
     * Gets the last ingested (submitted to Metadata Storage) RecordID
     * @return                 the RecordID for the last ingested {@link dk.statsbiblioteket.summa.common.Record}
     */
    public String getLastID();

    /**
     * A short sentence target for a human audience, giving hints to where in the ingest work-flow we are.
     *
     * @return                  what is going on?
     */
    public String getCurrentOperation();



}
