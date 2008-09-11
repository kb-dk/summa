/* $Id: JobStatus.java,v 1.2 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.2 $
 * $Date: 2007/10/04 13:28:19 $
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
package dk.statsbiblioteket.summa.dice;

import java.io.Serializable;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Immutable object enclosing status information about a Job.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class JobStatus implements Serializable {

    public enum Status {
        JOB_NONE,
        JOB_WORKER_FETCHING,
        JOB_WORKER_ENQUEUED,
        JOB_PROCESSING,
        JOB_PROCESSED,
        JOB_CONSUMER_FETCHING,
        JOB_FAILED,
        JOB_CONSUMER_ENQUEUED,
        JOB_CONSUMING,
        JOB_CONSUMED,
        JOB_LOST,
    };

    private String jobName;
    private Status status;

    public JobStatus (Job job, Status status) {
        this.jobName = job.getName();
        this.status = status;
    }
    
    public JobStatus (String jobName, Status status) {
        this.jobName = jobName;
        this.status = status;
    }

    /**
     * Return the name of the corresponding {@link Job}
     */
    public String getName () {
        return jobName;
    }

    public Status getStatus () {
        return status;
    }

    public String toString () {
        return jobName + "::" + status;
    }
}



