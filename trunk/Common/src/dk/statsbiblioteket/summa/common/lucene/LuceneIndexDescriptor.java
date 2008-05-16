/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.statsbiblioteket.summa.common.lucene;

import java.text.ParseException;

import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaKeywordAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaSortKeyAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaNumberAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.analysis.FreeTextAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;

/**
 * Trivial implementation of a Lucene IndexDescriptor.
 */
public class LuceneIndexDescriptor
        extends IndexDescriptor<LuceneIndexField> {
    private static Log log = LogFactory.getLog(LuceneIndexDescriptor.class);

    public LuceneIndexField createNewField() {
        return new LuceneIndexField();
    }

    public LuceneIndexField createNewField(Node node) throws ParseException {
        // TODO: Implement this
        return null; //new LuceneIndexField(node, this);
    }


    /**
     * Creates default fields, ready to be inherited.
     */
    public void init() {
//        makeField("storedKeyWord", Field.Index.UN_TOKENIZED, Field.Store.YES, Field.TermVector.NO, new SummaKeywordAnalyzer()),
        /**
         * a keyword is UN_TOKENIZED, NOT STORED, NO TERMVECTORS, uses the SummaKeywordAnalyzer
         * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaKeywordAnalyzer
         */
  //      keyWord("keyword", Field.Index.TOKENIZED,Field.Store.NO, Field.TermVector.YES, new SummaKeywordAnalyzer()),
        /**
         * a text field is, TOKENIZED, NOT STORED, TERMVECTOR WITH_POSITION_OFFSETS, uses SummaStandardAnalyzer
         * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer
         */
    //    text("text", Field.Index.TOKENIZED, Field.Store.NO, Field.TermVector.WITH_POSITIONS_OFFSETS, new SummaStandardAnalyzer()),
        /**
         * sort is, TOKENIZED, NOT STORED, TERMVECTOR YES, uses SummaSortKeyAnalyzer
         * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaSortKeyAnalyzer
         */
      //  sort("sortkey", Field.Index.TOKENIZED, Field.Store.NO, Field.TermVector.YES, new SummaSortKeyAnalyzer()),
        /**
         * stored is: INDEX NO, STORED COMPRESS, TERMVECTOR NO, uses SummaStandardAnalyzer.<br>
         * note: the analyzer is not acctually used as the type is not indexed!!!
         */
//        stored("stored", Field.Index.NO, Field.Store.COMPRESS, Field.TermVector.NO, new SummaStandardAnalyzer()),
        /**
         * date is : TOKENIZED, STOREED NO, TERMVECTOR NO, uses SimpleAnalyzer.
         * @see org.apache.lucene.analysis.SimpleAnalyzer
         */
  //      date("date", Field.Index.TOKENIZED, Field.Store.NO, Field.TermVector.NO, new SimpleAnalyzer()),
        /**
         * number is: TOKENIZED, STORED YES, TERMVECTOR NO, uses SummaNumberAnalyzer.
         * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaNumberAnalyzer
         */
    //    number("number", Field.Index.TOKENIZED, Field.Store.YES, Field.TermVector.NO, new SummaNumberAnalyzer()),
        /**
         * text is: TOKENIZED, STORED NO, TERMVECTOR WITH_POSITION_OFFSET, new FreeTextAnalyzer.
         * @see dk.statsbiblioteket.summa.common.lucene.analysis.FreeTextAnalyzer
         */
      //  freetext("freetext", Field.Index.TOKENIZED, Field.Store.NO, Field.TermVector.WITH_POSITIONS_OFFSETS, new FreeTextAnalyzer());

    }

    private IndexField<Analyzer, Tokenizer, IndexFilter> makeField(
            String name, Field.Index index, Field.Store store,
            Field.TermVector termVector, Analyzer analyzer) {
        IndexField<Analyzer, Tokenizer, IndexFilter> field =
                new IndexField<Analyzer, Tokenizer, IndexFilter>(name);
        if (index.equals(Field.Index.NO)) {
            field.setDoIndex(false);
            field.setTokenize(false);
        } else if (index.equals(Field.Index.NO_NORMS)) {
            throw new UnsupportedOperationException(
                    "Storing with no norms is not supported. Offending field "
                    + "has name '" + name + "'");
        } else if (index.equals(Field.Index.TOKENIZED)) {
            field.setDoIndex(true);
            field.setTokenize(true);
        } else if (index.equals(Field.Index.UN_TOKENIZED)) {
            field.setDoIndex(true);
            field.setTokenize(false);
        }
        if (store.equals(Field.Store.COMPRESS)) {
            field.setDoStore(true);
            field.setDoCompress(true);
        } else if (store.equals(Field.Store.NO)) {
            field.setDoStore(false);
            field.setDoCompress(false);
        } else if (store.equals(Field.Store.YES)) {
            field.setDoStore(true);
            field.setDoCompress(false);
        }
        if (termVector.equals(Field.TermVector.NO)) {
//            field
        }
        return field;
    }
    // TODO: Create default fields (including freetext)
}
