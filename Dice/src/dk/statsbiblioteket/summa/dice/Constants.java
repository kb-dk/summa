/* $Id: Constants.java,v 1.3 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.3 $
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

import dk.statsbiblioteket.summa.dice.caching.RemoteCache;
import dk.statsbiblioteket.summa.dice.caching.CacheWriter;
import dk.statsbiblioteket.summa.dice.caching.CacheReader;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Interface to define keys for configuration parameters.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public interface Constants {

    /** Name of default config file in the class path.
     * This file will be loaded by {@link Config#loadDefaults} */
    public static final String DEFAULT_RESOURCE = "dice.config.xml";

    /** */
    public static final int NO_SUCH_INT = -1;

    /**
     * Standard Dice properties
     */

    /** Class to instantiate {@link Employer} from */
    public static final String CONF_EMPLOYER_CLASS = "employer.class";

    /** Class to instantiate {@link Consumer} from */
    public static final String CONF_CONSUMER_CLASS = "consumer.class";

    /** Class to instantiate {@link Worker} from */
    public static final String CONF_WORKER_CLASS = "worker.class";

    /** Socket type to use for RMI communications.
     * <code>normal</code> or <code>ssl</code>. There's also a
     * buggy <code>gzip</code> socket type. */
    public static final String CONF_SOCKET_TYPE = "socket.type";

    /** Hostname for the machine on which the Employer is running */
    public static final String CONF_EMPLOYER_HOSTNAME = "employer.hostname";

    /** Hostname for the machine on which the Consumer is running */
    public static final String CONF_CONSUMER_HOSTNAME = "consumer.hostname";

    /** RMI service name for the Employer */
    public static final String CONF_EMPLOYER_SERVICE = "employer.service";

    /** RMI service name for the Consumer */
    public static final String CONF_CONSUMER_SERVICE = "consumer.service";

    /** Port on which the Employer talks RMI */
    public static final String CONF_EMPLOYER_PORT = "employer.port";

    /** Port on which the Consumer talks RMI */
    public static final String CONF_CONSUMER_PORT = "consumer.port";

    /** The port on which to start an RMI registry on */
    public static final String CONF_REGISTRY_PORT = "registry.port";

    /** Number of jobs the Employer should pre-cache */
    public static final String CONF_EMPLOYER_QUEUE_SIZE = "employer.queue";

    /** Number of jobs pending on the consumer before blocking */
    public static final String CONF_CONSUMER_QUEUE_SIZE = "consumer.queue";

    /** Number of jobs to prefetch on the Worker */
    public static final String CONF_WORKER_QUEUE_SIZE = "worker.queue";

    /** <code>dedicated</code> or <code>sleeper</code>. A dedicated
     * worker use all available system resources while a sleeping
     * worker tries to play nice and run in the background */
    public static final String CONF_WORKER_ROLE = "worker.role";

    /** A place for the Employer to store data */
    public static final String CONF_EMPLOYER_DATA_PATH = "employer.data.path";

    /** A place for the Consumer to store data, this would typically be the final
     * data */
    public static final String CONF_CONSUMER_DATA_PATH = "consumer.data.path";

    /** A place for the Worker to store data, this would typically be intermediate
     * output of job processing  */
    public static final String CONF_WORKER_DATA_PATH = "worker.data.path";

    /**
     * Caching constants
     */

    /** RMI service name for the Employers {@link RemoteCache} */
    public static final String CONF_EMPLOYER_CACHE_SERVICE = "employer.cache.service";

    /** RMI service name for the Consumers {@link RemoteCache} */
    public static final String CONF_CONSUMER_CACHE_SERVICE = "consumer.cache.service";

    /** Port on which the Employers cache talks RMI */
    public static final String CONF_EMPLOYER_CACHE_PORT = "employer.cache.port";

    /** Port on which the Consumers cache talks RMI */
    public static final String CONF_CONSUMER_CACHE_PORT = "consumer.cache.port";

    /** Path to store cached items on the Employer.
     * Defaults to {@link #CONF_EMPLOYER_DATA_PATH}/cache */
    public static final String CONF_EMPLOYER_CACHE_PATH = "employer.cache.path";

    /** Path to store cached items on the Consumer.
     * Defaults to {@link #CONF_CONSUMER_DATA_PATH}/cache */
    public static final String CONF_CONSUMER_CACHE_PATH = "consumer.cache.path";

    /** Path to store cached items on the Worker.
     * Defaults to {@link #CONF_WORKER_DATA_PATH}/cache */
    public static final String CONF_WORKER_CACHE_PATH = "worker.cache.path";

    /** {@link CacheWriter} for writing to the Workers cache */
    public static final String CONF_WORKER_CACHE_WRITER = "worker.cache.writer";

    /** {@link CacheWriter} for writing to the Employers cache */
    public static final String CONF_EMPLOYER_CACHE_WRITER = "employer.cache.writer";

    /** {@link CacheWriter} for writing to the Consumers cache */
    public static final String CONF_CONSUMER_CACHE_WRITER = "consumer.cache.writer";

    /** {@link CacheReader} for reading from the Workers cache */
    public static final String CONF_WORKER_CACHE_READER = "worker.cache.reader";

    /** {@link CacheReader} for reading from the Employers cache */
    public static final String CONF_EMPLOYER_CACHE_READER = "employer.cache.reader";

    /** {@link CacheReader} for reading from the Consumers cache */
    public static final String CONF_CONSUMER_CACHE_READER = "consumer.cache.reader";


    /**
     * Job hints
     */

    /** Job hint containing the <code>long</code> cache id for the data
     * of a cached job */
    public static final String CONF_JOB_CACHE_ID = "job.cache.id";

}



