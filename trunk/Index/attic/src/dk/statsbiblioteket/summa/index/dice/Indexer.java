/* $Id: Indexer.java,v 1.8 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.8 $
 * $Date: 2007/10/05 10:20:23 $
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
package dk.statsbiblioteket.summa.index.dice;

import dk.statsbiblioteket.summa.dice.EmployerBase;
import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.summa.dice.Job;
import dk.statsbiblioteket.summa.dice.util.SimpleLog;
import dk.statsbiblioteket.summa.dice.util.MD5;
import dk.statsbiblioteket.summa.dice.caching.*;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.util.*;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.security.NoSuchAlgorithmException;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Dice {@link dk.statsbiblioteket.summa.dice.Employer} implementation for distributed Lucene index compilation.
 *
 * <p><i>WARNING</i>: This implementation is not thread safe
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Indexer extends EmployerBase {

    private static final Log log = LogFactory.getLog(Indexer.class);

    private static final String CONFIG = "index.properties.xml";

    int nextTarget;
    int jobSize;
    Target currentTarget;
    List<Target> targets;
    CacheClient<Record> cache;
    private SimpleLog targetLog;
    private SimpleLog jobMapLog;
    private MD5 hashFactory;

    public Indexer (Config conf) throws RemoteException {
        super (conf);

        targets = new ArrayList<Target>();
        parseConfig();
        targetLog = new SimpleLog(conf.getString(IndexConfig.DATA_DIR)  + File.separator + "targets.log");
        jobMapLog = new SimpleLog(conf.getString(IndexConfig.DATA_DIR)  + File.separator + "job_map.log");

        try {
            hashFactory = new MD5();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to create MD5 sum factory");
        }

        nextTarget = 0;
        currentTarget = null;

        // TODO: Replace cache R/W with memory backed ones and store GZipBlobs instead
        Cache<Record> gzippingCache = new CacheService<Record>(conf.getClass(CONF_EMPLOYER_CACHE_WRITER),
                                                                conf.getClass(CONF_EMPLOYER_CACHE_READER),
                                                                conf.getString(CONF_EMPLOYER_CACHE_PATH));

        log.info ("Exporting cache as " + conf.getString(CONF_EMPLOYER_CACHE_SERVICE) + " on port " + conf.getInt(CONF_EMPLOYER_CACHE_PORT));
        Cache<Record> remoteCache = new RemoteCacheService<Record>(gzippingCache,
                                                                    conf.getString (IndexConfig.CONF_EMPLOYER_CACHE_SERVICE),
                                                                    conf.getInt (IndexConfig.CONF_EMPLOYER_CACHE_PORT),
                                                                    conf.getClientSocketFactory(),
                                                                    conf.getServerSocketFactory());
        cache = new GenericCacheClient<Record>(gzippingCache);
    }

    /**
     * Upload job data to the cache and return a {@link Job} with empty data member
     */
    public Job fetchJob () {
        updateTarget();
        if (currentTarget == null) {
            log.info ("No more targets. Stopping.");
            return null;
        }

        ArrayList<Record> data = new ArrayList<Record>();

        boolean targetDepleted = false; // Used to detect if we deplete the target during job construction

        for (int i = 0; i < jobSize; i++) {
            Record record = currentTarget.getNextRecord();
            if (record == null) {
                // Target depleted
                log.info ("Target " + currentTarget + " depleted");
                targetLog.log("Depleted: " + currentTarget);
                targetDepleted = true;
                break;
            }
            data.add (record);
        }

        if (data.isEmpty()) {
            // currentTarget was empty, try the next
            if (currentTarget != null) {
                log.error("Got empty data field, but target is not empty. This should never happen.");
            }
            return fetchJob();
        }

        // Return a job with job hints inherited from the current target and
        // add a job hint storing the first and last record id.
        // The (unique) job name is the name of first record record in the data.
        HashMap<String,String> hints = new HashMap<String,String> (currentTarget.getJobHints());
        hints.put(IndexConfig.LAST_RECORD_ID, data.get(data.size()-1).getId());
        hints.put(IndexConfig.FIRST_RECORD_ID, data.get(0).getId());

        // Upload data to cache, and set the CONF_JOB_CACHE_ID job hint so that
        // the workers can retrieve it
        try {
            // TODO: Employer should use a memory backed cache where it stores GZipBlobs (see constructor)
            long id = cache.put (data);
            hints.put (CONF_JOB_CACHE_ID, "" + id);
        } catch (IOException e) {
            log.fatal ("Failed to cache job data", e);
            throw new RuntimeException("Failed to cache job data", e);
        }

        // Create job. Name is the md5sum of the name of the first record
        Job job = new Job (null, hints, hashFactory.md5sum(hints.get(IndexConfig.FIRST_RECORD_ID)));
        log.info ("Created " + job + " with " + data.size() + " records, and hints: " + job.getHints());

        if (targetDepleted) {
            currentTarget = null;
        }

        jobMapLog.log (job.getName() + "\n\t" + job.getHint(IndexConfig.FIRST_RECORD_ID) + "\n\t" + job.getHint(IndexConfig.LAST_RECORD_ID));

        return job;
    }

    private void updateTarget() {
        if (currentTarget == null) {
            currentTarget = getNextTarget();
            if (currentTarget == null) {
                // No more targets, no more jobs
                return;
            }
            try {
                log.info ("Initializing target: " + currentTarget);
                targetLog.log ("Init: " + currentTarget);
                currentTarget.initialize();
                log.info ("Target " + currentTarget + " initialized");
                targetLog.log ("Ready: " + currentTarget);
            } catch (IOException e) {
                log.error ("Failed to initialize target: " + currentTarget, e);
            }
            log.info ("Switching to target " + currentTarget);
        }
    }

    private Target getNextTarget () {
        if (nextTarget >= targets.size()) {
            // no more targets
            return null;
        } else {
            Target target = targets.get(nextTarget);
            nextTarget++;
            return target;
        }
    }

    private void parseConfig () {
        targets = parseTargets (conf);

        if (log.isInfoEnabled()) {
            // Print target information
            String msg = "\n";
            for (Target target : targets) {
                msg += target + ":\n";
                Map<String,String> hints = target.getJobHints();
                for (Map.Entry<String,String> entry : hints.entrySet()) {
                    msg += "\t" + entry.getKey() + "=" + entry.getValue() + "\n";
                }
                msg += "\n";
            }
            log.info (msg);
        }

        jobSize = 10000;
    }

    /**
     * Build a list of {@link Target}s with configuration data collected from
     * the resource specified by {@link #CONFIG} and the input {@link Config}.
     * @param conf the configuration to extract employer and consumer service
     * names from.
     * @return uninitialized targets with full job hints
     */
    public static List<Target> parseTargets (Config conf) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream propStream = new BufferedInputStream(loader.getResourceAsStream(CONFIG));
        Properties props = new Properties();
        try {
            props.loadFromXML(propStream);
        } catch (IOException e) {
            log.fatal ("Failed to parse targets", e);
            throw new RuntimeException ("Failed to parse targets", e);
        }

        List<String> targetNames = extractTargetNames(props);
        List<Target> targets = new ArrayList<Target>();

        for (String target : targetNames) {
            targets.add (new Target(createTarget(target, props, conf)));
        }

        return targets;
    }

    private static List<String> extractTargetNames (Properties targetProps) {
        List<String> targetNames = new ArrayList<String>();

        for (Map.Entry<Object, Object> entry : targetProps.entrySet()) {
            Object obj = entry.getKey();
            if (obj instanceof String && ((String) obj).endsWith("_run")) {
                String targetName = ((String) obj).substring(0, ((String) obj).indexOf("_run"));
                if (Boolean.parseBoolean((String) targetProps.get(targetName + "_run"))) {
                    targetNames.add(targetName);
                    log.info("Found target " + targetName);
                }
            }
        }

        return targetNames;
    }

    private static HashMap<String,String> createTarget (String targetName, Properties targetProps, Config conf) {
        HashMap<String,String> info = new HashMap<String,String>();

        info.put(IndexConfig.BASE, targetName);
        info.put (CONF_EMPLOYER_CACHE_SERVICE, conf.getString(CONF_EMPLOYER_CACHE_SERVICE));
        info.put (CONF_CONSUMER_CACHE_SERVICE, conf.getString(CONF_CONSUMER_CACHE_SERVICE));

        for (Map.Entry entry : targetProps.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith(targetName)) {
                if (key.endsWith("_xslt_url")) {
                    info.put (IndexConfig.XSLT_URL, (String)entry.getValue());
                } else if (key.endsWith("_io_storage")) {
                    info.put (IndexConfig.RECORD_SERVICE, (String)entry.getValue());
                }
            }
        }



        return info;
    }
}



