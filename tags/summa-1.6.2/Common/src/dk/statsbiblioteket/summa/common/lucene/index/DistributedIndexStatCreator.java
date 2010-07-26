/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.summa.common.lucene.index;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.IndexOutput;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

/**
 * This is used to create proper distributed stat indexes.
 * Use this to prepare indexes for distribution.
 *
 * Stats are written to the {@link org.apache.lucene.store.Directory} instance of the IndexReader added.
 * Implementors needs to take this into consideration as it might not be usable to add IndexReaders opened
 * other diretory impelmentations that {@link org.apache.lucene.store.FSDirectory}
 *
 * @author Hans Lund, State and University Library, Aarhus Denmark
 * @version $Id: DistributedIndexStatCreator.java,v 1.3 2007/10/04 13:28:19 te Exp $
 * @deprecated use {@link dk.statsbiblioteket.summa.common.lucene.distribution.TermStatExtractor} instead.
 */
@Deprecated
public class DistributedIndexStatCreator {

    private ArrayList<IndexReader> indexex;
    private Map<Term, Integer> buffer;
    private int numdocs;
    private static final String EOF = "eof";
    public static final String DISTRIBUTED_STATS_FILE = "diststat.sts";

    /**
     * Create a new Instance of this creator.
     */
    public DistributedIndexStatCreator(){
        this.buffer = new HashMap<Term,Integer>();
        this.indexex = new ArrayList<IndexReader>();
        this.numdocs = 0;
    }

    /**
     * Add an IndexReader with an open connection to a index that should be part of the distributed index.
     *
     * @param r
     * @throws IOException
     */
    public void add(IndexReader r) throws IOException {
        boolean needsAdd;
        synchronized(this){
           needsAdd = !indexex.contains(r);
        }
        if (needsAdd){
           indexex.add(r);
           numdocs += r.numDocs();
           TermEnum te = r.terms();
           while (te.next()){
            Term t = te.term();

            int frq = buffer.get(t) != null ? te.docFreq() + buffer.get(t) : te.docFreq();
            buffer.put(t,frq);
           }
        }
    }

    /**
     * This will create the diststat.sts index in the directories of the added IndexReaders.
     *
     * @throws IOException
     */
    public synchronized void writeStats() throws IOException {
        for (IndexReader r : indexex){
            IndexOutput o = r.directory().createOutput(DISTRIBUTED_STATS_FILE);
            o.writeVInt(numdocs);
            TermEnum te = r.terms();
            while(te.next()){
                Term t = te.term();
                o.writeString(t.field()); o.writeString(t.text()); o.writeVInt(buffer.get(t));
            }
            o.writeString(EOF);o.writeString(EOF);o.writeVInt(0);
        }
    }

}




