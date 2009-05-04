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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import dk.statsbiblioteket.summa.common.index.IndexField;
import dk.statsbiblioteket.summa.common.lucene.analysis.*;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.lucene.search.SummaQueryParser;
import dk.statsbiblioteket.summa.common.util.ParseUtil;
import dk.statsbiblioteket.summa.common.xml.DefaultNamespaceContext;
import dk.statsbiblioteket.util.Logs;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Field;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trivial implementation of a Lucene IndexDescriptor.
 */
public class LuceneIndexDescriptor
        extends IndexDescriptor<LuceneIndexField> {
    private static Log log = LogFactory.getLog(LuceneIndexDescriptor.class);

    public static final String LUCENE_DESCRIPTOR_NAMESPACE =
            "http://statsbiblioteket.dk/summa/2009/LuceneIndexDescriptor";
    public static final String LUCENE_DESCRIPTOR_NAMESPACE_PREFIX = "lu";

    private Analyzer indexAnalyzer;
    private Analyzer queryAnalyzer;

    private List<String> moreLikethisFields;

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
                             Field.Store.COMPRESS,
                             Field.TermVector.NO,
                             new SummaStandardAnalyzer());
        }
        if (baseFieldName.equals(DATE)) {
            return makeField(baseFieldName,
                             Field.Index.ANALYZED,
                             Field.Store.NO,
                             Field.TermVector.NO,
                             new SimpleAnalyzer());
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

    final static String MLT_EXPR = String.format(
            "%s:IndexDescriptor/%s:moreLikethisFields/%1$s:field",
            DESCRIPTOR_NAMESPACE_PREFIX, LUCENE_DESCRIPTOR_NAMESPACE_PREFIX);
    @Override
    public Document parse(String xml) throws ParseException {
        XPath xPath = createXPath();
        Document document = super.parse(xml);
        if (document == null) {
            log.warn("parse(" + xml + ") error: Document was null");
        }

        log.trace("Extracting nodes with expression '" + MLT_EXPR + "'");
        NodeList mltNodes;
        try {
            mltNodes = (NodeList)xPath.evaluate(MLT_EXPR, document,
                                                XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new ParseException(String.format(
                    "Expression '%s' for selecting MoreLikeThis was invalid",
                    MLT_EXPR), -1);
        }
        moreLikethisFields = new ArrayList<String>(mltNodes.getLength());
        if (mltNodes.getLength() == 0) {
            log.debug("No MoreLikeThis nodes in index descriptor");
            return document;
        }
        for (int i = 0 ; i < mltNodes.getLength() ; i++) {
            String ref = ParseUtil.getValue(
                    xPath, mltNodes.item(i), "@ref", (String)null);
            if (ref == null || "".equals(ref)) {
                log.warn("Got field without reference in moreLikeThisFields");
            } else {
                moreLikethisFields.add(ref);
            }
        }
        log.info("Finished extracting MoreLikeThis fields: "
                 + Logs.expand(moreLikethisFields, 20));

        return document;
        //createAnalyzers();
    }

    private XPath createXPath() {
        DefaultNamespaceContext nsCon = new DefaultNamespaceContext();
        nsCon.setNameSpace(DESCRIPTOR_NAMESPACE,
                           DESCRIPTOR_NAMESPACE_PREFIX);
        nsCon.setNameSpace(LUCENE_DESCRIPTOR_NAMESPACE,
                           LUCENE_DESCRIPTOR_NAMESPACE_PREFIX);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(nsCon);
        log.trace("XPath created");
        return xPath;
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



