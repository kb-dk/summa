/* $Id: FieldType.java,v 1.2 2007/10/04 13:28:19 te Exp $
 * $Revision: 1.2 $
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

import dk.statsbiblioteket.summa.common.lucene.analysis.*;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;


/**
 * The FieldType enum mappes a set of types to IndexField charateristics.
 * (tokenized, store, termvector and Analyzer)
 * FieldTypes are used within the SummaIndex.xsd index definition xml.
 *
 *@author Hans Lund, State and University Library, Denmark
 *@version $Id: FieldType.java,v 1.2 2007/10/04 13:28:19 te Exp $
 * // TODO: Change deprecation to point to Lucene-specific code when available
 *@deprecated see default-fields from {@link LuceneIndexDescriptor} instead.
 */
public enum FieldType {

    /**
     * a storedKeyword is UN_TOKENIZED, STORED, NO TERMVECTORS, uses the SummaKeywordAnalyzer
     * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaKeywordAnalyzer
     */
    storedKeyword("storedKeyWord", Field.Index.UN_TOKENIZED, Field.Store.YES, Field.TermVector.NO, new SummaKeywordAnalyzer()),
    /**
     * a keyword is UN_TOKENIZED, NOT STORED, NO TERMVECTORS, uses the SummaKeywordAnalyzer
     * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaKeywordAnalyzer
     */
    keyWord("keyword", Field.Index.TOKENIZED,Field.Store.NO, Field.TermVector.YES, new SummaKeywordAnalyzer()),
    /**
     * a text field is, TOKENIZED, NOT STORED, TERMVECTOR WITH_POSITION_OFFSETS, uses SummaStandardAnalyzer
     * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer
     */
    text("text", Field.Index.TOKENIZED, Field.Store.NO, Field.TermVector.WITH_POSITIONS_OFFSETS, new SummaStandardAnalyzer()),
    /**
     * sort is, TOKENIZED, NOT STORED, TERMVECTOR YES, uses SummaSortKeyAnalyzer
     * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaSortKeyAnalyzer
     */
    sort("sortkey", Field.Index.TOKENIZED, Field.Store.NO, Field.TermVector.YES, new SummaSortKeyAnalyzer()),
    /**
     * stored is: INDEX NO, STORED COMPRESS, TERMVECTOR NO, uses SummaStandardAnalyzer.<br>
     * note: the analyzer is not acctually used as the type is not indexed!!!
     */
    stored("stored", Field.Index.NO, Field.Store.COMPRESS, Field.TermVector.NO, new SummaStandardAnalyzer()),
    /**
     * date is : TOKENIZED, STOREED NO, TERMVECTOR NO, uses SimpleAnalyzer.
     * @see org.apache.lucene.analysis.SimpleAnalyzer
     */
    date("date", Field.Index.TOKENIZED, Field.Store.NO, Field.TermVector.NO, new SimpleAnalyzer()),
    /**
     * number is: TOKENIZED, STORED YES, TERMVECTOR NO, uses SummaNumberAnalyzer.
     * @see dk.statsbiblioteket.summa.common.lucene.analysis.SummaNumberAnalyzer
     */
    number("number", Field.Index.TOKENIZED, Field.Store.YES, Field.TermVector.NO, new SummaNumberAnalyzer()),
    /**
     * text is: TOKENIZED, STORED NO, TERMVECTOR WITH_POSITION_OFFSET, new FreeTextAnalyzer.
     * @see dk.statsbiblioteket.summa.common.lucene.analysis.FreeTextAnalyzer
     */
    freetext("text", Field.Index.TOKENIZED, Field.Store.NO, Field.TermVector.WITH_POSITIONS_OFFSETS, new FreeTextAnalyzer());


    private Analyzer analyzer;
    private Field.Index index;
    private Field.Store store;
    private Field.TermVector vector;
    private String type;


    FieldType(String type, Field.Index index, Field.Store store, Field.TermVector vector, Analyzer analyzer){
          this.type = type;
          this.index = index;
          this.store = store;
          this.vector = vector;
          this.analyzer = analyzer;
    }

    /**
     * The Field.Index contains information about the indexing status of the field.
     *
     * @see org.apache.lucene.document.Field.Index
     * @return  the index information for this Type.
     */
    public Field.Index getIndex() {
        return index;
    }

    /**
     * The Field.Store contains information about the storing status of the field.
     *
     * @see org.apache.lucene.document.Field.Store
     * @return  the store status for this Type
     */
    public Field.Store getStore() {
        return store;
    }

    /**
     * The Field.TermVector contains information about the termvevtor status of the field.
     *
     * @see org.apache.lucene.document.Field.TermVector
     * @return the types TermVector definition
     */
    public Field.TermVector getVector() {
        return vector;
    }

    /**
     * The Analyzer transform input text through a series of filters depending on the type of analyzer.
     *
     * @see org.apache.lucene.analysis.Analyzer
     * @return - the analyzer used for fields of this type.
     */
    public Analyzer getAnalyzer(){ return analyzer; }

    /**
     * A FieldType is a named set of characteristics.
     * Here you can get the name.
     *
     * @return the name of this type.
     */
    public String getType(){ return type; }

    /**
     * The toString method will return the name of the type
     * @return a string representation of this.
     */
    public String toString(){ return type; }

    /**
     * This method will return the FieldType with a given name.
     *
     * @param type  the name of the type
     * @return the FieldType
     */
    public static FieldType getType(String type){
        if ("keyword".equals(type)) {
                return FieldType.keyWord;
            } else if ("text".equals(type)) {
                return FieldType.text;
            } else if ("sortkey".equals(type)) {
                return FieldType.sort;
            } else if ("stored".equals(type)) {
                return FieldType.stored;
            } else if ("date".equals(type)) {
                return FieldType.date;
            } else if ("number".equals(type)){
                return FieldType.number;
            } else if ("freetext".equals(type)){
                return FieldType.freetext;
            } else if ("sort".equals(type)) {
                // for backwards compatiblility only
                return FieldType.sort;
            }
        return null;
    }

}
