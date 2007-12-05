/* $Id: CachingWorker.java,v 1.2 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.2 $
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
import dk.statsbiblioteket.summa.dice.caching.*;

import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.registry.Registry;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 13/09/2006
 * Time: 14:00:20
 * To change this template use File | Settings | File Templates.
 */
abstract public class CachingWorker extends Worker implements Constants {

    private Config conf;
    private CacheClient<Object> employerCache;
    private CacheClient<Object> consumerCache;
    protected CacheClient<Object> cache;
    private String employerCacheServiceName;
    private String consumerCacheServiceName;

    public CachingWorker(Config conf) {
        super(conf);

        this.conf = conf;
        employerCache = null;
        consumerCache = null;
        employerCacheServiceName = "";
        consumerCacheServiceName = "";

        // Ensure we have enough information to set up the worker
        validateConfig ();

        log.info("Creating cache with " + conf.getClass(WORKER_CACHE_WRITER).getSimpleName() + ", "
                                        + conf.getClass(WORKER_CACHE_READER).getSimpleName() + ", on path "
                                        + conf.getString (WORKER_CACHE_PATH));

        Cache<Object> cacheBackend = new CacheService<Object> (conf.getClass(WORKER_CACHE_WRITER),
                                                               conf.getClass(WORKER_CACHE_READER),
                                                               conf.getString (WORKER_CACHE_PATH));

        Cache<Object> blockingDataCache = new BlockingCacheService<Object> (cacheBackend);

        cache = new GenericCacheClient<Object>(blockingDataCache);
    }

    /**
     * The data member passed to <code>processJob</code> is an {@link Iterable}, and you
     * can obtain an {@link Iterator} over the cached data via the <code>iterator()</code>
     * method on the data object.
     * @param job job to be processed.
     * @return the processed job
     */
    abstract protected Job processJob (Job job) throws JobFailedException;

    protected Job nextJob () throws IOException, JobFailedException {
        Job job = super.nextJob();

        if (job == null) {
            return null;
        } else if (job instanceof NoJob) {
            return job;
        }

        if (conf.getWorkerQueueSize() == 0) {
            // Write the data to the cache since the JobLoader thread doesn't do it for us
            bufferJob(job);
        }

        if (job.getHint(JOB_CACHE_ID) == null) {
            log.error(JOB_CACHE_ID + " job hint not set. Unable to retrieve data from cache");
            throw new JobFailedException(JOB_CACHE_ID + " job hint is not set");
        }

        ProxyCacheItem proxyData = new ProxyCacheItem(cache, Long.parseLong(job.getHint(JOB_CACHE_ID)));

        return new Job (proxyData, job.getHints(), job.getName());

    }

    /**
     * The CachingWorker assumes that the data member of an incomming job
     * is a {@link Iterable}.
     * @param job
     */
    public void bufferJob (Job job) throws JobFailedException {

        if (job instanceof NoJob) {
            super.bufferJob(job);
            return;
        }

        String serviceName = job.getHint(EMPLOYER_CACHE_SERVICE);
        Iterator parts;
        if (serviceName == null) {
            parts = ((Iterable)job.getData()).iterator();
        } else {
            if (!serviceName.equals(employerCacheServiceName) || employerCache == null) {
                // We have a new employerCache service or there is none set
                lookupEmployerCache(serviceName);
            }

            String jobId = job.getHint(JOB_CACHE_ID);
            if (jobId == null) {
                throw new JobFailedException (JOB_CACHE_ID + " not set for job " + job);
            }

            try {
                // Get the remote Cacheable and feed it into the local cache
                parts =  employerCache.get(Long.parseLong(jobId));
            } catch (IOException e) {
                throw new JobFailedException(e);
            }
        }

        // Write job data to local cache
        long localCacheId = -1;
        try {
            localCacheId = cache.put (new ProxyIterable(parts));
        } catch (IOException e) {
            log.error("Failed to cache job data for job: " + job);
            throw new JobFailedException("Failed to cache job data for job: " + job, e);
        }

        // Store the cache id in the job hints and reset the data field
        job.getHints().put (JOB_CACHE_ID, ""+localCacheId);
        job = new Job (null, job.getHints(), job.getName());

        // Make sure we respect the job queue size, this call blocks until there is room in the queue
        super.bufferJob(job);
    }

    /**
     * Upload the job to the consumer. If the consumer is caching (detected via the
     * {@link Constants#CONSUMER_CACHE_SERVICE} job hint) upload job data to the
     * consumer's cache and don't pass the job's data member over the wire at all.
     * @param job Job to send of to the {@link Consumer}
     * @throws IOException if there are errors contacting the {@link Consumer}
     */
    protected void dispatchJob (Job job) throws IOException {
        String serviceName = job.getHint(CONSUMER_CACHE_SERVICE);
        if (!serviceName.equals (consumerCacheServiceName) || consumerCache == null) {
                // We have a new consumerCache service or there is none set
                lookupConsumerCache(serviceName);
        }

        if (serviceName != null) {
            // Consumer does indeed use a cache, upload job data and store the id in the jobHints,
            // finally reset the job data so we don't pass it over the rmi connection
            long id = consumerCache.put((Iterable<Object>)job.getData());
            job.getHints().put(JOB_CACHE_ID, "" + id);
            job = new Job (null, job.getHints(), job.getName());
        } else {
            // Make sure we don't pass any confusing hints to the consumer
            job.getHints().remove(JOB_CACHE_ID);
        }

        getConsumer().consumeJob(job);
    }

    private void lookupEmployerCache (String serviceName) {
        employerCacheServiceName = serviceName;
        try {
            Registry reg = RegistryManager.getRemoteRegistry(conf.getEmployerHostname(), conf.getRegistryPort(), conf);
            employerCache = new GenericCacheClient<Object> ((RemoteCache<Object>) reg.lookup(serviceName));

        } catch (RemoteException e) {
            log.error("Failed to look up employer cache", e);
        } catch (NotBoundException e) {
            log.error("Employer cache not bound in registry", e);
        }
    }

    private void lookupConsumerCache (String serviceName) {
        consumerCacheServiceName = serviceName;
        try {
            Registry reg = RegistryManager.getRemoteRegistry(conf.getConsumerHostname(), conf.getRegistryPort(), conf);
            consumerCache = new GenericCacheClient<Object> ((RemoteCache<Object>) reg.lookup(serviceName));

        } catch (RemoteException e) {
            log.error("Failed to look up consumer cache", e);
        } catch (NotBoundException e) {
            log.error("Consumer cache not bound in registry", e);
        }
    }

    /**
     * Ensure we have enough information to set up the worker
     */
    private void validateConfig () {
        if (conf.get(WORKER_CACHE_PATH) == null) {
            throw new NullPointerException(WORKER_CACHE_PATH + " not set");
        }

        if (conf.get(WORKER_CACHE_READER) == null) {
            conf.set (WORKER_CACHE_READER, ObjectCacheReader.class);
        }

        if (conf.get(WORKER_CACHE_WRITER) == null) {
            conf.set (WORKER_CACHE_WRITER, ObjectCacheWriter.class);
        }
    }
}
