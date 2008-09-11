/* $Id: SummaIndexReader.java,v 1.3 2007/10/04 13:28:19 te Exp $
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
package dk.statsbiblioteket.summa.common.lucene.index;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.IndexInput;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The SummaIndexReader is usable for making distributed lucene indexes.<br>
 * A 'total' but partitionated index is mapped through the
 * {@link dk.statsbiblioteket.summa.common.lucene.index.DistributedIndexStatCreator}<br>
 * The index partition can now be opened through this reader, that now guaranties that
 *  documents in the partition will score as if the resided in a merged index.<br><br>
 * 
 * Be aware, This IndexReader will consume extra memory for cashing (around 1/10 of the index size)
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class SummaIndexReader extends FilterIndexReader {
    private final Log log = LogFactory.getLog(FilterIndexReader.class);

    private int numdoc;
    private Map<Term, Integer> buffer = null;
    private boolean distributedStatsFilePresent = false;


    /**
     * Creates an indexReader that will find its scoring stats in the
     * diststat.sts index file of in underlying indexReader.
     * </p><p>
     * An {@link IllegalArgumentException} will be thrown if no distributed
     * stats can be found. This indicates that the index has not been properly
     * prepared.
     * @param indexReader the reader to wrap.
     */
    public SummaIndexReader(IndexReader indexReader) {
        super(indexReader);
        buffer = new HashMap<Term,Integer>(10000);
        try{
            IndexInput inp = indexReader.directory().openInput(
                    DistributedIndexStatCreator.DISTRIBUTED_STATS_FILE);
            numdoc = inp.readVInt();
            while (true) {
                Term t = new Term(inp.readString(), inp.readString());
                int co = inp.readVInt();
                if (co == 0) {
                    break;
                }
                buffer.put(t, co);
            }
            distributedStatsFilePresent = true;
        } catch (IOException e) {
            log.warn("IndexReader could not load distributed stats from '"
                      + indexReader.directory() + "'", e);
        }
    }

    public int numdoc(){
        // TODO: Shouldn't we always return super here?
        return distributedStatsFilePresent ? numdoc : super.numDocs();
    }

    public int docFreq(Term t) throws IOException {
        if (!distributedStatsFilePresent) {
            return super.docFreq(t);
        }
        try{
            return buffer.get(t);
        } catch (Exception e){
            return super.docFreq(t);
        }
    }


}



