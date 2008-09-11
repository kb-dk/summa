/* $Id: PrimeDriver.java,v 1.3 2007/10/04 13:28:20 te Exp $
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

import dk.statsbiblioteket.summa.dice.util.DiceFactory;
import dk.statsbiblioteket.summa.dice.Config;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 4/09/2006
 * Time: 09:29:02
 * Main class for running WEC tests. It can be used to start a worker, employer, or consumer.
 */
public class PrimeDriver {

    private static String mode;

    /**
     * Main method running either a worker, employer, or consumer
     * @param args array of length 1, containing "worker", "employer", or "consumer".
     */
    public static void main (String[] args) throws Exception {

        parseArgs (args);

        Config conf = new PrimeConfig();
        DiceFactory wec = new DiceFactory(conf);

        // We run the target in the main thread,
        // by simply calling their run method directly
        try {
            if (mode.equals("worker")) {
                wec.newWorker().run();
            } else if (mode.equals ("employer")) {
                wec.newEmployer().run();
            } else if (mode.equals ("consumer")) {
                wec.newConsumer().run ();
            }
        } catch (Exception  e) {
            System.out.println (conf.dumpString());
            throw e;
        }

    }

    private static void parseArgs (String[] args) {
        if (args.length != 1) {
            printUsage();
            System.exit (1);
        }

        mode = args[0];
        if (mode.equals("worker") ||
            mode.equals("employer") ||
            mode.equals("consumer")) {
            // all is good
            return;
        }
        else {
            System.err.println ("Unknown option: " + mode);
            System.err.println ("The command line argument must be one of worker, employer, or consumer.");
            System.exit (1);
        }
    }

    private static void printUsage () {
        System.out.println ("USAGE:\n\twectest <worker,consumer,employer>");
    }

}



