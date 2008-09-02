/* $Id: ConsumerBase.java,v 1.4 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.4 $
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

import dk.statsbiblioteket.summa.dice.util.RegistryManager;
import dk.statsbiblioteket.summa.dice.util.DiceFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract base class to for {@link Consumer} implementations.
 * </p><p>
 * To start a Consumer based on this class call the {@link #run}
 * method, or put this {@link Runnable} in a {@link Thread} and call {@link Thread#start}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
abstract public class ConsumerBase extends UnicastRemoteObject
                                   implements Consumer, Runnable {

    protected static final Log log = LogFactory.getLog(Consumer.class);

    private ArrayBlockingQueue<Job> jobQueue;
    private boolean compressedIO;
    private boolean stop;
    Thread dataProcessor;
    Employer employer;
    Config conf;
    String hostname;

    private class DataProcessor implements Runnable {

        public void run () {
            log.info ("Starting data processor");
            while (!stop) {

                try {
                    if (employer.isFinished()) {
                        log.info("Employer reports finished.");
                        stop = true;
                        break;
                    }
                } catch (RemoteException e) {
                    log.error("Failed to retrieve isFinished status from Employer. Sleeping.", e);
                    sleep();
                    continue;
                }

                Job job = job = nextJob();
                if (job == null) {
                    log.warn ("Got null job. Sleeping.");
                    sleep();
                    continue;
                }


                log.info ("Processing job " + job);
                setJobStatus(new JobStatus(job, JobStatus.Status.JOB_CONSUMING));
                processJob(job);
                setJobStatus(new JobStatus(job, JobStatus.Status.JOB_CONSUMED));
            }
            log.info ("Data processing finished. Closing.");
            close();

        }
    }

    /**
     *
     * @param conf
     * @throws RemoteException
     */
    protected ConsumerBase (Config conf)
                            throws RemoteException {
        super(conf.getConsumerPort(), conf.getClientSocketFactory(), conf.getServerSocketFactory());
        this.conf = conf;

        // Find hostname of the Consumer
        try {
            java.net.InetAddress localMachine =
                    java.net.InetAddress.getLocalHost();
            hostname = localMachine.getHostName();
            if (!hostname.equals (conf.getConsumerHostname())) {
                log.warn ("Configured consumer hostname \"" + conf.getConsumerHostname() + "\"doesn't match actual consumer hostname \"" + hostname + "\"");
            }
        } catch (java.net.UnknownHostException uhe) {
            throw new RuntimeException ("Unable to obtain Consumer hostname.", uhe);
        }

        stop = false;
        jobQueue = new ArrayBlockingQueue<Job>(conf.getConsumerQueueSize());

        dataProcessor = new Thread (new DataProcessor(), "DataProcessor");
        dataProcessor.setPriority(Thread.MIN_PRIORITY); // TODO is it necessary to set low prio?

        try {
            DiceFactory fact = new DiceFactory(conf);
            //employer = (Employer) Naming.lookup(employerAddress);
            //employer = (Employer) RegistryManager.lookup (employerAddress);
            employer = fact.lookupEmployer();
        } catch (RemoteException e) {
            log.error ("Failed to lookup Employer", e);
            throw new RuntimeException("Failed to lookup Employer", e);
        }
    }

    protected void sleep () {
        try {
            Thread.sleep (5000);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    abstract protected void processJob (Job job);

    /**
     * Called when all jobs have been registered JobStatus.Status.JOB_CONSUMED
     * on the {@link Employer}.
     */
    abstract public void close ();

    protected Employer getEmployer () {
        return employer;
    }

    /**
     * Get the next job from the job queue.
     */
    protected Job nextJob () {
        try {
            return jobQueue.take();
        } catch (InterruptedException e) {
            log.warn ("nextJob interrupted, returning null.");
            return null;
        }
    }

    public synchronized void consumeJob (Job job) {
        try {
            if (jobQueue.contains(job)) {
                log.warn ("Job " + job + " already enqueued. Skipping.");
                return;
            }
            setJobStatus(new JobStatus (job, JobStatus.Status.JOB_CONSUMER_ENQUEUED));
            log.debug ("Consuming job: " + job);
            jobQueue.put(job);
        } catch (InterruptedException e) {
            // DO nothing
        }
    }

    protected final void setJobStatus (JobStatus status) {
        int tries = 0;
        while (tries < 10) {
            try {
                employer.setJobStatus(status, hostname);
                return;
            } catch (RemoteException e) {
                log.error ("Failed to report status to employer. Sleeping.", e);
                sleep();
                tries++;
                continue;
            }
        }
        log.fatal ("Unable to contact Employer in " + tries + " attempts. Bailing out.");
        stop();
        System.exit(1);
    }

    /**
     * An actual implementation of a Consumer should call this method
     * to stop the data processing thread properly. Typically from
     * the {@link #close} method.
     */
    public void stop () {
        log.info ("Stopping data processor");
        this.stop = true;
        dataProcessor.interrupt();
        try {
            dataProcessor.join();
        } catch (InterruptedException e) {
            // Do nothing
        }
    }

    /**
     * Bind this Consumer in the registry. This call blocks, so you might
     * want to embed the ConsumerBase in a Thread (it is a Runnable).
     */
    public final void run () {
        try {
            System.setSecurityManager(new RMISecurityManager());
            RegistryManager.getRegistry(conf).rebind (conf.getConsumerServiceName(), this);
            log.info ("Consumer bound as " + conf.getConsumerServiceName() + " on port " + conf.getConsumerPort());
            dataProcessor.start();
        } catch (RemoteException e) {
            log.error (e);
        }
    }

}
