/* $Id: PrimeConsumer.java,v 1.3 2007/10/04 13:28:20 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:20 $
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

import org.apache.log4j.Logger;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.dice.Config;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 1/09/2006
 * Time: 15:06:07
 * To change this template use File | Settings | File Templates.
 */
public class PrimeConsumer extends CachingConsumer {

    private static final Logger log = Logger.getLogger(Consumer.class);

    String finalFilename = "/home/mikkel/tmp/primes.txt";
    String partFileNamePattern = "/home/mikkel/tmp/part.primes.";
    SortedSet<Integer> doneJobs;

    public PrimeConsumer (Config conf) throws RemoteException {
        super (conf);
        doneJobs = new TreeSet<Integer>();
    }

    /**
     * Write job data member to partFileNamePattern+job.getName()
     */
    public void processJob (Job job) {

        File outputFile = new File (partFileNamePattern+job.getName());
        outputFile.delete();

        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            log.fatal ("Failed to create output file: " + outputFile, e);
            return;
        }

        log.info ("Writing output file: " + outputFile);

        PrintWriter out = null;

        try {
            out = new PrintWriter(outputFile);
        } catch (FileNotFoundException e) {
            log.fatal ("Output file not found: " + outputFile, e);
            return;
        }

        Iterator parts = ((Iterable)job.getData()).iterator();        

        while (parts.hasNext()){
            out.println(parts.next());
        }
        out.flush();
        out.close();

        doneJobs.add (new Integer(job.getName()));
    }

    /**
     * Merge all partFileNamePattern* into one file
     */
    public void close () {
        File outputFile = new File (finalFilename);
        outputFile.delete();

        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            log.fatal ("Failed to create output file: " + outputFile, e);
            return;
        }

        log.info ("Writing final output file: " + outputFile);

        FileWriter out = null;

        try {
            out = new FileWriter(outputFile);
        } catch (Exception e) {
            log.fatal ("Output file not found: " + outputFile, e);
            return;
        }

        for (Object jobName : doneJobs) {
            // Create a FileReader for partial output file
            String filename = partFileNamePattern+jobName.toString();
            log.info ("Merging file: " + filename);
            FileReader reader = null;
            try {
                reader = new FileReader(filename);
            } catch (FileNotFoundException e) {
                log.fatal ("Unable to find partial output file: " + filename, e);
                return;
            }

            // Read partial output and write to final output
            int bufSize = 1024;
            char buf[] = new char[bufSize];
            int offset = 0;
            int numRead = 0;
            try {
                do {
                    numRead = reader.read (buf,offset,bufSize);
                    out.write(buf, 0, numRead);
                    out.flush();
                } while (numRead == bufSize);
            } catch (IOException e) {
                log.error (e);
            }

        }
        try {
            out.close();
        } catch (IOException e) {
            log.error (e);
        }


    }
}

