/* $Id: Digester.java,v 1.8 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.8 $
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

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.io.IOException;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A Digester is responsible for handling the traversal of targets for ingesting.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "hal")
public abstract class Digester {

    protected ExecutorService ser;
    // todo : this should be configurable
    public static final int MaxThreadPool = 3;

    //todo: this should be configurable
    public static final int INITIAL_THREADPOOL_SIZE = 3;

    // todo: this should be configurable
    public static final int QUEUE_SIZE= 5000;

    static Log log = LogFactory.getLog(Digester.class);

    protected Target target;
    protected Ingest in;


    /**
     * Constructs a new Digester.
     */
    public Digester() {
      ser = new ThreadPoolExecutor(INITIAL_THREADPOOL_SIZE,
                                    MaxThreadPool,
                                    Long.MAX_VALUE,
                                    TimeUnit.MILLISECONDS,
                                    new ArrayBlockingQueue<Runnable>(QUEUE_SIZE),
                                    new ThreadPoolExecutor.CallerRunsPolicy());
      Progress.reset();

    }

    /**
     * Recursivelly digest a directory.<br>
     * Digesting is a process where all meta-data xml files will be parsed, records extracted and comitted to Storage.
     *
     * @param directory                 the current directory in the recursive tree.
     * @throws IOException              thrown if the argument is not a directory or if the digester has
     *                                  insuffiecent privilliges to the files
     * @throws RecordFormatException    if something is bad in the medata-data.
     */
    protected void digest(File directory) throws IOException, RecordFormatException{
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()){
                digest(file);
            } else if (file.getName().endsWith(".ingest")){
                 log.debug("processing file:" + file.getAbsolutePath());
                 if (isParsable(file)) {
                     ser.submit(createParser(file));
                 }
            }
        }
    }

    /**
     * Examines a file to check if it is parseable.<br>
     * This can be owerwritten to make special conditions for some xml files in the directory that shold not be parsed.
     * @param f         the file to examine
     * @return          true if the file should be parsed through a {@link dk.statsbiblioteket.summa.ingest.IngestContentHandler}
     */
    protected boolean isParsable(File f){
        return true;
    }

    /**
     * ParserTask factory method. implementing classes needs to instanciate a propper ParserTask.
     *
     * @param f         the file that will be parsed.
     * @return          the {@link dk.statsbiblioteket.summa.ingest.ParserTask} used to parse the file.
     */
    protected abstract ParserTask createParser(File f);


    /**
     * Digest a target using the Ingest given.<br>
     * The digest process in some cases produce a different {@link dk.statsbiblioteket.summa.ingest.Target}
     * that given to the metod.<br>
     * This can be checked by examine the target returned by the method:<br>
     * <code>
     *          Digester digester = new ....
     *          Target target = new Target(....
     *          Ingest in = new Ingest(....
     *          Target processedTarget = digester.digest(target,in);
     *          if (target.equals(processedTarget)){
     *              // target the same, no targetinfo found during digest
     *          } else {
     *             // digest has found targetinfo
     *
     *          }
     *          if (target.equals(digester.digest(target
     * </code>
     *
     * @param target        the target to digest.
     * @param in            the Ingest to use.
     * @return              the processed target.
     */
    public Target digest(Target target, Ingest in)  {
        this.target = target;
        this.in = in;
        File f = new File(target.getDirectory());
        if (!f.isDirectory()){
            log.error(target.getDirectory() + " is not directory");
            throw new IllegalArgumentException(target.getDirectory() + " is not directory");
        }
        try {
            digest(f);
        } catch (IOException e) {
            log.error(e);
        } catch (RecordFormatException e) {
            log.error(e);
        }
        return target;
    }
}
