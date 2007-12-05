/* $Id: Driver.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.index.dice;

import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.summa.dice.util.DiceFactory;
import dk.statsbiblioteket.summa.index.IndexManipulationUtils;
import dk.statsbiblioteket.util.qa.QAInfo;


@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Driver {

    private static String mode;

    /**
     * Main method running either a worker, employer, or consumer
     * @param args array of length 1, containing "worker", "employer", or "consumer".
     */
    public static void main (String[] args) throws Exception {

        parseArgs (args);

        Config conf = new IndexConfig();
        DiceFactory dice = new DiceFactory(conf);

        if (System.getProperty("worker.role") != null) {
            conf.set ("worker.role", System.getProperty("worker.role"));
        }

        // We run the target in the main thread,
        // by simply calling their run method directly
        try {
            if (mode.equals("worker")) {
                dice.newWorker().run();
            } else if (mode.equals ("employer")) {
                dice.newEmployer().run();
            } else if (mode.equals ("consumer")) {
                dice.newConsumer().run ();
            } else if (mode.equals ("merge")) {
                String[] newArgs = new String[args.length - 1];
                for (int i = 1; i < args.length; i++) {
                    newArgs[i-1] = args[i];
                }
                IndexManipulationUtils.fullMerge(newArgs);
            }
        } catch (Exception  e) {
            System.out.println (conf.dumpString());
            throw e;
        }

    }

    private static void parseArgs (String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit (1);
        }

        mode = args[0];
        if (mode.equals("worker") ||
            mode.equals("employer") ||
            mode.equals("consumer") ||
            mode.equals("merge")) {
            // all is good
            return;
        }
        else {
            System.err.println ("Unknown option: " + mode);
            System.err.println ("The command line argument must be one of worker, employer, consumer or merge.");
            System.exit (1);
        }
    }

    private static void printUsage () {
        System.out.println ("USAGE:\n\tdicetest <worker,consumer,employer,merge>");
        System.out.println ("\n\tworker\t - start a new worker");
        System.out.println ("\temployer\t - start a new indexer");
        System.out.println ("\tconsumer\t - start a new merger");
        System.out.println ("\tmerge\t - merge indexes into one index (run without args for usage)");
    }

}

