/* $Id: Merger.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
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

import dk.statsbiblioteket.summa.dice.Job;
import dk.statsbiblioteket.summa.dice.Config;
import dk.statsbiblioteket.summa.dice.CachingConsumer;
import dk.statsbiblioteket.summa.dice.JobStatus;
import dk.statsbiblioteket.summa.dice.util.ZippedFolder;
import dk.statsbiblioteket.summa.dice.util.SimpleLog;
import dk.statsbiblioteket.summa.dice.util.FileAlreadyExistException;
import dk.statsbiblioteket.summa.index.IndexManipulationUtils;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.rmi.RemoteException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This implementation is mostly stateless to ease resuming of failed runs.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class Merger extends CachingConsumer {


    public static int MAX_MERGE = 28;
    private String MERGE_DIR;
    private String MERGE_DIR_PREFIX = "index.";
    private String MERGE_LOG_FILE = "merge.log";
    private String fullIndex;
    private Config conf;

    private Thread mergeSlave;
    private SimpleLog mergeLog;

    public static class MergeTask {
        public String[] targets;
        public String outputDir;
        public String jobName;

        public MergeTask (String[] targets, String outputDir) {
            this.targets = targets;
            this.outputDir = outputDir;
        }

        public String toString () {
            String result = outputDir + " < ";
            for (String target : targets) {
                result += "\n + " + target;
            }
            return result.substring(0, result.length() - 3);
        }
    }

    public class MergeSlave implements Runnable {
        public void run ()  {

            //MergeSlave Mainloop
            while (true) {
                MergeTask mt = null;
                boolean employerFinished = false;

                // Get Employer status
                try {
                    employerFinished = getEmployer().isFinished();
                } catch (RemoteException e) {
                    log.warn ("Failed to obtain Employer status. Sleeping.");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e1) {
                        log.warn ("MergeSlave interrupted. Stopping.");
                        break;
                    }
                }

                // Get next merge task (possibly an upmerge)
                try {
                    mt = getNextMergeTask(MERGE_DIR);
                    if (mt == null && employerFinished) {
                        // Check for upmerges
                        mt = getNextUpmergeTask(MERGE_DIR);
                    }
                } catch (RemoteException e) {
                        log.error ("Failed to get Employer status", e);
                } catch (IOException e) {
                    log.error ("Failed to get next merge task", e);
                    continue;
                }

                // Check if we are done
                if (mt == null) {
                    if (employerFinished) {
                        // No merges or upmerges and Employer reports finished; we are done
                        log.info ("Employer reports finished. MergeSlave is done.");
                        break;
                    } else {
                        // There are no merges, but the employer has not reported finished. Sleep and retry.
                        try {
                            log.info ("Waiting for jobs");
                            Thread.sleep (10000);
                            continue;
                        } catch (InterruptedException e) {
                            log.warn("MergeSlave interrupted. Stopping.");
                            break;
                        }
                    }
                }

                // Handle the MergeTask
                log.info ("Merging: " + mt);
                try {
                    processMergeTask (mt);
                } catch (Exception e) {
                    log.error ("Error during merge: " + mt, e);
                    // We can't really know what went wrong, so we have to mark all
                    // involved indexes as failed
                    for (String source : mt.targets) {
                        setJobStatus(new JobStatus(getFileBaseName(source), JobStatus.Status.JOB_LOST));
                    }
                }

                // Delete processed indexes so we don't pick them up again
                for (String idx : mt.targets) {
                    try {
                        IndexManipulationUtils.deletePath(idx);
                    } catch (IOException e) {
                        log.error ("Error deleting " + idx, e);
                    }
                }
            }
            log.info ("MergeSlave exiting");
        }
    }

    public Merger (Config conf) throws RemoteException {
        super (conf);
        this.conf = conf;
        MERGE_DIR = conf.get (IndexConfig.DATA_DIR) + File.separator +"wec_merges";
        fullIndex = conf.get (IndexConfig.DATA_DIR) + File.separator +"full_index";
        mergeLog = new SimpleLog(conf.get(IndexConfig.DATA_DIR) + File.separator + MERGE_LOG_FILE);
        mergeSlave = new Thread (new MergeSlave(), "MergeSlave");
        
        log.info ("Starting MergeSlave");
        mergeSlave.start();
    }


    public void processMergeTask(MergeTask mt) throws IOException {
        mergeLog.log ("Doing merge:\n" + mt.toString());
        IndexManipulationUtils.mergeIndexes(mt.targets, mt.outputDir);
        IndexManipulationUtils.mergeSearchDescriptors(mt.targets, mt.outputDir);
        mergeLog.log ("Merge done: " + mt.outputDir);
    }

    public void processJob (Job job) {
        // Unzip job from cache to merging queue ($DATA_DIR/wec_merges/index.0)
        String cacheFile = null;
        String output = null;

        try {
            cacheFile = cache.getPath(Long.parseLong(job.getHint(CONF_JOB_CACHE_ID)));
        } catch (IOException e) {
            log.error ("Error getting cache file for job " + job, e);
            return;
        }

        try {
            output = new File(getTempIndexName(MERGE_DIR, 0, job.getName())).getParent();
            try {
                ZippedFolder.unzip(cacheFile, output, false);
            } catch (FileAlreadyExistException e) {
                log.warn ("Target index " + getTempIndexName(MERGE_DIR, 0, job.getName()) + " already exists. Overwriting");
                ZippedFolder.unzip(cacheFile, output, true);
            }
        } catch (IOException e) {
            log.error ("Failed to extract subindex " + cacheFile + " to " + output, e);
        }

        // The base class (ConsumerBase) will now report the job as CONSUMED
        // to the Employer
    }

    /**
     * Wait for the MergeSlave to exit, and then locate the unique index of highest level
     * and mark that as the final.
     */
    public void close () {
        log.info ("Closing");
        try {
            mergeSlave.join();
        } catch (InterruptedException e) {
            log.warn ("Joining of mergeSlave interrupted. Undertermined index state.");
            return;
        }
        log.info ("Merge slave has stopped. Finding final index.");

        // Find index of highest level
        int level = 0;
        while (true) {
            File mergeDir = new File (MERGE_DIR, MERGE_DIR_PREFIX + (level+1));
            if (!mergeDir.exists()) {
                // level is the highest index level
                break;
            }
            level++;
        }

        File lastMerge = new File (MERGE_DIR, MERGE_DIR_PREFIX + level);
        String[] indexes = lastMerge.list();

        if (indexes.length > 1) {
            log.error(lastMerge + " contains more than one index. Bailing out.");
            return;
        } else if (indexes.length == 0) {
            log.error (lastMerge + " does not contain any indexes. Bailing out.");
            return;
        }

        File finalIndex = new File (lastMerge, indexes[0]);
        log.info ("Moving " + finalIndex + " to " + fullIndex);
        finalIndex.renameTo(new File(fullIndex));
        log.info ("Index complete.");

        //log.info ("Transfering indexes to remote servers");
        //transferIndex();
        try {
            File idxLocator = new File (System.getProperty("user.home"), "index.locator");
            log.info ("Writing index locator: " + idxLocator);        
            idxLocator.delete();
            PrintWriter locWriter = new PrintWriter(new FileWriter(idxLocator));
            locWriter.println(fullIndex);
            locWriter.flush();
            locWriter.close();
        } catch (IOException e) {
            log.error ("Error writing index locator", e);
        }
        
        
        // Spawn a thread to close the consumers jvm after 5 seconds
        Runnable systemKiller = new Runnable () {
            public void run () {
                try {
                    log.info ("Consumer is exiting in 5 seconds");
                    Thread.sleep(5000);
                    log.info ("Stopping JVM. Bye.");
                    System.exit (0);
                } catch (InterruptedException e) {
                    log.warn ("System shutdown interrupted. The JVM might hang.");
                }
            }         
        };        
        new Thread (systemKiller).run();

    }

    /**
     * scp the generated index to a list of target uris
     */
    /*private void transferIndex() {
        String remoteFilename = "~/tmp/" + conf.getStartDate();
        String uris[] = {"summa@debit", "summa@kredit"};
        
        int count = 0;
        for (String uri : uris) {
            String remoteTarget = uri + ":" + remoteFilename;            
            
			String createDirCommand = "ssh " + uri + " 'mkdir -p " + remoteFilename + "'";
            String command = "scp -r " + fullIndex + " " + remoteTarget;
            try {
            	// Ensure target dirs exists
            	log.info ("Creating remote target directory: " + createDirCommand);
            	Runtime.getRuntime().exec(createDirCommand).waitFor();
            	
            	log.info ("Transfering index to " + remoteTarget);
                final Process transfer = Runtime.getRuntime().exec(command);
                // IMPORTANT: This process will block unless its output stream is flushed

                Runnable streamFlusher = new Runnable() {
                    // Local thread to flush the output stream of each sub process

                    public void run () {
                        BufferedInputStream in = new BufferedInputStream (transfer.getInputStream());                        
                        byte buf[] = new byte[1024];
                        try {
                            while (in.read(buf, 0, buf.length) != -1) {
                                // Ignore stdout of the subprocess
                            }
                        } catch (IOException e) {
                            log.error ("Error flushing stdout of subprocess", e);                            
                        }
                    }

                };
                
                new Thread (streamFlusher).start();
                int exitCode = transfer.waitFor();
                if (exitCode != 0) {
                    log.error ("Command \"" + command + "\" returned with exit code " + exitCode);
                    
                    // Build an error message from the error stream and log it								
					try {
						BufferedInputStream err = new BufferedInputStream (transfer.getErrorStream());
						String errMsg = "";
						byte buf[] = new byte[1024];
						while (err.read(buf, 0, buf.length) != -1) {
							errMsg += new String(buf);
						}
						log.error ("Sub process said: " + errMsg);
					} catch (Exception e) {
						log.error ("Error reading error-stream of subprocess", e);
					}
                }
                
                if (count == 0) {                    
                    String clusterCommand = "/home/summa/cluster/full_cluster.sh " + remoteFilename;
                    String remoteCommand = "ssh -qf " + uri + " \"" + clusterCommand + "\"";
                    log.info ("Starting remote cluster generation: " + remoteCommand);
                    Runtime.getRuntime().exec(remoteCommand);
                }
            } catch (Exception e) {
                log.error ("Error executing command \"" + command + "\":", e);
            }
            
            count++;
        }
    }*/

    /**
     * Scan baseDir and find from two up to MAX_MERGE indexes from the same level to merge
     * up to the next level.
     * @param baseDir dir to scan for index levels
     * @return a merge task with info needed for running {@link IndexManipulationUtils#mergeIndexes(String[], String)}. Return null if there are no tasks
     */
    public MergeTask getNextMergeTask (String baseDir) {
        ArrayList<String> targets = new ArrayList<String>();
        String idxName = null;
        int i = -1;
        while (true) {
            // Check next valid dir
            i ++;
            File mergeDir = new File (baseDir, MERGE_DIR_PREFIX + i);
            if (!mergeDir.exists()) {
                return null;
            }
            if (!mergeDir.isDirectory()) {
                log.warn("Garbage file in merge dir: " + mergeDir);
                continue;
            }

            // Trim out non-consumed jobs since they are not fully unpacked yet
            ArrayList<String> candidates = new ArrayList<String>();
            for (String dir : mergeDir.list()){
                try {
                    JobStatus status = getEmployer().getJobStatus(dir);
                    if (status != null ) {
                        if (status.getStatus().equals (JobStatus.Status.JOB_CONSUMED)) {
                            candidates.add(dir);
                        }
                    }
                } catch (RemoteException e) {
                    log.error("Unable to get jobStatus: " ,e);
                }
            }

            String[] pending = candidates.toArray(new String[candidates.size()]);

            // If there are 1 or 0 pending indexes to be merged at this level
            // we can't do anything yet
            if (pending.length <= MAX_MERGE-1) {
                continue;
            }

            // There are >1 pending indexes in this level
            // return the first MAX_MERGE items
            int count = 0;
            for (String idx : pending) {
                if (count >= MAX_MERGE) {
                    break;
                }
                if (count == 0) {
                    idxName = idx;
                }
                targets.add (mergeDir.getAbsolutePath() + File.separator + idx);
                count++;
            }

            break;

        }
        return new MergeTask (targets.toArray(new String[targets.size()]), getTempIndexName(MERGE_DIR, ++i, idxName));
    }

    /**
     * An Upmerge is when the lowest occupied level contains exactly one item.
     * @param baseDir
     * @return A MergeTask merging everything into one index. Return null if there are no tasks
     * @throws IOException
     */
    public MergeTask getNextUpmergeTask (String baseDir) throws IOException {
        int i = -1;
        ArrayList<String> candidates = new ArrayList<String>();
        while (true) {
            // Check next valid dir
            i ++;
            File mergeDir = new File (baseDir, MERGE_DIR_PREFIX + i);
            if (!mergeDir.exists()) {
                // We've reached the final level
                if (candidates.size() == 1) {
                    // There's only one index in the entire tree. This is the final index
                    return null;
                } else if (candidates.size() == 0) {
                    log.fatal("There are no indexes in the tree!");
                    return null;
                } else {
                    // We have found some cadidates, exit loop and package them in a MergeTask
                    break;
                }
            }
            if (!mergeDir.isDirectory()) {
                log.warn("Garbage file in merge dir: " + mergeDir);
                continue;
            }

            // Trim out non-consumed jobs since they are not fully unpacked yet
            for (String dir : mergeDir.list()){
                try {
                    JobStatus status = getEmployer().getJobStatus(dir);
                    if (status != null ) {
                        if (status.getStatus().equals (JobStatus.Status.JOB_CONSUMED)) {
                            candidates.add(getTempIndexName(baseDir, i, dir));
                        } else {
                            log.error ("Job " + status.getName() + " not marked as finished. Job lost.");
                            setJobStatus(new JobStatus(status.getName(), JobStatus.Status.JOB_LOST));
                        }
                    }
                } catch (RemoteException e) {
                    log.error("Unable to get jobStatus: " ,e);
                }
            }

            /*String[] pending = candidates.toArray(new String[candidates.size()]);

            if (pending.length == 0) {
                continue;
            } else if (pending.length > 1) {
                // The lowest occupied level contains >1 items, this does not qualify as an upmerge
                return null;
            }

            // Now pending.length == 1, find an item in a higher level.
            // If there is no higer level item return null. In this case
            // pending[0] is the only index in the the merge tree
            String lonelyItem = getTempIndexName(baseDir, i, pending[0]);
            while (true) {
                i++;
                File nextLevel = new File (baseDir, MERGE_DIR_PREFIX + i);
                if (!nextLevel.exists()) {
                    // No higher level items
                    return null;
                }
                if (!nextLevel.isDirectory()) {
                    log.warn("Garbage file in merge dir: " + mergeDir);
                    continue;
                }

                String[] nextLevelItems = nextLevel.list();
                if (nextLevelItems.length >= 1) {
                    // Set up a MergeTarget merging the lonely item into
                    // the first item in the next nonempty level

                    String mergeTarget = getTempIndexName(baseDir, i, nextLevelItems[0]);
                    File tmpMergeTarget = new File (mergeTarget);
                    tmpMergeTarget.renameTo(new File(mergeTarget + ".tmp")); // rename to avoid overwriting

                    String targets[] = new String[2];
                    targets[0] = lonelyItem;
                    targets[1] = mergeTarget + ".tmp";
                    return new MergeTask(targets, mergeTarget);
                }
            } */
        }

        String[] pending = candidates.toArray (new String[candidates.size()]);
        return new MergeTask(pending, getTempIndexName(MERGE_DIR, i, getFileBaseName(pending[0])));
    }

    /**
     * Get full pathname for storing and index with a given name on some specified level
     * @param baseDir base dir for index levels
     * @param level the level to store the index in
     * @param name name of the index
     * @return full path name for the index
     */
    private String getTempIndexName (String baseDir, int level, String name) {
        return baseDir + File.separator + MERGE_DIR_PREFIX + level + File.separator + name;
    }

    /**
     * Extract the base filename from a path. Fx. the base name of /hello/world.txt
     * is world.txt.
     *
     * For temporary indexes the base filename will be the job name of the job from
     * which it originates (useful for status reporting).
     * @param path
     * @return the last part of path after a separator
     */
    public static String getFileBaseName (String path) {
        // Note: This actually works even if lastIndexOf return -1
        return path.substring(path.lastIndexOf(File.separator) + 1);
    }

    public void stop () {
        super.stop();
        log.info("Stopping MergeSlave");
        mergeSlave.interrupt();
        try {
            mergeSlave.join();
        } catch (InterruptedException e) {
            log.error ("Interrupted while waiting for MergeSlave to exit");
        }
    }
}



