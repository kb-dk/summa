/* $Id: DiceFactory.java,v 1.4 2007/10/04 13:28:17 te Exp $
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
package dk.statsbiblioteket.summa.dice.util;

import dk.statsbiblioteket.summa.dice.*;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.registry.Registry;
import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Create instances of actual implementations of {@link Worker}, {@link EmployerBase},
 * and {@link ConsumerBase} classes. Class constructors must conform to specific
 * signatires as described in {@link #DiceFactory}
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class DiceFactory {

    private static final Log log = LogFactory.getLog(DiceFactory.class);

    private Config conf;

    /**
     * Create a new WECFactory configured for creating instances of the classes
     * given in the constructor.
     * The employerClass, consumerClass, and workerClass of config must have a
     * constructor matching the signature:
     * <code>constructor (Config conf)</code>,
     * or else an IllegalArgumentException is thrown.
     * @param config
     */
    public DiceFactory(Config config) {
        this.conf = config;
        validateConstructors();
    }

    /**
     * Return a new instance of the registered {@link EmployerBase} implementation.
     * @return a new instance of a EmployerBase implementation
     */
    public EmployerBase newEmployer () {
         try {
            // We don't have to check that con!=null since this is checked
            // in the constructor
            Constructor con = getConstructor(conf.getEmployerClass());
            EmployerBase employer = (EmployerBase) con.newInstance(conf);
            return employer;
        } catch (Exception e) {
            log.error ("Failed to instantiate Employer", e);
            throw new RuntimeException("Failed to instantiate Employer", e);
        }
    }

    public Employer lookupEmployer () throws RemoteException {
        Registry reg = null;
        try {
            reg = RegistryManager.getRemoteRegistry(conf.getEmployerHostname(), conf.getRegistryPort(), conf);
        } catch (RemoteException e) {
            throw new RemoteException ("Failed to find remote registry", e);
        }

        try {
            return (Employer) reg.lookup (conf.getEmployerServiceName());
        } catch (NotBoundException e) {
            log.error ("Failed to look up Employer",e);
            throw new RemoteException("Failed to look up Employer", e);
        }
    }

    /**
     * Return a new instance of the registered {@link ConsumerBase} implementation.
     * @return a new instance of a ConsumerBase implementation
     */
    public ConsumerBase newConsumer () {
        try {
            // We don't have to check that con!=null since this is checked
            // in the constructor
            Constructor con = getConstructor(conf.getConsumerClass());
            ConsumerBase consumer = (ConsumerBase) con.newInstance(conf);
            return consumer;
        } catch (Exception e) {
            log.error ("Failed to instantiate Consumer", e);
            throw new RuntimeException("Failed to instantiate Consumer", e);
        }
    }

    public Consumer lookupConsumer () throws RemoteException {
        Registry reg = null;
        try {
            reg = RegistryManager.getRemoteRegistry(conf.getConsumerHostname(), conf.getRegistryPort(), conf);
        } catch (RemoteException e) {
            throw new RemoteException ("Failed to find remote registry", e);
        }
        try {
            return (Consumer) reg.lookup (conf.getConsumerServiceName());
        } catch (NotBoundException e) {
            log.error ("Failed to look up Consumer",e);
            throw new RemoteException("Failed to look up Consumer", e);
        }
    }

    /**
     * Return a new instance of the registered {@link Worker} implementation.
     * @return a new instance of a Worker implementation
     */
    public Worker newWorker () {
          try {
            // We don't have to check that con!=null since this is checked
            // in the constructor
            Constructor con = getConstructor(conf.getWorkerClass());

            Worker worker = (Worker) con.newInstance(conf);
            return worker;
        } catch (Exception e) {
            log.error ("Failed to instantiate Worker", e);
            throw new RuntimeException("Failed to instantiate Worker", e);
        }
    }

    /**
     * Check if a given class has a constructor matching
     * <code>constructor (int, int, RMIClientSocketFactory, RMIServerSocketFactory)</code>
     * signature, and return it if so. Other wise return null.
     * @param c the class to examine
     * @return class constructor with described signature or null if no such constructor exist
     */
    private Constructor getConstructor (Class c) {
        // Is the requested class a subclass of EmployerBase or ConsumerBase:
        if (EmployerBase.class.isAssignableFrom(c)) {
            try {
                Constructor con = c.getConstructor(Config.class);
                return con;
            } catch (NoSuchMethodException e) {
                return null;
            }
        } else if (ConsumerBase.class.isAssignableFrom(c)) {
            try {
                Constructor con = c.getConstructor(Config.class);
                return con;
            } catch (NoSuchMethodException e) {
                return null;
            }

        } else if (Worker.class.isAssignableFrom(c)) {
            try{
                Constructor con = c.getConstructor(Config.class);
                return con;
            } catch (NoSuchMethodException e) {
                return null;
            }
        }

        // The requested class is not a subclass of EmployerBase, ConsumerBase or Worker
        return null;
    }

    /**
     * Throw an IllegalArgumentsException if the one of the constrcutors for one of
     * the registered classes is not found.
     */
    private void validateConstructors () {
        Constructor con = getConstructor (conf.getEmployerClass());
        if (con == null) {
            throw new IllegalArgumentException("No qualifying constructor for " + conf.getEmployerClass());
        }
        con = getConstructor(conf.getConsumerClass());
        if (con == null) {
            throw new IllegalArgumentException("No qualifying constructor for " + conf.getConsumerClass());
        }
        con = getConstructor(conf.getWorkerClass());
        if (con == null) {
            throw new IllegalArgumentException("No qualifying constructor for " + conf.getWorkerClass());
        }
    }

}



