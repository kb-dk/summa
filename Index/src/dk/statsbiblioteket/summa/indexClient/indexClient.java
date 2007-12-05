/* $Id: indexClient.java,v 1.6 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.6 $
 * $Date: 2007/10/05 10:20:24 $
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
package dk.statsbiblioteket.summa.indexClient;

import dk.statsbiblioteket.summa.index.IndexService;

import dk.statsbiblioteket.summa.tools.monitor.Monitor;
import dk.statsbiblioteket.summa.tools.monitor.RemoteMonitor.IteratableSourceIndexMonitor;
import dk.statsbiblioteket.summa.tools.schedule.Scheduler;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.util.qa.QAInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;

/**
 * The client application for the IndexService of DIVA.
 * The ClientMBean is used to set up a periodic scheduled FileSystemMonitor with handlers for adding, deleteing and changing XML files to the index.
 * Initially the client can be called so that a index is created from start.
 * @deprecated
 */

@QAInfo(level = QAInfo.Level.NOT_NEEDED,
        state = QAInfo.State.UNDEFINED,
        author = "hal")
public class indexClient {


    /**
     * A Log for logging.
     */
    private static Log log = LogFactory.getLog("dk.statsbiblioteket.summa.indexClient");

    /**
     * Main function, starts the client.
     *
     * @param args The arguments to the client should be: SourceDirectoryPath, recursiveSource(YES/NO), XSLTFilePath, FileMonitorStart(int seconds), MonitorPeriod(int seconds), RMIServer, ServiceName, StartWithFullIndex(YES/NO), indexPath(optional)
     */
    public static void main(final String[] args) {
        log.info("Starting indexClient");

        final String newLine = "\n";

        if (args.length != 8) {
            promptUsageExit();
        }

        final String XSLTFilePath = args[0];
        final String fileMonitorStart = args[1];
        final String monitorPeriod = args[2];
        final String RMIserver = args[3];
        final String RMIport = args[4];
        final String serviceName = args[5];
        final String indexPath = args[6];
        final String resume = args[7];

        log.debug("ClientMBean started with the following parameters:" + newLine
                  + "XslFile: " + XSLTFilePath + newLine
                  + "FileSystemMonitorLatency:" + fileMonitorStart + newLine
                  + "FileSystemMonitorInterval:" + monitorPeriod + newLine
                  + "RMIServer:" + RMIserver + newLine
                  + "ServiceName: " + serviceName + newLine
                  + "IndexPath:" + indexPath + newLine);

        Monitor mon = IteratableSourceIndexMonitor.getInstance();
        IndexService indexer = null;

        try {
            System.setSecurityManager(new RMISecurityManager());
            log.debug("Activate the IndexService on :"  + RMIserver);
            indexer = (IndexService) Naming.lookup("//" + RMIserver + ":" + RMIport + "/" + serviceName);
        } catch (MalformedURLException e) {
            log.fatal(e);
            System.exit(-1);
        } catch (NotBoundException e) {
            log.fatal(e);
            System.exit(-1);
        } catch (RemoteException e) {
            log.fatal(e);
            System.exit(-1);
        }


        try {
        if (!"NO".equals(resume)){
                indexer.startIndex(indexPath);
        } else {
            indexer.resumeIndex(indexPath);
        }
        } catch (RemoteException e) {
            log.fatal(e);
        System.exit(-1);
        } catch (IndexServiceException e) {
            log.fatal(e);
        System.exit(-1);
        }

       // try {
       //     log.info("Setting xslt url to: " + XSLTFilePath);
            //indexer.(XSLTFilePath);
        //} catch (RemoteException e) {
        //    log.error(e);
       // }

        log.info("Starting the scheduler for monitoring");
        new Scheduler().start(mon, intValue(fileMonitorStart), intValue(monitorPeriod));

        log.info("indexClient up and running");

    }


    private static void removePersistentObjects(final String monitorStorePath) {
        // clean up old serialized objects
        File serialize_dir = new File(monitorStorePath);
        File[] files = serialize_dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile() && (file.getName().endsWith(".obj") || file.getName().endsWith(".instance"))) {
                if (!file.delete()) {
                    log.fatal("Unable to clean up files in " + monitorStorePath);
                    System.exit(-1);
                }
            }
        }
    }


    /**
     * Evaluates a String as boolean in YES/NO syntax.
     *
     * @param arg any String
     *
     * @return true if and only if arg is YES
     */
    private static boolean boolValue(final String arg) {
        return "YES".equals(arg);
    }


    /**
     * Determines whether the argument follows the YES/NO syntax.
     *
     * @param arg any String
     *
     * @return true if the String is either YES or NO
     */
    private static boolean isBoolean(final String arg) {
        return ("YES".equals(arg) || "NO".equals(arg));
    }


    /**
     * Converts a String to int, application will exit if the conversion fails.
     *
     * @param arg String representation of a integer
     *
     * @return the int value of the String
     */
    private static int intValue(final String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            promptUsageExit();
        }
        return 0;
    }

    /**
     * Determines whether the arg is a valid file path.
     *
     * @param arg filePath
     *
     * @return true if arg is a file path.
     */
    private static boolean isFile(final String arg) {
        final File f = new File(arg);
        return (f.exists() && f.isFile());
    }

    /**
     * Determines whether the arg is a valid directory path.
     *
     * @param arg
     *
     * @return true if arg is a directory path.
     */
    private static boolean isDirectory(final String arg) {
        final File f = new File(arg);
        return ((f.exists() && f.isDirectory()));
    }

    /**
     * Sends a usage message to standard out - and exit.
     */
    private static void promptUsageExit() {
        System.out.println("Syntax error USAGE: indexClient " +
                           "XSLTFilePath, " +
                           "FileMonitorStart(int seconds), " +
                           "MonitorPeriod(int seconds), " +
                           "RMIServer, " +
                           "RMIPort , " +
                           "ServiceName, " +
                           "indexPath" +
                           "Resume(YES/NO), "
                           );
        System.exit(1);
    }
}
