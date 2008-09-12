/* $Id: Job.java,v 1.2 2007/10/04 13:28:19 te Exp $
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
import java.util.HashMap;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Immutable object representing a Job to be processed by a {@link Worker}.
 *
 * The <code>data</code> field of a Job object can be
 * any serializable Java object. If you are using lots of smaller
 * data objects with many fields you might want to let the data objects
 * implement the {@link java.io.Externalizable} interface to reduce
 * serialization overhead.
 *
 * If the data objects are very large, you should consider using {@link CachingWorker}.
 * {@link CachingEmployer}, and {@link CachingConsumer}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Job implements Serializable {
    protected Object data;
    protected HashMap<String,String> jobHints;
    protected String jobName;

    /**
     * Construct a new Job object.
     * @param data Data to process by the {@link Worker}
     * @param jobHints HashMap passing relevant information to the {@link Worker} processing the Job.
     * @param jobName Unique name identifying this Job to the {@link Employer}
     */
    public Job (Object data, HashMap<String,String> jobHints, String jobName) {
        this.data = data;
        this.jobName = jobName;
        if (jobHints == null) {
            this.jobHints = new HashMap();
        } else {
            this.jobHints = jobHints;
        }
    }

    public Object getData () {
        return data;
    }

    /**
     * Get the value of a job hint. A hint can contain information relevat
     * to processing this job.
     * @param hintName
     * @return the hint value or null if the hint does not exist
     */
    public String getHint (String hintName) {
        return jobHints.get(hintName);
    }
    
    public HashMap<String,String> getHints () {
        return jobHints;
    }
    
    public String getName () {
        return jobName;
    }

    public String toString () {
        return jobName;
    }

    /**
     * Dump information about this Job. Mainly for debugging.
     * Currently the dump contains job name, data class, and
     * all job hints.
     */
    public String[] dump () {
        String[] result;
        result = new String[jobHints.size()+2];

        result [0] = "jobName: " + jobName;
        result [1] = "dataClass: " + data.getClass();

        int i = 2;
        for (String key : jobHints.keySet()) {
            result[i] = "hint::" + key + " = " + jobHints.get(key);
        }
        return result;
    }

    /**
     * Dump Job information in a single string.
     * See {@link #dump} for the supplied info.
     */
    public String dumpString () {
        String result = "";
        String[] jobDump = dump();
        for (String s : jobDump) {
            result += s + "\n";
        }
        return result;
    }
}



