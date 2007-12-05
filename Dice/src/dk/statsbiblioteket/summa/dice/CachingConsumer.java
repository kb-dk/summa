/* $Id: CachingConsumer.java,v 1.4 2007/10/04 13:28:19 te Exp $
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

import dk.statsbiblioteket.summa.dice.caching.*;
import dk.statsbiblioteket.summa.dice.util.SimpleLog;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: mikkel
 * Date: 14/09/2006
 * Time: 11:05:23
 * {@link Consumer} base class storing job data members in a {@link dk.statsbiblioteket.summa.dice.caching.Cache}.
 * This means that the data member of all incomming jobs must be an {@link Iterable}.
 * The {@link Config} object passed to the constructor must have the
 * {@link Constants#CONSUMER_CACHE_PATH}, {@link Constants#CONSUMER_CACHE_PORT},
 * {@link Constants#CONSUMER_CACHE_SERVICE} properties set.
 *
 * Any {@link Worker} connecting to this Consumer must upload job data members to
 * the service specified in the {@link Constants#CONSUMER_CACHE_SERVICE}, by issuing
 * a {@link dk.statsbiblioteket.summa.dice.caching.CacheClient#put} and setting the returned <code>id</code> as the
 * {@link Constants#JOB_CACHE_ID} job hint. Upon processing a job the CachingConsumer
 * will look up data given by this id in the cache.
 *
 * The general contract is that the worker only calls {@link #consumeJob} when it is done
 * uploading the data item to the cache.
 *
 * The {@link CachingWorker} implements the required behavior.
 */
public abstract class CachingConsumer extends ConsumerBase implements Constants {

    protected dk.statsbiblioteket.summa.dice.caching.CacheClient<Object> cache;
    private SimpleLog cacheMapLog;
    private static final String CACHEMAP_LOG_FILE = "cache_map.log";

    /**
     * Create a new Consumer with a {@link Cache} exposed over rmi. Port and service names
     * will be read from the supplied {@link Config}.
     * 
     * @param conf the keys consumer.cache.service and consumer.cache.port must be set
     * @throws java.rmi.RemoteException If the there are errors detecting the registry or binding the service
     */
    protected CachingConsumer(Config conf) throws RemoteException {
        super(conf);

        validateConfig(conf);

        log.info("Creating cache with " + conf.getClass(CONSUMER_CACHE_WRITER).getSimpleName() + ", "
                                        + conf.getClass(CONSUMER_CACHE_READER).getSimpleName() + ", on path "
                                        + conf.getString (CONSUMER_CACHE_PATH));

        dk.statsbiblioteket.summa.dice.caching.Cache<Object> store = new dk.statsbiblioteket.summa.dice.caching.CacheService<Object>(conf.getClass(CONSUMER_CACHE_WRITER),
                                                        conf.getClass(CONSUMER_CACHE_READER),
                                                        conf.getString(CONSUMER_CACHE_PATH));

        Cache<Object> blockingCache = new BlockingCacheService<Object> (store);

        /* Expose the blocking cache over rmi */
        log.info ("Exporting cache as " + conf.getString (CONSUMER_CACHE_SERVICE) + " on port " + conf.getInt (CONSUMER_CACHE_PORT));
        Cache<Object> remoteCache = new RemoteCacheService<Object>
                                                            (blockingCache,
                                                            conf.getString (CONSUMER_CACHE_SERVICE),
                                                            conf.getInt (CONSUMER_CACHE_PORT),
                                                            conf.getClientSocketFactory(),
                                                            conf.getServerSocketFactory());

        cache = new GenericCacheClient<Object> (blockingCache);

        cacheMapLog = new SimpleLog(conf.get(Constants.CONSUMER_DATA_PATH) + File.separator + CACHEMAP_LOG_FILE);
    }

    /**
     * The <code>job</code> parameter is guarantted to have the job hint {@link Constants#JOB_CACHE_ID}
     * set to the <code>id</code> of the corresponding data in the cache.
     * You can obtain an {@link Iterator} over the data parts by calling <code>cache.get(id)</code>.
     * @param job job to be processed.
     */
    abstract protected void processJob (Job job);

    public void consumeJob (Job job) {
        super.consumeJob(job);
        cacheMapLog.log (job.getName() + " -> " + job.getHint(JOB_CACHE_ID));
    }

    /**
     * Get the next job from the job queue and load it with the job data from the cache
     * @return A {@link Job} whose data member is a proxy iterator over the data member in the cache.
     */
    protected Job nextJob () {
        Job job = super.nextJob();

        if (job.getHint(JOB_CACHE_ID) == null) {
            log.error(JOB_CACHE_ID + " job hint not set. Unable to retrieve data from cache");
            throw new NullPointerException(JOB_CACHE_ID + " job hint is not set");
        }

        ProxyCacheItem proxyData = new ProxyCacheItem(cache, Long.parseLong(job.getHint(JOB_CACHE_ID)));

        return new Job (proxyData, job.getHints(), job.getName());
    }

    /**
     * Throw an exception if the configuration looks insufficient or malformed
     * @param conf the configuration to check
     */
    private void validateConfig (Config conf) {
        String serviceName = conf.getString (CONSUMER_CACHE_SERVICE);
        String cachePath = conf.getString (CONSUMER_CACHE_PATH);
        int cachePort = conf.getInt (CONSUMER_CACHE_PORT);

        if (serviceName == null) {
            throw new NullPointerException(CONSUMER_CACHE_SERVICE + " not set in config");
        }

        if (cachePath == null) {
            throw new NullPointerException(CONSUMER_CACHE_PATH + " not set in config");
        }

        if (cachePort == NO_SUCH_INT) {
            throw new NullPointerException(CONSUMER_CACHE_PORT + " not set in config");
        }

        if (conf.get(CONSUMER_CACHE_READER) == null) {
            conf.set (CONSUMER_CACHE_READER, ObjectCacheReader.class);
        }

        if (conf.get(CONSUMER_CACHE_WRITER) == null) {
            conf.set (CONSUMER_CACHE_WRITER, ObjectCacheWriter.class);
        }
    }
}
