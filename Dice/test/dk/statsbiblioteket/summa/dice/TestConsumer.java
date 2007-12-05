/* $Id: TestConsumer.java,v 1.3 2007/10/04 13:28:20 te Exp $
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.io.*;
import java.rmi.RemoteException;

import dk.statsbiblioteket.summa.dice.Config;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 30/08/2006
 * Time: 13:02:10
 * To change this template use File | Settings | File Templates.
 */
public class TestConsumer extends CachingConsumer {

    private static final Logger log = Logger.getLogger(Consumer.class);

    ArrayList output;
    File outputFile;
    String outputFilename = "/home/mikkel/tmp/test_output";

    public TestConsumer (Config conf) throws RemoteException {
        super (conf);
        output = new ArrayList();
    }

    public void processJob (Job job) {
        Iterator data = ((Iterable) job.getData()).iterator();
        while(data.hasNext()) {
            output.add (data.next());
        }
    }

    public void close () {
        outputFile = new File (outputFilename);
        outputFile.delete();
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            log.fatal ("Failed to create output file: " + outputFilename, e);
            return;
        }

        log.info ("Writing output file: " + outputFile);

        PrintWriter out = null;

        try {
            out = new PrintWriter(outputFile);
        } catch (FileNotFoundException e) {
            log.fatal ("Output file not found: " + outputFilename, e);
            return;
        }

        Object data[] = output.toArray();
        Arrays.sort(data);

        for (Object o : data){
             out.println(o);
        }
        out.flush();
        out.close();

    }
}
