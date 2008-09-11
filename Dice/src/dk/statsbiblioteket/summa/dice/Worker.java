/* $Id: Worker.java,v 1.4 2007/10/04 13:28:19 te Exp $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.rmi.RemoteException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Random;
import java.io.IOException;

import dk.statsbiblioteket.summa.dice.util.DiceFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Abstract base class to ease implementation of cluster slaves. Note that the {@link Employer}
 * or {@link Consumer} doesn't depend on any specific api from the workers/slaves so you are
 * free to use any implementation you like.
 *
 * TODO: We prefetch jobs in a thread, - we might also upload jobs to consumer in another thread
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
abstract public class Worker implements Runnable {

    protected static final Log log = LogFactory.getLog(Worker.class);

    private String hostname;
    private Employer employer;
    private Consumer consumer;
    private Thread jobLoaderThread;
    private BufferingJobLoader jobLoader;
    private boolean stop;
    private boolean keepAlive;
    private Thread mainLoop;
    Config conf;
    private ArrayBlockingQueue<Job> jobQueue;
    private Random random;

    /**
     * Poison object for the job queue
     */
    protected class NoJob extends Job {
        public NoJob() {
            super(null, null, "NoJob");
        }
    }

    /**
     * A simple local Job buffer for the Worker.
     * Initialize with bufferSize=0 to disable buffering.
     */
    private class BufferingJobLoader implements Runnable {

        public void run () {
            log.info ("Starting BufferingJobLoader");
            if (conf.getWorkerQueueSize() == 0) {
                // We don't buffer. Stop the thread.
                log.info ("Worker is running without Job buffer");
                return;
            } else {
                log.info ("Worker job buffer size: " + conf.getWorkerQueueSize());
            }

            // Main loop
            // Note: We never run this if bufferSize==0
            int failedRuns = 0;
            while (!stop) {
                Job job = null;
                try {
                    while (jobQueue.remainingCapacity() == 0) {
                        sleep(1000);
                    }

                    job = employer.getJob(hostname);

                    if (job == null) {
                        log.info ("Got null Job, stopping BufferingJobLoader");
                        stop = true;
                        job = new NoJob();
                        break;
                    }

                    /*if (knownJobs.contains(job.getName())) {
                        try {
                            log.info("Got old job. Sleep.");
                            Thread.sleep (10000);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }
                        continue;
                    } else {
                        knownJobs.add (job.getName());
                    }*/

                    if ( !(job instanceof NoJob) ) {
                        setJobStatus(new JobStatus (job, JobStatus.Status.JOB_WORKER_ENQUEUED));
                    }

                    bufferJob (job);

                } catch (RemoteException e) {
                    log.error ("Error getting job from employer. Stopping Worker.", e);
                    stop = true;
                    break;
                } catch (JobFailedException e) {
                    log.error ("Job " + job + " failed. " + failedRuns + " consecutive errors.", e);
                    setJobStatus(new JobStatus(job, JobStatus.Status.JOB_FAILED));
                    sleep(30000*failedRuns + random.nextInt(1000));
                    failedRuns++;
                    if (failedRuns == 10) {
                        log.fatal("10 consecutive errors. Stopping worker.", e);
                        System.exit (1);
                    }
                    continue;
                }
                failedRuns = 0;

            }
            log.info ("BufferingJobLoader stopped.");

        }
    }

    /**
     * Create a new Worker instance.
     * @param conf Configuration used for prefetch queue size, and {@link Employer} and {@link Consumer} lookup
     */
    public Worker (Config conf) {
        super();
        this.conf = conf;

        //knownJobs = new HashSet();

        if (conf.getWorkerQueueSize() < 0) {
                throw new IllegalArgumentException("workerQueueSize must be >= 0");
            }

        // Find hostname for the Worker
        try {
            java.net.InetAddress localMachine =
                    java.net.InetAddress.getLocalHost();
            hostname = localMachine.getHostName();
        } catch (java.net.UnknownHostException uhe) {
            throw new RuntimeException ("Unable to obtain Worker hostname.", uhe);
        }

        try {
            DiceFactory fact = new DiceFactory(conf);
            employer = fact.lookupEmployer();
            consumer = fact.lookupConsumer();
        } catch (RemoteException e) {
            log.fatal (e);
            throw new RuntimeException(e);
        }

        stop = false;
        keepAlive = false;
        jobLoader = new BufferingJobLoader();
        jobLoaderThread = new Thread (jobLoader, "BufferingJobLoader");
        jobQueue = new ArrayBlockingQueue<Job>(conf.getWorkerQueueSize());
        random = new Random(System.currentTimeMillis());
    }

    /**
     * Enqueue a job locally. Defaults to adding the job to a blocking queue.
     * Actual implementations overriding this method should probably override
     * {@link #nextJob} too. See fx. {@link CachingWorker} for an example of this.
     * @param job job to enqueue
     */
    public void bufferJob (Job job)  throws JobFailedException {
        if (conf.getWorkerQueueSize() == 0) {
            return;
        }

        try {
            jobQueue.put(job);
        } catch (InterruptedException e) {
            log.warn ("Job buffering interrupted");
            throw new JobFailedException("Job buffering interrupted", e);
        }
    }

    /**
     * Retrieve the next job from the job queue. If the workerQueueSize
     * is 0, just retrieve directly from Employer. If this call is blocking
     * it should be interrutible by issuing {@link Thread#interrupt()} on
     * the main loop. If interrupted return null.
     *
     * Actaul implementations overriding this methods should probably also
     * override {@link #bufferJob}. See fx. {@link CachingWorker}
     *
     * @throws IOException if the worker is runnning without a job queue
     *         (ie. without prefetching) and there is an error getting the
     *         job from the {@link Employer}.
     *         This is an IOException instead of a RemoteException to ease life
     *         for subclasses overriding this method.
     * @throws JobFailedException This implementation does not throw a JobFailedException,
     *         the exception is declared to allow subclasses overriding this method
     *         to throw it if they need to do some preprocessing on the job that might fail.
     *         See fx {@link CachingWorker#nextJob()}.
     * @return next Job from the job queue.
     */
    protected Job nextJob () throws IOException, JobFailedException {
        if (conf.getWorkerQueueSize() == 0) {
            // We don't buffer. Use employer directly.
            return employer.getJob(hostname);
        }
        try {
            return jobQueue.take();
        } catch (InterruptedException e) {
            log.info("getJob interrupted, returning null");
            return null;
        }
    }

    /**
     * Submit a processed {@link Job} to the {@link Consumer}.
     * If you overwrite this method make sure you call {@link Consumer#consumeJob}.
     * @throws IOException if there is a problem calling {@link Consumer#consumeJob}.
     *         This is an IOException instead of a RemoteException to ease life
     *         for subclasses overriding this method.
     * @param job
     */
    protected void dispatchJob (Job job) throws IOException {
        consumer.consumeJob(job);
    }

    abstract protected Job processJob (Job job) throws JobFailedException;

    /**
     * Contact the Employer and set the job status. This call automatically
     * passes along the hostname of the Worker.
     * @param status
     */
    protected final void setJobStatus (JobStatus status) {
        int tries = 0;
        while (tries < 10) {
            try {
                employer.setJobStatus(status, hostname);
                return;
            } catch (RemoteException e) {
                log.error ("Failed to report status to employer. Sleeping.", e);
                sleep(5000);
                tries++;
                continue;
            }
        }
        log.fatal ("Unable to contact Employer in " + tries + " attempts. Bailing out.");
        System.exit(1);
    }

    protected final Employer getEmployer () {
        return employer;
    }

    protected final Consumer getConsumer () {
        return consumer;
    }

    public void stop () {
        log.info ("Stopping BufferingJobLoader");
        stop = true;
        jobLoaderThread.interrupt();
        mainLoop.interrupt();
        try {
            jobLoaderThread.join();
        } catch (InterruptedException e) {
            // Do nothing
        }
    }

    private void sleep (int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public final void run () {
        log.info("Starting Worker");

        mainLoop = Thread.currentThread();

        // Start the buffering job loader in a new thread
        jobLoaderThread.start();

        // Worker main loop, process incomming data
        int failedRuns = 0;
        while (!stop) {
            if (failedRuns == 10) {
                log.fatal(failedRuns + " consecutive failed jobs. Stopping worker.");
                System.exit(1);
            }
            
            Job incommingJob = null;
            try {
                incommingJob = nextJob ();
            } catch (IOException e) {
                log.error ("Failed to retrieve next job. Stopping Worker.", e);
                stop();
                failedRuns++;
                break;
            } catch (JobFailedException e) {
                log.error ("Job failed " + incommingJob + ". Sleeping.", e);
                sleep(5000*failedRuns + random.nextInt(1000));
                failedRuns++;
                continue;
            }

            // Sanity check job
            if (incommingJob == null) {
                log.warn ("Skipping null Job");
                continue;
            } else if (incommingJob instanceof NoJob) {
                log.warn ("Got termination signal");
                if (!keepAlive) {
                    log.warn ("Worker is set keepAlive=false, stopping worker");
                    stop();
                }
                continue;
            }

            // Process job
            log.info ("Starting job: " + incommingJob);
            setJobStatus(new JobStatus(incommingJob, JobStatus.Status.JOB_PROCESSING));
            Job processedJob = null;
            try {
                processedJob = processJob(incommingJob);
            } catch (Exception e) {
                log.error ("Error processing job " + incommingJob + ": " + e.getMessage() + ".\n Sleeping.", e);
                setJobStatus(new JobStatus(incommingJob, JobStatus.Status.JOB_FAILED));
                sleep(5000*failedRuns + random.nextInt(1000));
                failedRuns++;
                continue;
            }

            // Notify consumer
            setJobStatus(new JobStatus(incommingJob, JobStatus.Status.JOB_PROCESSED));
            try {
                dispatchJob(processedJob);
            } catch (IOException e) {
                log.error("Error dispatching job " + processedJob + ". Sleeping.", e);
                setJobStatus(new JobStatus(incommingJob, JobStatus.Status.JOB_FAILED));
                sleep(5000*failedRuns + random.nextInt(1000));
                failedRuns++;
                continue;
            }

            failedRuns = 0;
        }
        log.info ("Mainloop exiting");
    }

}



