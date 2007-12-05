/* $Id: WECTest.java,v 1.4 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/04 13:28:17 $
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
package dk.statsbiblioteket.summa.test;

import junit.framework.TestCase;

import java.rmi.registry.Registry;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;

import dk.statsbiblioteket.summa.dice.*;
import dk.statsbiblioteket.summa.dice.rmigz.GZIPSocketFactory;
import dk.statsbiblioteket.summa.dice.util.RegistryManager;
import dk.statsbiblioteket.summa.dice.util.DiceFactory;
import dk.statsbiblioteket.summa.dice.Config;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 30/08/2006
 * Time: 14:11:00
 * To change this template use File | Settings | File Templates.
 */
public class WECTest extends TestCase {

    private static final Logger log = Logger.getLogger("dk.statsbiblioteket.summa");

    EmployerBase employer;
    ConsumerBase consumer;
    Worker worker;

    public WECTest () {
        super();
    }

    public void setUp () throws Exception {

    }

    public void testPrimeWEC () throws Exception {
        log.info ("Setting up WEC classes");
        Config conf = new PrimeConfig();
        DiceFactory wec = new DiceFactory(conf);
        Registry reg = RegistryManager.getRegistry(conf);

        employer = wec.newEmployer();
        employer.run();
        //new Thread(employer).start();
        Thread.sleep(2000);

        System.out.println ("Known services:");
        for (String s: reg.list()) {
            System.out.println ("\t - " + s);
        }

        consumer = wec.newConsumer();
        consumer.run();
        //new Thread(consumer).start();

        Thread.sleep(1000);
        System.out.println ("Known services:");
        for (String s: reg.list()) {
            System.out.println ("\t - " + s);
        }

        worker = wec.newWorker ();
        worker.run();
    }

    public void testCachingWEC () throws Exception {
        log.info ("Setting up WEC classes");
        Config conf = new TestConfig();
        DiceFactory wec = new DiceFactory(conf);
        Registry reg = RegistryManager.getRegistry(conf);

        employer = wec.newEmployer();
        employer.run();
        //new Thread(employer).start();
        //Thread.sleep(2000);

        /*System.out.println ("Known services:");
        for (String s: reg.list()) {
            System.out.println ("\t - " + s);
        }*/

        consumer = wec.newConsumer();
        consumer.run();
        //new Thread(consumer).start();

        //Thread.sleep(1000);
        System.out.println ("Known services:");
        for (String s: reg.list()) {
            System.out.println ("\t - " + s);
        }

        worker = wec.newWorker ();
        worker.run();
    }

    public void testGZIPSockets () throws Exception {
        final GZIPSocketFactory fact = new GZIPSocketFactory();

        Runnable server =  new Runnable () {
            public void run () {
                ServerSocket serverSocket = null;
                Socket conn = null;
                BufferedReader in = null;
                try {
                    serverSocket = fact.createServerSocket(2768);


                    System.out.println ("Server: Awaiting connection");
                    conn = serverSocket.accept();
                    System.out.println ("Server: Got connection");

                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    byte[] buf = new byte[64];
                    for (int i = 0; i < 2; i++) {
                        System.out.println ("Server: Reading input...");
                        int numRead = conn.getInputStream().read(buf);
                        System.out.println (numRead);
                        //System.out.println ("Server: got \"" + in.readLine() + "\"");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit (1);
                }


            }
        };

        Runnable client = new Runnable () {
            public void run () {
                System.out.println ("Client: connecting to server");
                Socket clientSocket = null;
                try {
                    clientSocket = fact.createSocket("pc134", 2768);

                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                    String message = "Hello world!";
                    System.out.println ("Client: sending \"" + message + "\"...");
                    out.println (message);
                    //((GZIPOutputStream)clientSocket.getOutputStream()).flush();
                    System.out.println ("Client: message send");
                    out.flush();

                    //((GZIPOutputStream)clientSocket.getOutputStream()).finish();
                    Thread.sleep (1000);


                    message = "Hello again!";
                    System.out.println ("Client: sending \"" + message + "\"...");
                    out.println (message);
                    //((GZIPOutputStream)clientSocket.getOutputStream()).flush();
                    System.out.println ("Client: message send");

                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        };

        System.out.println ("Starting server");
        new Thread (server).start();

        System.out.println ("Starting client");
        new Thread (client).start();

        Thread.sleep (10000);
    }

    public void testConfig () throws Exception {
        Config conf = new Config();
        conf.setDefaults();
        System.out.println ("Dumping WEConf defaults:\n");
        for (String s : conf.dump()) {
            System.out.println (s);
        }
    }
}
