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
package dk.statsbiblioteket.summa.common.lucene;

import java.text.ParseException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.document.Field;
import org.w3c.dom.Node;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.index.FieldProvider;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaFieldSeparatingAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaStandardAnalyzer;

/**
 * Extension of IndexField to support Lucene-specific behaviour.
 * TODO: Migrate the building of Analyzer from IndexServiceImpl.
 */
public class LuceneIndexField extends
                              IndexField<Analyzer, Tokenizer, IndexFilter> {
    private Field.TermVector termVector =
            Field.TermVector.WITH_POSITIONS_OFFSETS;

    public LuceneIndexField() {
        super();
    }

    public LuceneIndexField(String name) {
        super(name);
    }

    public LuceneIndexField(Node node, FieldProvider indexFieldFieldProvider)
                                                         throws ParseException {
        super(node, indexFieldFieldProvider);
    }

    public LuceneIndexField(IndexField<Analyzer, Tokenizer, IndexFilter>
            parent) {
        super(parent);
    }

    protected Analyzer getDefaultIndexAnalyzer() {
        return new SummaStandardAnalyzer();
    }

    protected Analyzer getDefaultQueryAnalyzer() {
        return new SummaStandardAnalyzer();
    }

    public Analyzer getIndexAnalyzer() {
        if (isMultiValued()) {
            // TODO: Consider sanity-check for non-text analyzers
            return new SummaFieldSeparatingAnalyzer(super.getIndexAnalyzer());
        }
        return super.getIndexAnalyzer();
    }

    public Analyzer getQueryAnalyzer() {
        if (isMultiValued()) { // TODO: Does multi-value make sense here?
            // TODO: Consider sanity-check for non-text analyzers
            return new SummaFieldSeparatingAnalyzer(super.getQueryAnalyzer());
        }
        return super.getQueryAnalyzer();
    }

    protected void assignFrom(LuceneIndexField parent) {
        super.assignFrom(parent);
        setTermVector(parent.getTermVector());
    }

    // TODO: Implement clone
    // TODO: Make getAnalyzer wrap in case of multi value

    /* Mutators */

    public Field.TermVector getTermVector() {
        if (!isDoIndex()) {
            // It is illegal to attempt to create term vectors for non-indexed
            return Field.TermVector.NO;
        } else {
            return termVector;
        }
    }

    public void setTermVector(Field.TermVector termVector) {
        this.termVector = termVector;
    }

    /**
     * Convenience method for translating doStore and doCompress to Lucene
     * Store.
     * @return how to store a term based on this field.
     */
    public Field.Store getStore() {
        if (isDoStore()) {
            if (isDoCompress()) {
                return Field.Store.COMPRESS;
            } else {
                return Field.Store.YES;
            }
        } else {
            return Field.Store.NO;
        }
    }

    /**
     * Convenience method for translating index and tokenize to Lucene Index.
     * @return how to index a term based on this field.
     */
    public Field.Index getIndex() {
        if (isDoIndex()) {
            if (isAnalyze()) {
                return Field.Index.TOKENIZED;
            } else {
                return Field.Index.UN_TOKENIZED;
            }
        } else {
            return Field.Index.NO;
        }
    }

    // TODO: Implement methods below to get custom A/T/I in the descriptor

    protected Analyzer createAnalyzer(Node node) {
        return super.createAnalyzer(node);
    }

    protected Tokenizer createTokenizer(Node node) {
        return super.createTokenizer(node);
    }

    protected IndexFilter createFilter(Node node) {
        return super.createFilter(node);
    }

    protected String analyzerToXMLFragment(Analyzer analyzer) {
        return super.analyzerToXMLFragment(analyzer);
    }

    protected String tokenizerToXMLFragment(Tokenizer tokenizer) {
        return super.tokenizerToXMLFragment(tokenizer);
    }

    protected String filterToXMLFragment(IndexFilter filter) {
        return super.filterToXMLFragment(filter);
    }
}




