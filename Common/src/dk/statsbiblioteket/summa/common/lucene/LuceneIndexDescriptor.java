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
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaSymbolRemovingAnalyzer;
import dk.statsbiblioteket.summa.common.lucene.analysis.*;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
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
    private Analyzer indexAnalyzer;
    private Analyzer queryAnalyzer;

    public LuceneIndexDescriptor(Configuration configuration) throws IOException {
        super(configuration);
    }

    public LuceneIndexDescriptor() {
        super();
    }

    public LuceneIndexDescriptor(URL absoluteLocation) throws IOException {
        super(absoluteLocation);
    }

    public LuceneIndexDescriptor(String xml) throws ParseException {
        super(xml);
    }

    public LuceneIndexField createNewField() {
        log.trace("createNewField called");
        return new LuceneIndexField();
    }

    public LuceneIndexField createNewField(Node node) throws ParseException {
        return new LuceneIndexField(node, this);
    }

    /**
     * Creates default fields, ready to be inherited.
     */
    public void init() {
        log.trace("init() called");
        addField(makeField(IndexField.FREETEXT,
                           Field.Index.TOKENIZED,
                           Field.Store.NO,
                           Field.TermVector.WITH_POSITIONS_OFFSETS, // ?
                           new FreeTextAnalyzer()));
        addField(makeField(IndexField.SUMMA_DEFAULT,
                           Field.Index.TOKENIZED,
                           Field.Store.YES,
                           Field.TermVector.WITH_POSITIONS_OFFSETS,
                           new SummaStandardAnalyzer()));
        addField(makeField("storedKeyWord",
                           Field.Index.UN_TOKENIZED,
                           Field.Store.YES,
                           Field.TermVector.NO,
                           new SummaKeywordAnalyzer()));
        addField(makeField("keyword",
                           Field.Index.TOKENIZED,
                           Field.Store.NO,
                           Field.TermVector.YES,
                           new SummaKeywordAnalyzer()));
        addField(makeField("text",
                           Field.Index.TOKENIZED,
                           Field.Store.NO,
                           Field.TermVector.WITH_POSITIONS_OFFSETS,
                           new SummaStandardAnalyzer()));
        addField(makeField("sortkey",
                           Field.Index.TOKENIZED,
                           Field.Store.NO,
                           Field.TermVector.YES,
                           new SummaSymbolRemovingAnalyzer()));
        addField(makeField("stored",
                           Field.Index.NO,
                           Field.Store.COMPRESS,
                           Field.TermVector.NO,
                           new SummaStandardAnalyzer()));
        addField(makeField("date",
                           Field.Index.TOKENIZED,
                           Field.Store.NO,
                           Field.TermVector.NO,
                           new SimpleAnalyzer()));
        addField(makeField("number",
                           Field.Index.TOKENIZED,
                           Field.Store.YES,
                           Field.TermVector.NO,
                           new SummaNumberAnalyzer()));
    }

    public void parse(String xml) throws ParseException {
        super.parse(xml);
        createAnalyzers();
    }

    private LuceneIndexField makeField(String name, Field.Index index,
                                       Field.Store store,
                                       Field.TermVector termVector,
                                       Analyzer analyzer) {
        LuceneIndexField field = new LuceneIndexField(name);
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
        field.setTermVector(termVector);
        field.setIndexAnalyzer(analyzer);
        field.setQueryAnalyzer(analyzer);
        return field;
    }

    /**
     * Returns analyzer used for indexing. The analyzer represents the
     * analyzers for the separate fields, which means that it will normally
     * be a {@link PerFieldAnalyzerWrapper}.
     * @return an analyzer for indexing (use with
     * <code>writer.add(someDocument, theAnalyzer)</code>).
     */
    public Analyzer getIndexAnalyzer() {
        if (indexAnalyzer == null) {
            createAnalyzers();
        }
        return indexAnalyzer;
    }

    /**
     * Returns an analyzer for querying. The analyzer represents the
     * analyzers for the separate fields, which means that it will normally
     * be a {@link PerFieldAnalyzerWrapper}.
     * @return an analyzer for building a Lucene Query tree.
     * @see {@link SummaQueryParser}.
     */
    public Analyzer getQueryAnalyzer() {
        if (queryAnalyzer == null) {
            createAnalyzers();
        }
        return queryAnalyzer;
    }

    private void createAnalyzers() {
        log.debug("createAnalyzers called");
        PerFieldAnalyzerWrapper indexWrapper =
                new PerFieldAnalyzerWrapper(defaultField.getIndexAnalyzer());
        PerFieldAnalyzerWrapper queryWrapper =
                new PerFieldAnalyzerWrapper(defaultField.getQueryAnalyzer());
        for (Map.Entry<String, LuceneIndexField> entry:
                getFields().entrySet()) {
            log.debug("Adding field " + entry.getKey() + " index-analyzer "
                      + entry.getValue().getIndexAnalyzer()
                      + " and query-analyzer "
                      + entry.getValue().getQueryAnalyzer());
            queryWrapper.addAnalyzer(entry.getKey(),
                                     entry.getValue().getQueryAnalyzer());
        }
        indexAnalyzer = indexWrapper;
        queryAnalyzer = queryWrapper;
    }
}



