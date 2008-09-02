/* $Id: EmployerBase.java,v 1.4 2007/10/04 13:28:19 te Exp $
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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.lang.management.ManagementFactory;
import java.io.Serializable;
import java.io.File;
import java.text.DateFormat;

import dk.statsbiblioteket.summa.dice.util.RegistryManager;
import dk.statsbiblioteket.summa.dice.util.SimpleLog;
import dk.statsbiblioteket.summa.dice.util.Profiler;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.management.*;

/**
 * Base class for easy implementation of a generic {@link Employer}.
 * The only thing an implementation need to do is implement the abstract
 * {@link #fetchJob()} method.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
abstract public class EmployerBase extends UnicastRemoteObject
                                   implements Employer, Runnable, EmployerBaseMBean, Constants {

    private static final Log log = LogFactory.getLog(Employer.class);

    private ArrayBlockingQueue<Job> jobQueue;
    private Map<String,JobWithStatus> runningJobs;
    private Map<String,Profiler> workerInfo;
    private List<String> finishedJobs;
    private int port;
    private int nextUnprocessedJob;
    private boolean stop;
    private boolean forceStop;
    private boolean keepAlive;
    private boolean sourceDepleted;
    private boolean allDone;
    private Thread jobLoader;
    protected Config conf;
    private static final Date startDate = new Date();
    private static final DateFormat dateFormatter = DateFormat.getDateTimeInstance();
    private Profiler processProfiler;
    private Profiler fetchProfiler;

    private class NoJob extends Job {
        public NoJob() {
            super(null, null, null);
        }
    }

    private class JobWithStatus {
        Job job;
        JobStatus status;

        public JobWithStatus (Job job, JobStatus status) {
            this.job = job;
            this.status = status;
        }

        public Job getJob () { return job; }
        public JobStatus getStatus () { return status; }
        public synchronized void setStatus (JobStatus status) { this.status = status; }
    }

    private class JobLoader implements Runnable {
        public void run () {
            log.info ("Starting JobLoader");
            sourceDepleted = false;
            while (!stop && !forceStop) {

                Job incommingJob;
                try {
                    incommingJob = fetchJob();
                } catch (RuntimeException e) {
                    log.error("Error fetching job", e);
                    throw e;
                }

                if (incommingJob == null) {
                    log.info ("Found null Job, stopping JobLoader");
                    stop = true;
                    sourceDepleted = true;
                    incommingJob = new NoJob();
                } else {
                    // Ensure we have proper job hints
                    incommingJob = createJobWithHints(incommingJob);
                }

                try {
                    log.debug ("Enqueuing job: " + incommingJob);
                    jobQueue.put (incommingJob);

                } catch (InterruptedException e) {
                    // This is the end of the loop,
                    // so we check stop condition on next iteration anyway
                }

                // Updating the speed here causes jobQueue put waiting to
                // be counted into the fecthing time. That is intentional!
                fetchProfiler.update();
                log.info ("Mean fetching speed: " + fetchProfiler.getMeanSpeed()/1000);

            }
            log.info ("JobLoader stopped");
        }
    }

    public EmployerBase (Config conf)
                        throws RemoteException {
        super (conf.getEmployerPort(), conf.getClientSocketFactory(), conf.getServerSocketFactory());
        jobQueue = new ArrayBlockingQueue<Job>(conf.getEmployerQueueSize());
        runningJobs = Collections.synchronizedMap(new HashMap<String,JobWithStatus>());
        finishedJobs = Collections.synchronizedList(new ArrayList<String>());
        workerInfo = Collections.synchronizedMap(new HashMap<String,Profiler>());

        nextUnprocessedJob = 0;
        stop = false;
        forceStop = false;
        allDone = false;
        keepAlive = false;
        this.conf = conf;

        jobLoader = new Thread (new JobLoader(), "JobLoader");

        // Profiling
        processProfiler = new Profiler();
        fetchProfiler = new Profiler();
        processProfiler.start();
        fetchProfiler.start();
    }

    /**
     * Create a Job object to enqueue in the jobQueue.
     * If no more Jobs are available return null.
     * @return The next Job or null if no more Jobs will be available
     */
    abstract protected Job fetchJob ();

    public boolean isFinished () {
        // Check the big red kill switch
        if (forceStop) {
            return true;
        }

        // If we've ever reported finished, this method, will keep doing
        // so, without checking status again.
        if (allDone) {
            return allDone;
        } else {
            Job lastJob = jobQueue.peek();
            allDone = (lastJob == null || lastJob instanceof NoJob) && (runningJobs.size() == 0) && sourceDepleted;
            return allDone;
        }
    }

    /**
     *
     * @param status
     * @param workerHostname
     */
    public synchronized void setJobStatus (JobStatus status,
                                           String workerHostname) {

        if (!workerInfo.containsKey(workerHostname)) {
            registerWorker (workerHostname);
        }

        JobWithStatus jws = runningJobs.get (status.getName());
        if (jws != null) {
            // Status regressions should be ignored. They just mean that a worker is lagging behind.
            if (jws.getStatus().getStatus().compareTo(status.getStatus()) >= 0) {
                log.warn ("Trying to degrade job status for " + jws.getStatus() + " to " + status.getStatus() + ". Probably a re-issued job. Ignoring.");
                return;
            }

            if (status.getStatus().equals(JobStatus.Status.JOB_FAILED)) {
                log.warn ("Job " + status + " marked as failed from " + workerHostname + ". Reseting job status.");
                jws.setStatus(new JobStatus (jws.getJob(), JobStatus.Status.JOB_NONE));
                return;
            } else if (status.getStatus().equals(JobStatus.Status.JOB_LOST)) {
                // The JOB_LOST status means that the consumer choked on a
                // job in its local cache. Such jobs are not stored on the Employer
                // anymore - we're screwed.
                log.fatal ("Consumer failed to handle job: " + status);
                SimpleLog lostJobLog = new SimpleLog(conf.getString(Constants.EMPLOYER_DATA_PATH)  + File.separator + "lost_jobs.log");
                lostJobLog.log (status.getName());
                return;
            }

            log.debug ("Changing status of: " + status.getName() + " to " + status + ", by worker: " + workerHostname);
            jws.setStatus(status);

            if (status.getStatus().equals(JobStatus.Status.JOB_CONSUMED)) {
                log.info ("Job consumed: " + status.getName());
                // This job is done, free the data member of the Job
                runningJobs.remove(status.getName());
                finishedJobs.add(status.getName());
                processProfiler.update(); // This might better go under the the JOB_PROCESSED case, but doesn't make a huge difference 
                log.info ("Mean processing speed: " + processProfiler.getMeanSpeed()/1000);
            } else if (status.getStatus().equals(JobStatus.Status.JOB_PROCESSED)) {
                // Update the Workers Profiler
                workerInfo.get(workerHostname).update();
            }
        } else {
            if (finishedJobs.contains(status.getName())) {
                log.warn("Trying to change status on finished job " + status.getName() + ", by worker: " + workerHostname);
                return;
            }
            log.error ("Trying to set status on unknown job: " + status + ", by worker: " + workerHostname);
        }

    }

    /**
     * Returns the next job from the job queue. The return value is
     * null if no more jobs are available.
     * This call automatically removes the Job from the job queue
     * and appends it to the list of running jobs.
     * @return The next Job or null
     */
    public Job getJob (String workerHostname) {
        log.debug ("Handling job request from " + workerHostname);

        if (!workerInfo.containsKey(workerHostname)) {
            registerWorker (workerHostname);
        }

        Job job = null;

        if (forceStop) {
            return null;
        }

        try {
            job = jobQueue.take();
        } catch (InterruptedException e) {
            // Try again
            log.info ("getJob interrupted. Retrying.");
            return getJob(workerHostname);
        }

        if (job instanceof NoJob) {
            try {
                jobQueue.put(job);
            } catch (InterruptedException e) {
                log.warn ("Enqueing of NoJob interrupted. getJob calls might block from now on.");
            }
            // Get next job from the runningJobs list
            job = getNextRunningJob();
            if (job != null) {
                log.warn ("All jobs dispatched. Returning old job: " + job);
            } else {
                log.info ("All jobs dispatched and registered by consumer.");
            }
            return job;
        }

        // Register the job as running
        JobWithStatus jws = new JobWithStatus(job, new JobStatus(job, JobStatus.Status.JOB_WORKER_FETCHING));
        runningJobs.put(job.getName(), jws);
        log.debug ("returning job: " + job);
        return job;
    }

    /**
     * Get a {@link JobStatus} object for the named {@link Job}.
     * Returns null if the named Job is not running or registered as complete.
     * @param jobName Unique job name as given by Job.getName()
     * @return A JobStatus or null if the named Job isn't registered as running
     */
    public synchronized JobStatus getJobStatus (String jobName) {
        JobWithStatus jws = runningJobs.get(jobName);
        if (jws == null) {
            if (finishedJobs.contains(jobName)) {
                return new JobStatus (jobName, JobStatus.Status.JOB_CONSUMED);
            } else {
                log.warn ("Trying to lookup unknown job: " + jobName);
                return null;
            }
        } else {
            return jws.getStatus();
        }
    }

    private void registerWorker (String workerHostname) {
        if (workerInfo.containsKey(workerHostname)) {
            log.warn ("Trying to re-register worker: " + workerHostname + ". Ignoring request.");
            return;
        }
        log.info ("Registering new worker: " + workerHostname);
        Profiler p = new Profiler();
        p.start();
        workerInfo.put (workerHostname, p);
    }

    public void stop () {
        log.info ("Stopping JobLoader");
        stop = true;
        jobLoader.interrupt();
        try {
            jobLoader.join();
        } catch (InterruptedException e) {
            // Do nothing
        }
    }

    public void forceStop () {
        forceStop = true;
        stop();
    }

    private synchronized Job getNextRunningJob () {
        if (runningJobs.isEmpty()) {
            return null;
        }

        int i = 0;
        for (String jobName : runningJobs.keySet()) {
            if (i == nextUnprocessedJob) {
                Job job = runningJobs.get(jobName).getJob();
                nextUnprocessedJob++;
                if (getJobStatus(job.getName()).getStatus().equals (JobStatus.Status.JOB_CONSUMER_ENQUEUED)) {
                    // Jobs with status JOB_CONSUMER_ENQUEUED are considered processed
                    continue;
                }
                return job;
            }
            i++;
        }

        // Get the first job in the list of running jobs,
        // and let nextUnprocessedJob point at the second.
        nextUnprocessedJob = 1;
        String jobName = runningJobs.keySet().iterator().next();
        if (getJobStatus(jobName).getStatus().equals (JobStatus.Status.JOB_CONSUMER_ENQUEUED)) {
            // runningJobs is empty or contains only jobs with status JOB_CONSUMER_ENQUEUED
            log.warn ("getNextRunningJob: No running jobs, or all jobs enqueued on Consumer. Returning null.");
            return null;
        }

        return runningJobs.get(jobName).getJob();
    }

    public int getQueueSize () {
        return jobQueue.size();
    }

    public int getNumRunning () {
        return runningJobs.size() - getNumConsumerEnqueued();
    }

    public int getNumConsumerEnqueued () {
        int consumerQueueSize = 0;
        for (JobWithStatus jws : runningJobs.values()) {
            if (jws.getStatus().getStatus().equals (JobStatus.Status.JOB_CONSUMER_ENQUEUED)) {
                consumerQueueSize++;
            }
        }
        return consumerQueueSize;
    }

    public int getNumFinished () {
        return finishedJobs.size();
    }

    public String[] getWorkerInfo () {
        String[] result = new String[workerInfo.size()];
        int i = 0;
        for (String worker : workerInfo.keySet()) {
            result[i] = worker + ": " + workerInfo.get(worker);
                    i++;
        }
        return result;
    }

    public String getStartDate () {
        return  dateFormatter.format (startDate);
    }

    public double getMeanProcessingSpeed () {
        return processProfiler.getMeanSpeed()/1000;
    }

    public double getProcessingSpeed() {
        return processProfiler.getSpeed()/1000;
    }

    public double getMeanFetchingSpeed () {
        return fetchProfiler.getMeanSpeed()/1000;
    }

    /**
     * Set relevant job hints, if they are not already set
     * @param job
     */
    private Job createJobWithHints (Job job) {
        if (job.getHints() == null) {
            HashMap<String,String> hints = new HashMap<String,String>();
            job = new Job ((Serializable)job.getData(), hints, job.getName());
        }

        if (conf.get(EMPLOYER_CACHE_SERVICE) != null && job.getHint(EMPLOYER_CACHE_SERVICE) == null) {
            job.getHints().put(EMPLOYER_CACHE_SERVICE, conf.getString(EMPLOYER_CACHE_SERVICE));
        }

        if (conf.get(CONSUMER_CACHE_SERVICE) != null && job.getHint(CONSUMER_CACHE_SERVICE) == null) {
            job.getHints().put(CONSUMER_CACHE_SERVICE, conf.getString(CONSUMER_CACHE_SERVICE));
        }

        return job;
    }

    /**
     * Bind this Employer in the registry, and start the job loader
     */
    public void run () {
        try {
            log.info ("Starting employer");

            System.setSecurityManager(new RMISecurityManager());

            RegistryManager.getRegistry(conf).rebind (conf.getEmployerServiceName(), this);
            log.info ("Employer bound as: " + conf.getEmployerServiceName() + " on port " + conf.getEmployerPort());
            jobLoader.start();

            MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(this.getClass().getName()+ ":type=Employer");
            mbserver.registerMBean(this, name);

        } catch (RemoteException e) {
            log.fatal (e);
            throw new RuntimeException(e);
        } catch (NotCompliantMBeanException e) {
            log.error (e);
        } catch (MBeanRegistrationException e) {
            log.error (e);
        } catch (MalformedObjectNameException e) {
            log.error (e);
        } catch (InstanceAlreadyExistsException e) {
            log.error (e);
        }
    }

    protected void finalize () throws Throwable {
        log.warn ("Finalizing Employer");

        log.warn ("Dumping config:\n===========================\n" +  conf.dumpString()+"\n");

        String workerStats = "";
        for (String stats : getWorkerInfo()) {
            workerStats += stats + "\n";
        }
        log.warn ("Dumping Worker stats:\n===========================\n" + workerStats);


        String employerStats = "MeanFetchingSpeed: " + getMeanFetchingSpeed() + "\n";
        employerStats = "MeanProcessingSpeed: " + getMeanProcessingSpeed() + "\n";
        employerStats = "ProcessingSpeed: " + getProcessingSpeed() + "\n";
        employerStats = "NumFinished: " + getNumFinished() + "\n";
        employerStats = "NumRunning: " + getNumRunning() + "\n";
        employerStats = "QueueSize: " + getQueueSize() + "\n";
        employerStats = "StartDate: " + getStartDate() + "\n";
        log.warn ("Dumping Employer stats:\n===========================\n" + employerStats);




    }
}
