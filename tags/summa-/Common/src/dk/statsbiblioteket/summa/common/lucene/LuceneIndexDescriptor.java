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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.lucene.analysis.*;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.util.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trivial implementation of a Lucene IndexDescriptor.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "tke",
        comment = "Needs JavaDoc")
public class LuceneIndexDescriptor
        extends IndexDescriptor<LuceneIndexField> {
    private static Log log = LogFactory.getLog(LuceneIndexDescriptor.class);

    private Analyzer indexAnalyzer;
    private Analyzer queryAnalyzer;

    private List<String> moreLikethisFields;

    public LuceneIndexDescriptor(Configuration configuration)
                                                            throws IOException {
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

    @Override
    protected LuceneIndexField createBaseField(String baseFieldName) {
        log.debug(String.format(
                "createBaseField(%s) for LuceneIndexDescriptor called",
                baseFieldName));
        if (baseFieldName.equals(IndexField.SUMMA_DEFAULT)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.YES,
                             Field.TermVector.WITH_POSITIONS_OFFSETS,
                             new SummaStandardAnalyzer());
        }
        if (baseFieldName.equals(IndexUtils.RECORD_FIELD)) {
            return makeField(baseFieldName,
                             Field.Index.NOT_ANALYZED,
                             Field.Store.YES,
                             Field.TermVector.NO,
                             new KeywordAnalyzer());
        }
        if (baseFieldName.equals(IndexUtils.RECORD_BASE)) {
            return makeField(baseFieldName,
                             Field.Index.NOT_ANALYZED,
                             Field.Store.YES,
                             Field.TermVector.NO,
                             new KeywordAnalyzer());
        }
        if (baseFieldName.equals(IndexField.FREETEXT)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.NO,
                             Field.TermVector.WITH_POSITIONS_OFFSETS, // ?
                             new FreeTextAnalyzer());
        }

        if (baseFieldName.equals(KEYWORD)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.NO,
                             Field.TermVector.NO,
                             new SummaKeywordAnalyzer());
        }
        if (baseFieldName.equals(STORED_KEYWORD)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.YES,
                             Field.TermVector.NO,
                             new SummaKeywordAnalyzer());
        }

        if (baseFieldName.equals(VERBATIM)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.NO,
                             Field.TermVector.NO,
                             new KeywordAnalyzer());
        }
        if (baseFieldName.equals(LOWERCASE)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.NO,
                             Field.TermVector.NO,
                             new SummaLowercaseAnalyzer());
        }
        if (baseFieldName.equals(STORED_VERBATIM)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.YES,
                             Field.TermVector.NO,
                             new KeywordAnalyzer());
        }

        if (baseFieldName.equals(TEXT)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.NO,
                             Field.TermVector.WITH_POSITIONS_OFFSETS,
                             new SummaStandardAnalyzer());
        }
        if (baseFieldName.equals(SORTKEY)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.NO,
                             Field.TermVector.YES,
                             new SummaSymbolRemovingAnalyzer());
        }
        if (baseFieldName.equals(STORED)) {
            return makeField(baseFieldName,
                             Field.Index.NO,
                             Field.Store.YES,
                             Field.TermVector.NO,
                             new SummaStandardAnalyzer());
        }
        if (baseFieldName.equals(DATE)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.NO,
                             Field.TermVector.NO,
                             new SimpleAnalyzer(Version.LUCENE_30));
        }
        if (baseFieldName.equals(NUMBER)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.YES,
                             Field.TermVector.NO,
                             new SummaNumberAnalyzer());
        }

        throw new IllegalArgumentException(String.format(
                "The base field '%s' is unknown by the LuceneIndexDescriptor",
                baseFieldName));
    }

    final static String MLT_EXPR = "IndexDescriptor/moreLikethisFields/field";

    @Override
    public Document parse(String xml) throws ParseException {
        Document document = super.parse(xml);
        if (document == null) {
            log.warn("parse(" + xml + ") error: Document was null");
        }

        log.trace("Extracting nodes with expression '" + MLT_EXPR + "'");
        NodeList mltNodes = DOM.selectNodeList(document, MLT_EXPR);

        moreLikethisFields = new ArrayList<String>(mltNodes.getLength());
        if (mltNodes.getLength() == 0) {
            log.debug("No MoreLikeThis nodes in index descriptor");
            return document;
        }
        for (int i = 0 ; i < mltNodes.getLength() ; i++) {
            String ref = DOM.selectString(mltNodes.item(i), "@ref", null);
            if (ref == null || "".equals(ref)) {
                log.warn("Got field without reference in moreLikeThisFields");
            } else {
                moreLikethisFields.add(ref);
            }
        }
        log.info("Finished extracting MoreLikeThis fields: "
                 + Logs.expand(moreLikethisFields, 20));

        return document;
    }

    private LuceneIndexField makeField(String name, Field.Index index,
                                       Field.Store store,
                                       Field.TermVector termVector,
                                       Analyzer analyzer) {
        LuceneIndexField field = new LuceneIndexField(name);
        if (index.equals(Field.Index.NO)) {
            field.setDoIndex(false);
            field.setAnalyze(false);
        } else if (index.equals(Field.Index.NOT_ANALYZED_NO_NORMS)) {
            throw new UnsupportedOperationException(
                    "Storing with no norms is not supported. Offending field "
                    + "has name '" + name + "'");
        } else if (index.equals(Field.Index.ANALYZED)) {
            field.setDoIndex(true);
            field.setAnalyze(true);
        } else if (index.equals(Field.Index.NOT_ANALYZED)) {
            field.setDoIndex(true);
            field.setAnalyze(false);
        }
        /*if (store.equals(Field.Store.COMPRESS)) {
                field.setDoStore(true);
                field.setDoCompress(true);
              } else*/
        if (store.equals(Field.Store.NO)) {
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
     * @see SummaQueryParser
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
            if (entry.getValue().getIndexAnalyzer() != null) {
                indexWrapper.addAnalyzer(entry.getKey(),
                                         entry.getValue().getIndexAnalyzer());
            }
            if (entry.getValue().getQueryAnalyzer() != null) {
                queryWrapper.addAnalyzer(entry.getKey(),
                                         entry.getValue().getQueryAnalyzer());
            }
        }
        indexAnalyzer = indexWrapper;
        queryAnalyzer = queryWrapper;
    }

    /**
     * @return the fields that should be used for MoreLikeThis functionality.
     */
    public List<String> getMoreLikethisFields() {
        return moreLikethisFields;
    }
}




