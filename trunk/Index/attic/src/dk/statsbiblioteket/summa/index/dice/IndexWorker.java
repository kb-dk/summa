/* $Id: IndexWorker.java,v 1.7 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.7 $
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

import dk.statsbiblioteket.summa.dice.util.ZippedFolder;
import dk.statsbiblioteket.summa.dice.util.FileAlreadyExistException;
import dk.statsbiblioteket.summa.dice.CachingWorker;
import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.summa.dice.Job;
import dk.statsbiblioteket.summa.dice.JobFailedException;
import dk.statsbiblioteket.summa.index.IndexService;
import dk.statsbiblioteket.summa.index.IndexServiceImpl;
import dk.statsbiblioteket.summa.index.FaultyDataException;
import dk.statsbiblioteket.summa.index.IndexManipulationUtils;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;

import javax.xml.transform.TransformerException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class IndexWorker extends CachingWorker {

    private static final Log log = LogFactory.getLog(IndexWorker.class);

    private IndexService indexService;
    private String tmpIndexPath;
    private String zippedIndexPath;
    private Config conf;

    public IndexWorker (Config conf) {
        super (conf);
        this.conf = conf;
                
        try {
            if (new String("dedicated").equals(conf.get("worker.role"))) {
                log.info ("Worker is running in dedicated mode. Using RAM based IndexService");
                indexService = new IndexServiceImpl(true);
            } else {
                log.info ("Worker is running in sleeper mode");
                indexService = new IndexServiceImpl();
            }
        } catch (IndexServiceException e) {
            throw new RuntimeException("Unable to instantiate indexService");
        }

        String tmpPath = conf.getString(IndexConfig.DATA_DIR);
        tmpIndexPath =  tmpPath + File.separator + "tmp_indexes";
        zippedIndexPath = tmpPath + File.separator + "zipped_indexes";
    }

    private String getCachePath (Job job) {
        try {
            return cache.getPath(Long.parseLong(job.getHint(IndexConfig.CONF_JOB_CACHE_ID)));
        } catch (IOException e) {
            log.error ("Failed to obtain path to job data in cache", e);
            return null;
        }
    }

    public Job processJob (Job job) throws JobFailedException {
        String indexPath = tmpIndexPath + File.separator + job.getName(); // path to store tmp index for this job

        try {
            indexService.startIndex(indexPath);
        } catch (Exception e) {
            log.error ("Failed to start indexService on path: " + indexPath);
            throw new RuntimeException("Failed to start indexService on path: " + tmpIndexPath + job.getName(), e);
        }

        // Extract records to memory and get and iterator over them
        List<Record> recordStore = new ArrayList();
        for (Record rec : (Iterable<Record>)job.getData()) {
            recordStore.add (rec);
        }
        Iterator records = recordStore.iterator();

        String xslt_url = job.getHint(IndexConfig.XSLT_URL);
        if (xslt_url == null) {
            throw new NullPointerException(IndexConfig.XSLT_URL + " job hint not set");
        }

        // Try an index the records
        String zipFileName = zippedIndexPath + File.separator + job + ".zip";
        ZippedFolder zippedIndex = null;
        try {
            log.info ("Indexing job: " + job + ", with hints:\n" + job.getHints());

            while (records.hasNext()) {
                Record rec = (Record) records.next();
                try {
                    if (!rec.isDeleted()) {
                         indexService.addXMLRecord(new String(rec.getContent(),"UTF-8"), rec.getId(), xslt_url);
                    }
                } catch (FaultyDataException e) {
                    log.error ("Error processing record " + rec + ". Skipping. " + e.getMessage(), e);
                }
            }

            indexService.optimizeAll();
            indexService.getDescriptor().writeDescription(indexPath);

            log.info ("Indexing of Job " + job + " done");

            log.info ("Sanity checking index");
            try {
                IndexReader.open (indexPath);
            } catch (Exception e) {
                new File (getCachePath(job)).renameTo(new File(getCachePath(job) + "-FAILED"));
                throw new JobFailedException("Index failed sanity check", e);
            }

            log.info ("Zipping index to " + zipFileName);
            // zippedIndex.iterator() will return a Splitter.iterator() which
            // magically splits the zipped file on fly when writing to consumers cache
            try {
                zippedIndex = new ZippedFolder (indexPath, zipFileName, false);
            } catch (FileAlreadyExistException e) {
                zippedIndex = new ZippedFolder (indexPath, zipFileName, true);
                log.warn ("Overwriting index " + new File(indexPath,zipFileName));
            }

            // Delete job from cache and delete uncompressed index
            try {
                IndexManipulationUtils.deletePath(indexPath);
                new File(getCachePath(job)).delete();
            } catch (IOException e) {
                log.error ("Error deleting temporary files for job " + job, e);
            }

            return new Job (zippedIndex, job.getHints(), job.getName());

        } catch (RemoteException e) {
            throw new JobFailedException ("Failed to index records", e);
        } catch (IndexServiceException e) {
            throw new JobFailedException ("Failed to index records", e);
        } catch (UnsupportedEncodingException e) {
            throw new JobFailedException ("Failed to index records", e);
        } catch (TransformerException e) {
            throw new JobFailedException ("Error writing search descriptor to " + indexPath, e);
        } catch (IOException e) {
            throw new JobFailedException ("Error in job " + job.getName(), e);
        }

    }

}



