/* $Id: UpdateWorkflow.java,v 1.6 2007/10/05 10:20:22 te Exp $
 * $Revision: 1.6 $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.lang.management.ManagementFactory;

import dk.statsbiblioteket.summa.storage.io.Access;
import dk.statsbiblioteket.summa.io.AddTask;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import dk.statsbiblioteket.summa.common.lucene.index.OldIndexField;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.util.qa.QAInfo;


import javax.management.*;
import javax.xml.transform.TransformerException;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
public class UpdateWorkflow {
    private static final String CONFIG = "index.properties.xml";

    private static TreeSet<String> tasks = new TreeSet<String>();

    private static final String XSLT_SUFFIX = "_xslt_url";
    private static final String IO_SERVICE = "_io_storage";
    private static final String ID_SUFFIX = "_last_id";
    private static final String TIME_SUFFIX = "_time";
    private static final String COMPLETE_SUFFIX = "withCluster";


    private static Log log = LogFactory.getLog(UpdateWorkflow.class);

    static final int qsize = 300;

    private static Access _io;

    private static LinkedList<String> idbuffer;

    private static List<String> deletedIDs;

    private static Properties p;

    private static IndexService[] indexers;

    static ArrayBlockingQueue<Runnable>[] queues;

    static int numberOfServices;

    private static URL proplocation;

    private static String indexPath;

    private static String index_with_cluster;

    private static String[] paths;

    private static ExecutorService[] services;


    public static void update() throws IOException, NotBoundException, RemoteException, InvalidPropertiesFormatException, MalformedObjectNameException, NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, IndexServiceException {


        long now = System.currentTimeMillis();
        deletedIDs = new ArrayList<String>();

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
        String compete = indexPath + p.get(COMPLETE_SUFFIX);



        Properties org_prop = null;

        //if (!hasClusterMap && buildCluster){
        //   org_prop = new Properties();
        //    org_prop.loadFromXML(loader.getResourceAsStream(CONFIG));
        //}



        //numberOfServices = Integer.parseInt((String)p.get("indexService"));



        paths = new String[1];

        queues = new ArrayBlockingQueue[1];
        services = new ExecutorService[1];
        indexers = new IndexService[1];

        new File(indexPath + File.separator + "update" + now).mkdir();

           paths[0] = indexPath + File.separator + "update" + now;

           queues[0] = new ArrayBlockingQueue<Runnable>(qsize);
           services[0] = new ThreadPoolExecutor(1,1,Long.MAX_VALUE, TimeUnit.MILLISECONDS, queues[0], new QueueFullHandler());
            try {
                indexers[0] = new IndexServiceImpl();
                indexers[0].startIndex(paths[0]);
            } catch (IndexServiceException e) {
                log.error(e);
            }





        log.info("Starting updating index:" + indexPath);


        for (Map.Entry<Object, Object> entry : p.entrySet()) {
            Object obj = entry.getKey();
            log.info(obj);
            if (obj instanceof String && ((String) obj).endsWith("_run")) {
                String target = ((String) obj).substring(0, ((String) obj).indexOf("_run"));
                if (Boolean.parseBoolean((String) p.get(target + "_run"))) {
                    tasks.add(target);
                    log.info("adding target to update " + target);
                }
            }
        }




        for (String s: tasks){
            Progress.setCurrentOperation("updating target:" + s);
            updateTaget(s);
        }
        indexers[0].closeAll();

        Progress.setCurrentOperation("merge update with org indexes");
        String clusterInPath = "";
        boolean hasCluster = false;
        IndexReader inR = IndexReader.open(compete);
        IndexSearcher s = new IndexSearcher(inR);
         IndexReader inClu = null;
        if (hasCluster){ inClu = IndexReader.open(clusterInPath); }
        QueryParser q = new QueryParser("id", new KeywordAnalyzer());
        for (String id : deletedIDs){
            try {
                Hits h = s.search(q.parse(id));
                if (h.length() >= 1){
                    int docnr = h.id(0);
                     inR.deleteDocument(docnr);
                     if (hasCluster) inClu.deleteDocument(docnr);
                }
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        inR.close();

        Directory[] d = new Directory[3];
        IndexWriter w = new IndexWriter(indexPath + File.separator + "mergedUpdated"+ now,null,true);
        d[0] = FSDirectory.getDirectory(new File(indexPath + File.separator + "update" + now), false);
        d[1] = FSDirectory.getDirectory(new File(compete), false);
        //todo  remove this hack;
        //d[2] = FSDirectory.getDirectory(new File("/home/summa/persistentFiles/index_new_nat/withCluster"), false);
        w.addIndexes(d);
        w.close();
        //return 1;



//        if (hasCluster)inClu.close();
//
//        IndexWriter wIn = new IndexWriter(indexPath, new StandardAnalyzer(), false);
//        IndexWriter wClu = null;
//        if(hasCluster){ wClu = new IndexWriter(clusterInPath, new StandardAnalyzer(), false);}
//        IndexReader upR = IndexReader.open (indexPath + File.separator + "update" + now);
//
//        int num = upR.numDocs();
//
//        for (int j = 0; j< num; j++){
//            Document d = upR.document(j);
//            Document clu = new Document();
//            Field f = new Field("cluster","", Field.Store.NO,Field.Index.TOKENIZED);
//            clu.add(f);
//            wIn.processPayload(d);
//            if (hasCluster)wClu.processPayload(clu);
//        }
//
//        //wIn.optimize();
//        if(hasCluster)wClu.optimize();
//
//        wIn.close();
//
//
//
//        wClu.close();

        Progress.setCurrentOperation("Update done");

        writeSearchDescriptor();

    }



    static void mergeOrgWithUpdate(String orgPath, String updatePath, String clusterPath) throws IOException {


        IndexReader inR = IndexReader.open(orgPath);
        IndexSearcher s = new IndexSearcher(inR);
        IndexReader inClu = IndexReader.open(clusterPath);
        QueryParser q = new QueryParser("recordID", new KeywordAnalyzer());
        for (String id : deletedIDs){
            try {
                Hits h = s.search(q.parse(id));
                if (h.length() >= 1){
                    int docnr = h.id(0);
                     inR.deleteDocument(docnr);
                     inClu.deleteDocument(docnr);
                }
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        inR.close();
        inClu.close();

        IndexWriter wIn = new IndexWriter(orgPath, new StandardAnalyzer(), false);
        IndexWriter wClu = new IndexWriter(clusterPath, new StandardAnalyzer(), false);
        IndexReader upR = IndexReader.open (updatePath);

        int num = upR.numDocs();

        for (int j = 0; j< num; j++){
            Document d = upR.document(j);
            Document clu = new Document();
            Field f = new Field("cluster","", Field.Store.NO,Field.Index.TOKENIZED);
            clu.add(f);
            wIn.addDocument(d);
            wClu.addDocument(clu);
        }

        wIn.optimize();
        wClu.optimize();

        wIn.close();
        wClu.close();

        Progress.setCurrentOperation("Update done");

        writeSearchDescriptor();



    }

    private static void updateTaget(String s) throws MalformedURLException, NotBoundException, RemoteException {
           long lastModified = 0;
            String xslt_url = (String)p.get(s + XSLT_SUFFIX);
            _io = (Access) Naming.lookup((String)p.get(s + IO_SERVICE));
            log.info("processing target " + s + "using xslt " + xslt_url);
            Iterator iter;
            String lon = (String)p.get(s+TIME_SUFFIX);
            if (lon != null && !"".equals(lon)){
                lastModified = Long.parseLong(lon);
            }
            if (lastModified <= 0){
                return;
            }
             log.info("getReecords:" + s +":"+lastModified);
             iter =_io.getRecordsModifiedAfter(lastModified, s);

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
                long time = r.getLastModified();
                try {
                    if(r.isDeleted()){
                            services[use].submit(new AddTask(indexers[use],xslt_url,new String(r.getContent(), "utf-8"), r.getId()));
                    }
                    deletedIDs.add(r.getId());
                    log.info("scheduled for delete in org index: " + r.getId() );
                    Progress.count();
                } catch (UnsupportedEncodingException e) {
                    log.error(e);
                } catch (Exception e){
                    log.error(e.getMessage());
                }
                if (idbuffer.size() == qsize*numberOfServices){
                    p.setProperty(s + ID_SUFFIX, idbuffer.poll());
                }
                if (time > lastModified){
                    p.setProperty(s + TIME_SUFFIX, "" + time);
                    lastModified = time;
                }

            }

        updateProperties();

    }

    private static void writeSearchDescriptor() throws IOException {
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

                d_full.setSingleFields( singleFields);
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



