/* $Id: WritableStorage.java,v 1.6 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.6 $
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
package dk.statsbiblioteket.summa.storage.api;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.IOException;
import java.util.List;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface WritableStorage extends Configurable {

    /**
     * Flush a record to the storage. In other words write it.
     * <p/>
     * Any nested child records (added via {@link Record#setChildren}) will
     * be added recursively to the storage.
     *
     * @param record The record to store or update
     * @throws IOException on comminication errors
     */
    void flush(Record record) throws IOException;

    /**
     * A batch optimized version of {@link #flush}. Use this method
     * to optimize IPC overhead. If two Record objects in {@code records} has
     * the same id only the last record in the list is stored.
     * <p/>
     * Just like {@link #flush} any child records added with
     * {@link Record#setChildren} will be added recursively to the storage. 
     *
     * @param records a list of records to store or update. On duplicate ids
     *                only the last of the duplicated records are stored
     * @throws IOException on communication errors
     */
    void flushAll(List<Record> records) throws IOException;

    /**
     * Close the storage for any further reads or writes.
     * When this method returns it should be safe to turn of the JVM in which
     * storage runs.
     *
     * @throws IOException on communication errors
     */
    void close() throws IOException;

    /**
     * Mark all records in a given base as deleted.
     *
     * @param base the name of the base to clear
     * @throws IOException on communication errors with the storage
     */
    void clearBase (String base) throws IOException;

    /**
     * Run a scripted batch job across a subset of the storage.
     * Any {@link javax.script.ScriptEngine} supported by the JVM can be used
     * as a scripting backend. See
     * <a href="https://scripting.dev.java.net/">scripting.dev.java.net</a>
     * for a full list of scripting engines for the Java platform. The script
     * code itself must be available as a resource on the classpath of the
     * storage process.
     * <p/>
     * The script environment will have a number of variables loaded on
     * execution time. These are
     * <ul>
     *   <li><tt>record</tt> - The {@link Record} object to operate on</li>
     *   <li><tt>log</tt> - A Commons-Logging
     *                      {@link org.apache.commons.logging.Log} object</li>
     *   <li><tt>base</tt> - The name of the base being operated on
     *                       - possibly {@code null} when iterating over all
     *                         bases</li>
     *   <li><tt>minMtime</tt> - The minimum modification time for the records
     *                           in the batch</li>
     *   <li><tt>maxMtime</tt> - The maximum modification time for the records
     *                           in the batch</li>
     *   <li><tt>options</tt> - A {@link QueryOptions} instance</li>
     *   <li><tt>state</tt> - A private variable to be used by the script to hold
     *                        arbitrary state in between invocations. This variable
     *                        is initialized to {@code null} by the runtime</li>
     *   <li><tt>out</tt> - A {@link StringBuilder} collecting any output to return
     *                      from the script</li>
     *   <li><tt>commit</tt> - A boolean flag that must be set to {@code true}
     *                         if the script wants to commit any changes it has
     *                         done to {@code record}. This variable will be set to
     *                         {@code false} prior to each evaluation of the script
     *                         </li>
     *   <li><tt>first</tt> - A boolean flag which is {@code true} on the first
     *                        invocation of the script. Always {@code false}
     *                        otherwise</li>
     *   <li><tt>last</tt> - A boolean flag which is {@code true} when the last
     *                       record in the batch is reached. Always {@code false}
     *                       otherwise</li>
     * </ul>
     *
     * @param jobName The name of the job to instantiate.
     *                The job name must match the regular expression
     *                {@code [a-zA-z_-]+.job.[a-zA-z_-]+} and correspond to a
     *                resource in the classpath of the storage process
     * @param base Restrict the batch jobs to records in this base. If
     *             {@code base} is {@code null} the records from all bases will
     *             be included in the batch job
     * @param minMtime Only records with modification times greater than
     *                 {@code minMtime} will be included in the batch job
     * @param maxMtime Only records with modification times less than
     *                 {@code maxMtime} will be included in the batch job
     * @param options Restrict to records for which
     *                {@link QueryOptions#allowsRecord} returns true
     * @throws IOException if there is an error loading the script source code,
     *                     parsing, or evaluating the script
     */
    String batchJob(String jobName, String base,
                    long minMtime, long maxMtime, QueryOptions options)
                                                             throws IOException;
}



