/* $Id: Workflow.java,v 1.9 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.9 $
 * $Date: 2007/10/05 10:20:22 $
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
package dk.statsbiblioteket.summa.index;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dk.statsbiblioteket.summa.io.AddTask;
import dk.statsbiblioteket.summa.io.DeleteTask;
import dk.statsbiblioteket.summa.io.UpdateTask;
import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.common.lucene.index.OldIndexField;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, mke, te")
public class Workflow {



    private static final String CONFIG = "index.properties.xml";

    private static TreeSet<String> tasks = new TreeSet<String>();

    private static final String XSLT_SUFFIX = "_xslt_url";
    private static final String IO_SERVICE = "_io_storage";
    private static final String ID_SUFFIX = "_last_id";
    private static final String TIME_SUFFIX = "_time";
    private static final String COMPLETE_SUFFIX = "_complete_full_index";


    private static Log log = LogFactory.getLog(Workflow.class);

    static final int qsize = 300;

    private static Access _io;

    private static LinkedList<String> idbuffer;

    private static Properties p;

    private static IndexService[] indexers;

    static ArrayBlockingQueue<Runnable>[] queues;

    static int numberOfServices;

    private static URL proplocation;

    private static String indexPath;

    private static String index_with_cluster;

    private static String[] paths;

    private static ThreadPoolExecutor[] services;

    private static int recordCount = 0;

    private static boolean doTrace = log.isTraceEnabled();

    public static void main(String args[]) throws IOException, NotBoundException, RemoteException, InvalidPropertiesFormatException, MalformedObjectNameException, NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, IndexServiceException {

        if (args.length > 0 && "update".equals(args[0])){
             UpdateWorkflow.update();
             return;
        }

        Progress progress = new Progress();

        MBeanServer mbserver = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(Progress.class.getName() +
                ":type=IndexProgress");

        mbserver.registerMBean(progress, name);


        idbuffer = new LinkedList<String>();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        proplocation = loader.getResource(CONFIG);

        p = new Properties();
        p.loadFromXML(loader.getResourceAsStream(CONFIG));


        indexPath = (String)p.get("indexPath");
        index_with_cluster = indexPath + p.get("withCluster");

        boolean resume = Boolean.parseBoolean((String)p.get("resume"));
        boolean buildCluster = Boolean.parseBoolean((String)p.get("buildCluster"));
        boolean hasClusterMap = Boolean.parseBoolean((String)p.get("clusterMapGenerated"));


        Properties org_prop = null;

        if (!hasClusterMap && buildCluster){
            org_prop = new Properties();
            org_prop.loadFromXML(loader.getResourceAsStream(CONFIG));
        }



        numberOfServices = Integer.parseInt((String)p.get("indexService"));
        paths = new String[numberOfServices];

        queues = new ArrayBlockingQueue[numberOfServices];

        services = new ThreadPoolExecutor[numberOfServices];
        indexers = new IndexService[numberOfServices];


        for (int i = 0 ;i<numberOfServices; i++){
           paths[i] = indexPath + File.separator + i;
           queues[i] = new ArrayBlockingQueue<Runnable>(qsize);
           services[i] = new ThreadPoolExecutor(1,1,Long.MAX_VALUE, TimeUnit.MILLISECONDS, queues[i], new QueueFullHandler());
            try {
                indexers[i] = new IndexServiceImpl();
                if (resume) { indexers[i].resumeIndex(paths[i]); }
                else { indexers[i].startIndex(paths[i]); }
            } catch (IndexServiceException e) {
                log.error(e);
            }
        }




        log.info("Starting indexing into index:" + indexPath + " (starting with resume " + resume + ")");


        for (Map.Entry<Object, Object> entry : p.entrySet()) {
            Object obj = entry.getKey();
            log.info(obj);
            if (obj instanceof String && ((String) obj).endsWith("_run")) {
                String target = ((String) obj).substring(0, ((String) obj).indexOf("_run"));
                if (Boolean.parseBoolean((String) p.get(target + "_run"))) {
                    tasks.add(target);
                    log.info("adding target to index " + target);
                }
            }
        }




        for (String s: tasks){
            Progress.setCurrentOperation("Indexing target:" + s);
            indexTarget(s,resume);
        }
        Progress.setCurrentOperation("Merging indexes");
        mergeIndex("full");

        writeSearchDescriptor();

        System.exit(0);

    }


    private static void indexTarget(String target, boolean resume) throws MalformedURLException, NotBoundException, RemoteException {
            long lastModified = 0;
            String xslt_url = (String)p.get(target + XSLT_SUFFIX);
            _io = (Access) Naming.lookup((String)p.get(target + IO_SERVICE));
            log.info("processing target " + target + "using xslt " + xslt_url);
            Iterator iter;
            boolean comp= false;
            boolean update = false;
            long time;
            String lon = (String)p.get(target+TIME_SUFFIX);

            if (lon != null && !"".equals(lon)){
                lastModified = Long.parseLong(lon);
            }

            String complete = (String)p.get(target + COMPLETE_SUFFIX);
            if (complete != null) {
                comp = Boolean.parseBoolean(complete);
            }
            if (!resume){
                log.info("getRecords:" + target);
                iter = _io.getRecords(target);
            } else if (!comp){
                log.info("getRecords:" + (String)p.get(target + ID_SUFFIX));
                iter = _io.getRecordsFrom((String)p.get(target + ID_SUFFIX),target);
            } else {
               log.info("getRecords:" + target +":"+lastModified);
                iter =_io.getRecordsModifiedAfter(lastModified, target);
                update = true;
            }

            while (iter.hasNext()){
                int use = nextService();
                if (queues[use].remainingCapacity() < 100){
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            log.error(e);
                        }
                }
                Record r = (Record)iter.next();
                String id = r.getId();
                idbuffer.add(id);
                if (doTrace) { log.trace ("added " + id + " to indexbuffer"); }
                time = r.getLastModified();
                try {
                    if(!r.isDeleted()){
                        // TODO: Check if this was the right successor to the line below
//                    if(!r.isDeleted() Status.STORED.equals(r.getState())){
                        if (update) {
                            services[use].submit(new UpdateTask(indexers[use],xslt_url,new String(r.getContent(), "utf-8"), r.getId()));
                            recordCount++;
                        } else {
                            // Speed optimisation that assumes the index does not have the record.
                            // If the record is already present, it will be indexed twice.
                            services[use].submit(new AddTask(indexers[use],xslt_url,new String(r.getContent(), "utf-8"), r.getId()));
                            recordCount++;
                        }
                    } else if (r.isDeleted()) {
                            services[use].submit(new DeleteTask(indexers[use], r.getId()));
                            recordCount++;
                    }
                    Progress.count();
                } catch (UnsupportedEncodingException e) {
                    log.error(e);
                } catch (Exception e){
                    log.error(e.getMessage());
                }
                if (idbuffer.size() == qsize*numberOfServices){
                    p.setProperty(target + ID_SUFFIX, idbuffer.poll());
                }
                if (time > lastModified){
                    p.setProperty(target + TIME_SUFFIX, "" + time);
                    lastModified = time;
                }
                updateProperties();
            }
            p.setProperty(target + COMPLETE_SUFFIX, "true");
            updateProperties();
    }




    private static int mergeIndex(String folder) throws IOException {
        log.trace("merging index to:" +  folder);
        long completed = 0;
        for (ThreadPoolExecutor e: services ){
            completed += e.getCompletedTaskCount();
        }
        if (completed<recordCount){
            log.trace("not ready to merge waiting");
            try {
                Thread.currentThread().sleep(1000);
            } catch (InterruptedException e) {
                log.info("thread interrupted");
            }
            mergeIndex(folder);
        }

        for (ExecutorService e: services){
            e.shutdown();
        }
        for (ExecutorService e: services){
            try {
                if (!e.awaitTermination(60*60*24, TimeUnit.SECONDS)){
                    log.fatal("index task didn't finish within 1 day- terminating");
                    System.exit(-1);
                }
            } catch (InterruptedException e1) {
                log.error("Indextask interrupted, index not complete");
            }
        }
        //optimizing all indexex and merge them in full index
        Directory[] d = new Directory[numberOfServices];
        IndexWriter w = new IndexWriter(indexPath + File.separator + folder,null,true);
        for (int j= 0; j<numberOfServices; j++){
            try {
                indexers[j].optimizeAll();
                d[j] = FSDirectory.getDirectory(paths[j], false);
            } catch (IndexServiceException e) {
                log.error(e);
            }
        }
        w.addIndexes(d);
        w.optimize();
        w.close();
        return 1;
    }


    private static void writeSearchDescriptor() throws IOException {
        log.trace("writing search descriptiors");
        TreeSet<OldIndexField> singleFields = new TreeSet<OldIndexField>();
        HashMap<String, SearchDescriptor.Group> groups = new HashMap<String, SearchDescriptor.Group>();

        for (int j= 0; j<numberOfServices; j++){

                SearchDescriptor dec = indexers[j].getDescriptor();
                singleFields.addAll(dec.getSingleFields());
                if (dec.getGroups() != null){
                    Set<Map.Entry<String,SearchDescriptor.Group>> s =  dec.getGroups().entrySet();
                    for (Map.Entry<String,SearchDescriptor.Group> e : s){
                             SearchDescriptor.Group g_old =  groups.get(e.getKey());
                             if (g_old != null){
                                 TreeSet<OldIndexField> old_fields = g_old.getFields();
                                 old_fields.addAll(e.getValue().getFields());
                                 e.getValue().setFields(old_fields);
                             }
                                 groups.put(e.getKey(), e.getValue());
                    }
                }

                SearchDescriptor d_full = new  SearchDescriptor(indexPath + File.separator + "full");

                d_full.setSingleFields(singleFields);
                d_full.setGroups(groups);
            try {
                d_full.writeDescription(indexPath + File.separator + "full");
            } catch (TransformerException e) {
                log.error(e);
            }


        }
    }

    private  IndexService[] getIndexServives(boolean resume, String paths[]) throws RemoteException {
        IndexService[] indexers = new IndexService[numberOfServices];
        for (int i = 0 ;i<numberOfServices; i++){
           //paths[i] = indexPath + File.separator + i;
           //queues[i] = new ArrayBlockingQueue<Runnable>(qsize);
           //services[i] = new ThreadPoolExecutor(1,1,Long.MAX_VALUE, TimeUnit.MILLISECONDS, queues[i], new QueueFullHandler());
            try {
                indexers[i] = new IndexServiceImpl();
                if (resume) { indexers[i].resumeIndex(paths[i]); }
                else { indexers[i].startIndex(paths[i]); }
            } catch (IndexServiceException e) {
                log.error(e);
            }
        }
        return indexers;
    }



    private static void updateProperties(){
        String propFile;
        if ("".equals(propFile = proplocation.getFile())) {
            log.error("no place to store properties");
        }
        try {
            //String newFileName = propFile + ".new";
            FileOutputStream fs = new FileOutputStream(propFile);
            p.storeToXML(fs,null);
            fs.close();
            //new File(propFile).renameTo(new File(propFile+".bak"));
            //new File(newFileName).renameTo(new File(propFile));
        } catch (FileNotFoundException e) {
            log.error("Store properties failed", e);
        } catch (IOException e) {
            log.error("Store properties failed", e);
        }

    }


    /**
     * Select the queue with the largest remaining capacity.
     * @return the index of best suitet queue in the queue array;
     */
    private static int nextService(){
        int buf = 0, ret =0;
        for (int i = 0; i<numberOfServices; i++){
             int j = queues[i].remainingCapacity();
             if (j>buf) { ret = i; buf = j; }
        }
        return ret;
    }





}
