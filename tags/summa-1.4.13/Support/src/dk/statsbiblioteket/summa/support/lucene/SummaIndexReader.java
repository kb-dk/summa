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
package dk.statsbiblioteket.summa.support.lucene;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * A wrapped Lucene IndexReader that connects to a pool containing term
 * statistics extracted from a distributed environment. The reader does not
 * have a hard dependency on the stats and will fall back to the stats from
 * the current index if they are not available.
 * </p><p>
 * Using this reader ensures that ranking is coordinated between shards of the
 * whole conceptual index, making it possible for distribution to be
 * functionally equivalent with a single big index.
 * </p><p>
 * Note that it is acceptable for the TermProvider to return the docCount for
 * terms with only one instance as non-existing. This will result in a request
 * to the underlying IndexReader
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te, hal")
public class SummaIndexReader extends FilterIndexReader {
    private final Log log = LogFactory.getLog(SummaIndexReader.class);

    // Don't close the provider on reader close
    private TermProvider termProvider;

    public SummaIndexReader(IndexReader indexReader,
                            TermProvider termProvider) {
        super(indexReader);
        this.termProvider = termProvider;
    }

    @Override
    public int numDocs(){
        int num = termProvider.numDocs();
        return num == -1 ? super.numDocs() : num;
    }

    /**
     * Note: The explain and the JavaDocs for Lucene states that numDocs is
     *       used for valculating idf. In reality, maxDoc is called.
     * @return numDocs + 1 from termProvider or maxDoc is no term stats are
     *         present.
     */
    @Override
    public int maxDoc() {
        int num = termProvider.numDocs() + 1;
        return num == 0 ? super.maxDoc() : num;
    }

    /**
     * Resolve the number of documents containing the term across the
     * distributed index. If the underlying term-provider is unable to provide
     * a document count, the wrapped Indexreader  is used for resolving.
     * @param t the term to get the document count for.
     * @return the document count for the term.
     * @throws IOException if an error happened during resolving.
     */
    @Override
    public int docFreq(Term t) throws IOException {
        int freq;
        try {
            freq = termProvider.docFreq(t.field() + ":" + t.text());
        } catch (Exception e) {
            try {
                log.warn(String.format(
                        "Unexpected exception getting docFreq for term '%s:%s'",
                        t.field(), t.text()), e);
            } catch (Exception e2) {
                log.warn("Exception while extracting field and text from term "
                         + "to create error message. The term was '" + t + "'",
                         e2);
            }
            return atLeastOne(super.docFreq(t));
        }
        return freq == -1 ? atLeastOne(super.docFreq(t)) : freq;
    }

    private int atLeastOne(int value) {
        return value <= 0 ? 1 : value;
    }
}
