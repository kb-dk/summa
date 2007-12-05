/* $Id: IndexManipulationUtils.java,v 1.5 2007/10/05 09:31:30 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/05 09:31:30 $
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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.*;

import dk.statsbiblioteket.summa.common.lucene.index.IndexField;
import dk.statsbiblioteket.summa.common.lucene.index.SearchDescriptor;
import dk.statsbiblioteket.util.qa.QAInfo;

import javax.xml.transform.TransformerException;

/**
 * All purpose static utility methods for index jugglers
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal, te")
public class IndexManipulationUtils {

    private static Log log = LogFactory.getLog(IndexManipulationUtils.class);


/*    public static void main(String args[]){
        String[] searchdec = new String[]{"/home/hal/descriptorTest/0","/home/hal/descriptorTest/1","/home/hal/descriptorTest/2","/home/hal/descriptorTest/3"};
        try {
            writeSearchDescriptor(searchdec, "/home/hal/descriptorTest");
        } catch (IOException e) {
            log.error(e);
        } catch (TransformerException e) {
            log.error(e);
        }
    }*/

    public static void main(String args[]){
        //String[] searchdec = new String[]{"/home/hal/descriptorTest/0","/home/hal/descriptorTest/1","/home/hal/descriptorTest/2","/home/hal/descriptorTest/3"};
        //writeSearchDescriptor(searchdec, "/home/hal/descriptorTest");
        fullMerge(args);

    }

    /**
     * This method is designed to be directly invoked with the args[] from the main method.
     * It merges all indexes found in the arguments and merges them into the directory given
     * as the last argument. It also merges search descriptors of the given indexes.
     * @param args command line arguments as passed to main
     */
    public static void fullMerge (String[] args) {
        if (args.length < 3) {
                System.out.println ("USAGE:\n\t<index_1 index_2 ... index_n> <outputDir>");
                System.exit (1);
        }

        String[] indexes = new String[args.length-1];
        for (int i = 0; i < args.length - 1; i++) {
            indexes[i] = args[i];
        }

        System.out.println ("Merging: ");
        for (String index : indexes) {
            System.out.println ("\t+ " + index);
        }
        System.out.println (" > "  + args[args.length -1]);

        try {
            mergeIndexes(indexes, args[args.length -1]);
            mergeSearchDescriptors(indexes, args[args.length -1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Merge a collection of lucene indexes into the same index
     * @param indexDirs lucene indexes to be merged
     * @param outputDir place to store the merged index
     * @throws IOException if the underlying {@link IndexWriter} fails
     */
    public static void mergeIndexes (String[] indexDirs, String outputDir) throws IOException {
        Directory[] d = new Directory[indexDirs.length];
        IndexWriter w = new IndexWriter(outputDir , null, true);
        w.setMergeFactor(25);
        w.setMaxBufferedDocs(35000);
        w.setMaxFieldLength(Integer.MAX_VALUE);
        int count = 0;
        for (String dir : indexDirs) {
            d[count] = FSDirectory.getDirectory(dir);
            if (d[count] == null) {
                log.warn("FSDirectory.getDirectory for \"" + dir
                          + "\" should not be null. Retrying...");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.debug("Could not sleep for 100 ms");
                }
                d[count] = FSDirectory.getDirectory(dir);
                if (d[count] == null) {
                    log.error("FSDirectory.getDirectory for \"" + dir
                              + "\" is still null");
                    throw new IOException("Could not get directory for \""
                                          + dir + "\"");
                }
            }
            count++;
        }
        w.addIndexes(d); // This call also optimize the new index
        w.close();
    }

    /**
     * Read {@link SearchDescriptor}s from a collection of directories and merge them
     * in a SearchDescriptor stored in a specified output directory.
     * @param indexDirs directories with {@link SearchDescriptor}s
     * @param outputDir place to store the merged descriptor
     * @throws IOException if failing to write merged descriptor
     */
    public static void mergeSearchDescriptors (String[] indexDirs, String outputDir) throws IOException {

        TreeSet<IndexField> singleFields = new TreeSet<IndexField>();
        HashMap<String, SearchDescriptor.Group> groups = new HashMap<String, SearchDescriptor.Group>();

        for (String dir : indexDirs){

                SearchDescriptor dec = new SearchDescriptor (dir);
                dec.loadDescription(dir);

                // Add single fields
                singleFields.addAll(dec.getSingleFields());

                // Update groups
                if (dec.getGroups() != null){
                    Set<Map.Entry<String,SearchDescriptor.Group>> s =  dec.getGroups().entrySet();
                    for (Map.Entry<String,SearchDescriptor.Group> e : s){
                             SearchDescriptor.Group g_old =  groups.get(e.getKey());
                             if (g_old != null){
                                 TreeSet<IndexField> old_fields = g_old.getFields();
                                 old_fields.addAll(e.getValue().getFields());
                                 e.getValue().setFields(old_fields);
                             }
                                 groups.put(e.getKey(), e.getValue());
                    }
                }
        }

        SearchDescriptor d_full = new  SearchDescriptor(outputDir);

        d_full.setSingleFields(singleFields);
        d_full.setGroups(groups);
        try {
            d_full.writeDescription(outputDir);
        } catch (TransformerException e) {
            log.error(e);
        }
    }

    /**
     * Delete the file or directory given by <code>path</code> (recursively if
     * <code>path</code> is a directory).
     * @param path
     * @throws java.io.FileNotFoundException if the path doesn't exist
     */
    public static void deletePath (String path) throws FileNotFoundException {
        File f = new File (path);
        if (!f.exists()) {
            throw new FileNotFoundException(path);
        }
        if (f.isFile()) {
            f.delete();
            return;
        }
        for (String child : f.list()) {
            deletePath(path + File.separator + child);
        }
        f.delete();
    }

    static void mergeOrgWithUpdate(ArrayList<String> updatedIDS, String orgPath, String updatePath, String clusterPath) throws IOException {


        IndexReader inR = IndexReader.open(orgPath);
        IndexSearcher s = new IndexSearcher(inR);
        IndexReader inClu = IndexReader.open(clusterPath);
        QueryParser q = new QueryParser("id", new KeywordAnalyzer());
        for (String id : updatedIDS){
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

        //Progress.setCurrentOperation("Update done");

        //writeSearchDescriptor();
    }

    private static void writeSearchDescriptor(String[] descriptorsToMerge, String writeToPath) throws IOException, TransformerException {
        int numberOfDescriptors = descriptorsToMerge.length;
        TreeSet<IndexField> singleFields = new TreeSet<IndexField>();
        HashMap<String, SearchDescriptor.Group> groups = new HashMap<String, SearchDescriptor.Group>();

        for (int j= 0; j<numberOfDescriptors; j++){

                SearchDescriptor dec = new SearchDescriptor(descriptorsToMerge[j]);
                dec.loadDescription(descriptorsToMerge[j]);
                singleFields.addAll(dec.getSingleFields());
                if (dec.getGroups() != null){
                    Set<Map.Entry<String,SearchDescriptor.Group>> s =  dec.getGroups().entrySet();
                    for (Map.Entry<String,SearchDescriptor.Group> e : s){
                             SearchDescriptor.Group g_old =  groups.get(e.getKey());
                             if (g_old != null){
                                 TreeSet<IndexField> old_fields = g_old.getFields();
                                 old_fields.addAll(e.getValue().getFields());
                                 e.getValue().setFields(old_fields);
                             }
                                 groups.put(e.getKey(), e.getValue());
                    }
                }
        }

        SearchDescriptor d_full = new  SearchDescriptor(writeToPath);
        d_full.setSingleFields( singleFields);
        d_full.setGroups(groups);
        d_full.writeDescription(writeToPath);
    }


}
