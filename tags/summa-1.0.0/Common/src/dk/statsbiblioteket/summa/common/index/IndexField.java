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
package dk.statsbiblioteket.summa.common.index;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.List;
import java.util.ArrayList;
import java.io.StringWriter;
import java.text.ParseException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.util.ParseUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * IndexFields are used on several levels. From abstract to concrete, their
 * roles are as follows:
 * </p><p>
 * - Type role: The field defines how to handle content.<br />
 * - Named field role: The field has a concrete name.<br />
 * - Instantiated field: The field has content and is ready for indexing.
 * </p><p>
 * The class is generic. In order to use it properly, the A (Analyzer) and
 * F (Filter) must be specified. It is also strongly recommended to override
 * the methods
 * {@link #analyzerToXMLFragment(Object)},
 * {@link #createAnalyzer(Node)},
 * {@link #tokenizerToXMLFragment(Object)},
 * {@link #createTokenizer(Node)},
 * {@link #filterToXMLFragment(Object)} and
 * {@link #createFilter(Node)},
 * to conform to the standards of the chosen
 * index-format.
 * </p><p>
 * The IndexField is inspired heavily by Lucene Fields and partly by the SOLR
 * schema.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexField<A, T, F> {
    private static Log log = LogFactory.getLog(IndexField.class);

    /**
     * The name of the field is used verbatim as the field in generated indexes.
     * </p><p>
     * This is used at index- and query-time.
     */
    private String name = "Summa-class-default";

    /**
     * The name of the default-field - this is used if no parent is specified.
     */
    public static final String SUMMA_DEFAULT = "summa_default";

    /**
     * The name of the Field containing unordered freely searchable terms.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String FREETEXT = "freetext";

    /**
     * If true, the field should be indexed.
     * </p><p>
     * This is used at index-time.
     */
    private boolean doIndex = true;

    /**
     * If true, the content of the field should be tokenized upon index.
     * This takes effect if doIndex is true.
     * </p><p>
     * This is used at index-time.
     */
    private boolean tokenize = true;

    /**
     * If true, the field should be stored in the index.
     * </p><p>
     * This is used at index-time.
     */
    private boolean doStore = true;

    /**
     * If true, the field should be compressed.
     * </p><p>
     * This is used at index-time.
     */
    private boolean doCompress = false;

    /**
     * The parent used for creating this index field. This is null if the field
     * was build from scratch.
     * </p><p>
     * This is used at index-time.
     */
    private IndexField parent = null;

    /**
     * If true, multiple value-addition calls to this field will be result in
     * more instances of the field in the index. If false, multiple additions
     * will append to the field.
     * </p><p>
     * This is used at index-time.
     */
    private boolean multiValued = true;

    /**
     * The boost used when querying. This boost will be applied to every part
     * of the query that uses the field. Boosts defined on all other levels than
     * instantiated field should be used at query-time, making them easy to
     * modify. When an instantiated field is created by building on another
     * field, queryBoost is not used.
     * </p><p>
     * This is used at query-time.
     * @see {@link #indexBoost}.
     */
    private float queryBoost = 1.0f;

    /**
     * The boost used when indexing. This boost will be applied to any
     * instantiated field. Boosts specified explicitely for a given instantiated
     * field, will be multiplied to this boost.
     * </p><p>
     * This is used at index-time.
     * @see {@link #queryBoost}.
     */
    private float indexBoost = 1.0f;

    /**
     * A ISO 639-1 code specifying which locale that should be used for sorting.
     * Note that sorting with locale is a lot heavier than sorting without.
     * </p><p>
     * This is used at search-time.
     * @see [http://www.loc.gov/standards/iso639-2/php/code_list.php]
     */
    private String sortLocale = null;

    /**
     * The content of this field should be duplicated in the freetext-field
     * upon indexing.
     * </p><p>
     * This is used at index-time.
     */
    private boolean inFreetext = true;

    /**
     * If true, this index-field must be present upon indexing.<br />
     * This only makes sense for non-instantiated index fields.
     * </p><p>
     * This is used at index-time.
     */
    private boolean required = false;

    /**
     * Aliases for this index-field. If alias-expansion is used at query-time,
     * aliases are resolved to field-names in the index.
     * </p><p>
     * This is used at query-time.
     */
    private List<IndexAlias> aliases = new ArrayList<IndexAlias>(10);

    /**
     * The analyzer to use for indexing. The type of the analyzer is defined by
     * the user of this class and will normally be a Lucene-analyzer. This will
     * normally be identical to the queryAnalyzer.
     * </p><p>
     * This is used at index-time.
     */
    private A indexAnalyzer = getDefaultIndexAnalyzer();

    /**
     * The analyzer to use for query expansion. The type of the analyzer is
     * defined by the user of this class and will normally be a Lucene-analyzer.
     * This analyzer will normally be identical to the queryAnalyzer.
     * </p><p>
     * This is used at query-time.
     */
    private A queryAnalyzer = getDefaultQueryAnalyzer();

    /**
     * The filters to use when indexing. The type of filter is defined by the
     * user of this class.
     * </p><p>
     * This is used at index-time.
     */
    private List<F> indexFilters = new ArrayList<F>(10);

    /**
     * The filters to use for query expansion. The type of filter is defined by
     * the user of this class.
     * </p><p>
     * This is used at query-time.
     */
    private List<F> queryFilters = new ArrayList<F>(10);

    /**
     * The tokenizer used for text indexing. The type of analyzer is defined
     * by the user of this class.
     * </p><p>
     * This is used at index-time.
     */
    private T indexTokenizer = getDefaultIndexTokenizer();

    /**
     * The tokenizer used for text indexing. The type of analyzer is defined
     * by the user of this class.
     * </p><p>
     * This is used at query-time.
     */
    private T queryTokenizer = getDefaultQueryTokenizer();

    /**
     * Create a field with default values.
     */
    public IndexField() {
        // Do nothing - the defaults take care of themselves
    }

    /**
     * Create a field with the given name and default attributes.
     * @param name the name of the field.
     */
    public IndexField(String name) {
        this.name = name;
    }

    /**
     * Create a field based on the given Document Node. The Node should conform
     * to the output from {@link #toXMLFragment()}. If a parent field is
     * specified in the node, the new IndexField will be based on that parent.
     * All attributes specified in the Node will override that of the parent.
     * </p><p>
     * If any filters are specified in the node, all filters from the parent
     * will be ignored.
     * If no filters are specified in the node, the filters from the parent
     * will be used.
     * @param node          a representation of a Field.
     * @param fieldProvider if a parent name is specified, the fieldProvider is
     *                      queried for the parent.
     * @throws ParseException if the node could not be parsed properly.
     */
    public IndexField(Node node, FieldProvider fieldProvider) throws
                                                                ParseException {
        log.trace("Creating field based on node " + node);
        parse(node, fieldProvider);
    }

    /**
     * Assigns all the values from parent to the newly created field, then
     * assigns the {@link #parent} attribute to the parent field. 
     * @param parent the field to use as template for this field.
     */
    public IndexField(IndexField<A, T, F> parent) {
        log.debug("Creating field based on " + parent);
        assignFrom(parent);
        this.parent = parent;
    }

    /**
     * Assigns the values from the given field to this field. Used for
     * construction and cloning. Assignment will override all values.
     * Lists are shallow copies, to it is safe to modify the lists themselves
     * after assignment.
     * @param parent the field to get values from.
     */
    protected void assignFrom(IndexField<A, T, F> parent) {
        log.trace("Assigning from " + parent);
        name = parent.getName();
        this.parent = parent.getParent();
        doIndex = parent.isDoIndex();
        tokenize = parent.isTokenize();
        doStore = parent.isDoStore();
        doCompress = parent.isDoCompress();
        multiValued = parent.isMultiValued();
        queryBoost = parent.getQueryBoost();
        indexBoost = parent.getIndexBoost();
        sortLocale = parent.getSortLocale();
        inFreetext = parent.isInFreetext();
        required = parent.isRequired();
        aliases = new ArrayList<IndexAlias>(parent.getAliases());
        indexAnalyzer = parent.getIndexAnalyzer();
        queryAnalyzer = parent.getQueryAnalyzer();
        indexFilters = new ArrayList<F>(parent.getIndexFilters());
        queryFilters = new ArrayList<F>(parent.getQueryFilters());
    }

    public String toString() {
        return "Field(" + name + ")";
    }

    /**
     * Construct an XML fragment describing this field, suitable for insertion
     * in IndexDescriptor XML or similar persistent structure.
     * @return an XML for this field.
     */
    public String toXMLFragment() {
        StringWriter sw = new StringWriter(1000);
        sw.append(String.format(
                "<field name=\"%s\" parent=\"%s\" indexed=\"%s\" "
                + "tokenized=\"%s\" stored=\"%s\" compressed=\"%s\" "
                + "multiValued=\"%s\" queryBoost=\"%s\" indexBoost=\"%s\" "
                + "sortLocale=\"%s\" " + "inFreeText=\"%s\" required=\"%s\">\n",
                name, parent == null ? "" : parent.getName(), doIndex,
                tokenize, doStore, doCompress,
                multiValued, queryBoost, indexBoost, sortLocale,
                inFreetext, required));
        for (IndexAlias alias: aliases) {
            sw.append(alias.toXMLFragment());
        }
        if (indexAnalyzer != null || indexFilters.size() != 0) {
            sw.append("<analyzer type=\"index\"\n>");
            sw.append(String.format("%s\n",
                                    analyzerToXMLFragment(indexAnalyzer)));
            for (F filter: indexFilters) {
                sw.append(String.format("%s\n", filterToXMLFragment(filter)));
            }
            sw.append("</analyzer>\n");
        }
        sw.append("</field>\n");
        return sw.toString();
    }

    private final XPath xPath = XPathFactory.newInstance().newXPath();
    /**
     * Assign attributes of this field from the given Document Node.
     * The Node should conform to the output from {@link #toXMLFragment()}.
     * If a parent field is specified in the node, the new IndexField will be
     * based on that parent. All attributes specified in the Node will override
     * that of the parent. Aliases will always be overridden.
     * </p><p>
     * If any filters are specified in the node, all filters from the parent
     * will be ignored.
     * If no filters are specified in the node, the filters from the parent
     * will be used.
     * @param node          a representation of a Field.
     * @param fieldProvider if a parent name is specified, the fieldProvider is
     *                      queried for the parent.
     * @throws ParseException if there was an error parsing.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public void parse(Node node, FieldProvider fieldProvider) throws
                                                                ParseException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("parse called");
        String nameVal = ParseUtil.getValue(xPath, node, "@name", (String)null);
        if (nameVal == null) {
            throw new ParseException("No name defined for field", -1);
        }

        if (!nameVal.equals(SUMMA_DEFAULT)) {
            String parentName = ParseUtil.getValue(xPath, node, "@parent",
                                                   (String)null);
            if (parentName == null) {
                parentName = SUMMA_DEFAULT;
                if (fieldProvider.getField(SUMMA_DEFAULT) == null) {
                    log.warn("Could not locate default field '" + SUMMA_DEFAULT
                             + "'");
                }
            }
            log.trace("parse: Inheriting from parent '" + parentName + "'");
            IndexField<A, T, F> parentField;
            try {
                // TODO: Generify this
                //noinspection unchecked
                parentField =
                        (IndexField<A, T, F>)fieldProvider.getField(parentName);
            } catch (ClassCastException e) {
                throw (ParseException)new ParseException(
                        "The FieldProvider did not provide the right type",
                        -1).initCause(e);
            }
            if (parentField == null) {
                log.warn("parse: Could not locate parent '" + parentName
                          + "'");
            } else {
                assignFrom(parentField);
                parent = parentField;
            }
        }
        name = nameVal;
        aliases =     new ArrayList<IndexAlias>(IndexAlias.getAliases(node));
        doIndex =     ParseUtil.getValue(xPath, node, "@indexed",
                                         doIndex);
        // TODO: Consider makin compression an option
        doStore =     ParseUtil.getValue(xPath, node, "@stored",
                                         doStore);
        multiValued = ParseUtil.getValue(xPath, node, "@multiValued",
                                         multiValued);
        queryBoost =  ParseUtil.getValue(xPath, node, "@queryBoost",
                                         queryBoost);
        indexBoost =  ParseUtil.getValue(xPath, node, "@indexBoost",
                                         indexBoost);
        sortLocale =  ParseUtil.getValue(xPath, node, "@sortLocale",
                                         sortLocale);
        inFreetext =  ParseUtil.getValue(xPath, node, "@inFreeText",
                                         inFreetext);
        required =    ParseUtil.getValue(xPath, node, "@required",
                                         required);

        NodeList children = node.getChildNodes();
        for (int i = 0 ; i < children.getLength() ; i++) {
            Node child = children.item(i);
            if (child.getLocalName() != null
                && child.getLocalName().equals("analyzer")) {
                parseAnalyzer(child);
            }
        }
        log.debug("Finished parsing node to construct field " + this);
    }

    private void parseAnalyzer(Node node) throws ParseException {
        String typeAttr = ParseUtil.getValue(xPath, node, "@type",
                                             (String)null);
        boolean parseIndex = false;
        boolean parseQuery = false;
        //noinspection DuplicateStringLiteralInspection
        if ("index".equals(typeAttr)) {
            log.trace("Parsing analyzer information for index");
            parseIndex = true;
        } else if ("query".equals(typeAttr)) {
            log.trace("Parsing analyzer information for query");
            parseQuery = true;
        } else if (typeAttr == null) {
            log.trace("Parsing analyzer information for index and query");
            parseIndex = true;
            parseQuery = true;
        } else {
            log.warn("Unknown value for attribute type in element analyzer: '"
                     + typeAttr + "'. Assigning setup to both index and query");
            parseIndex = true;
            parseQuery = true;
        }

        A analyzer = createAnalyzer(node);
        T tokenizer = null;
        List<F> filters = null;
        try {
            String TOKENIZER = "tokenizer";
            if ((Boolean)xPath.evaluate(TOKENIZER, node,
                                        XPathConstants.BOOLEAN)) {
                tokenizer = createTokenizer((Node)xPath.evaluate(
                        TOKENIZER, node, XPathConstants.BOOLEAN));
            }
            String FILTER = "filter";
            if ((Boolean)xPath.evaluate(
                    FILTER, node, XPathConstants.BOOLEAN)) {
                NodeList filterNodes = (NodeList)xPath.evaluate(
                        FILTER, node, XPathConstants.NODESET);
                filters = new ArrayList<F>(filterNodes.getLength());
                for (int i = 0 ; i < filterNodes.getLength() ; i++) {
                    F filter = createFilter(filterNodes.item(i));
                    if (filter != null) {
                        filters.add(filter);
                    }
                }
            }
        } catch (XPathExpressionException e) {
            throw (ParseException)new ParseException(
                    "Error evaluating expression 'tokenizer'", -1).initCause(e);
        }
        if (parseIndex) {
            indexAnalyzer = analyzer == null ? indexAnalyzer : analyzer;
            indexTokenizer = tokenizer == null ? indexTokenizer : tokenizer;
            indexFilters = filters == null ? indexFilters : filters;
        }
        if (parseQuery) {
            queryAnalyzer = analyzer == null ? queryAnalyzer : analyzer;
            queryTokenizer = tokenizer == null ? queryTokenizer : tokenizer;
            queryFilters = filters == null ? queryFilters : filters;
        }
    }

    /**
     * Checks whether the name of the field or any of its aliases match the
     * given name and language.
     * @param name the name to match.
     * @param lang the language to match. This can be null.
     * @return true is the field matches the name (and language, if specified).
     */
    public boolean isMatch(String name, String lang) {
        if (this.name.equals(name)) {
            return true;
        }
        for (IndexAlias alias: aliases) {
            if (alias.isMatch(name, lang)) {
                return true;
            }
        }
        return false;
    }

    /* Recommended overrides */

    /**
     * Creates an XML fragment for the given analyzer. The fragment _must_ be
     * in the form of an element named tokenizer and must be a mirror of
     * {@link #createAnalyzer}.
     * </p><p>
     * Sample output: <tokenizer class="summa.SummaStandardAnalyzerFactory"/>.
     * @param analyzer the analyzer to create an XML fragment for.
     * @return an XML fragment representing the analyzer.
     */
    protected String analyzerToXMLFragment(A analyzer) {
        throw new UnsupportedOperationException(String.format(
                "XML fragment creation for analyzer '%s' not supported in '%s'",
                analyzer, getClass().toString()));
    }

    /**
     * Creates an analyzer based on the given Document Node.
     * @param node a node representing an analyzer as defined by
     *             {@link #analyzerToXMLFragment(Object)}.
     * @return an analyzer based on the given node.
     */
    protected A createAnalyzer(Node node) {
        throw new UnsupportedOperationException(String.format(
                "Creation of analyzer based on Node '%s' not supported in '%s'",
                node, getClass().toString()));
    }

    /**
     * Creates an XML fragment for the given filter. The fragment _must_ be
     * in the form of an element named filter and must be a mirror of
     * {@link #createFilter}.
     * </p><p>
     * Sample output: <filter class="summa.StopWordFilter" words="words.txt"/>.
     * @param filter the filter to create an XML fragment for.
     * @return an XML fragment representing the analyzer.
     */
    protected String filterToXMLFragment(F filter) {
        throw new UnsupportedOperationException(String.format(
                "XML fragment creation for filter '%s' not supported in '%s'",
                filter, getClass().toString()));
    }

    /**
     * Creates a filter based on the given Document Node.
     * @param node a node representing a filter as defined by
     *             {@link #filterToXMLFragment(Object)}.
     * @return a filter based on the given node.
     */
    protected F createFilter(Node node) {
        throw new UnsupportedOperationException(String.format(
                "Creation of filter based on Node '%s' not supported in '%s'",
                node, getClass().toString()));
    }

    /**
     * @return the default index analyzer.
     */
    protected A getDefaultIndexAnalyzer() {
        log.warn("No default index analyzer assigned");
        return null;
    }

    /**
     * @return the default query analyzer.
     */
    protected A getDefaultQueryAnalyzer() {
        log.warn("No default query analyzer assigned");
        return null;
    }

    /**
     * @return the default index tokenizer (normally null).
     */
    public T getDefaultIndexTokenizer() {
        return null;
    }

    /**
     * @return the default query tokenizer (normally null).
     */
    public T getDefaultQueryTokenizer() {
        return null;
    }

    /**
     * Creates an XML fragment for the given tokenizer. The fragment _must_ be
     * in the form of an element named tokenizer and must be a mirror of
     * {@link #createTokenizer}.
     * </p><p>
     * Sample output: <tokenizer class="summa.SplitOnHyphenFactory"/>.
     * @param tokenizer the tokenizer to create an XML fragment for.
     * @return an XML fragment representing the tokenizer.
     */
    protected String tokenizerToXMLFragment(T tokenizer) {
        throw new UnsupportedOperationException(String.format(
                "XML fragment creation for tokenizer '%s' not supported in "
                + "'%s'",
                tokenizer, getClass().toString()));
    }
    /**
     * Creates a tokenizer based on the given Document Node.
     * @param node a node representing a filter as defined by
     *             {@link #tokenizerToXMLFragment}.
     * @return a tokenizer based on the given node.
     */
    protected T createTokenizer(Node node) {
        throw new UnsupportedOperationException(String.format(
                "Creation of tokenizer based on Node '%s' not supported in "
                + "'%s'",
                node, getClass().toString()));
    }

    /* Fundamental methods */

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof IndexField)) {
            return false;
        }
        IndexField<A, T, F> other;
        try {
            // How do we check for generic types?
            //noinspection unchecked
            other = (IndexField<A, T, F>)o;
        } catch (ClassCastException e) {
            return false;
        }
        return nullCompare(name, other.getName())
               && doIndex == other.isDoIndex()
               && tokenize == other.isTokenize()
               && doStore == other.doStore
               && doCompress == other.isDoCompress()
               && multiValued == other.isMultiValued()
               && queryBoost == other.getQueryBoost() // Consider window
               && indexBoost == other.getIndexBoost() // Consider window
               && nullCompare(sortLocale, other.getSortLocale())
               && inFreetext == other.isInFreetext()
               && required == other.isRequired()
               && nullCompare(indexTokenizer, other.getIndexTokenizer())
               && nullCompare(queryTokenizer, other.getQueryTokenizer())
               && nullCompare(indexAnalyzer, other.getIndexAnalyzer())
               && nullCompare(queryAnalyzer, other.getQueryAnalyzer())
               && listCompare(aliases, other.getAliases())
               && listCompare(indexFilters, other.getIndexFilters())
               && listCompare(queryFilters, other.getQueryFilters());
    }

    private boolean listCompare(List l1, List l2) {
        if (l1 == null) {
            return l2 == null;
        }
        return !(l2 == null || l1.size() != l2.size()) && l1.containsAll(l2);
    }

    private boolean nullCompare(Object o1, Object o2) {
        return o1 == null && o2 == null || o1 != null && o1.equals(o2);
    }

    public int hashCode() {
        return name.hashCode();
    }

    /* Getters */

    public String getName() {
        return name;
    }

    public boolean isDoIndex() {
        return doIndex;
    }

    public boolean isTokenize() {
        return tokenize;
    }

    public boolean isDoStore() {
        return doStore;
    }

    public boolean isDoCompress() {
        return doCompress;
    }

    public IndexField getParent() {
        return parent;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public float getQueryBoost() {
        return queryBoost;
    }

    public float getIndexBoost() {
        return indexBoost;
    }

    public String getSortLocale() {
        return sortLocale;
    }

    public boolean isInFreetext() {
        return inFreetext;
    }

    public boolean isRequired() {
        return required;
    }

    public List<IndexAlias> getAliases() {
        return aliases;
    }

    public A getIndexAnalyzer() {
        return indexAnalyzer;
    }

    public A getQueryAnalyzer() {
        return queryAnalyzer;
    }

    public List<F> getIndexFilters() {
        return indexFilters;
    }

    public List<F> getQueryFilters() {
        return queryFilters;
    }

    public T getQueryTokenizer() {
        return queryTokenizer;
    }

    public T getIndexTokenizer() {
        return indexTokenizer;
    }

    /* Setters */
    public void setDoIndex(boolean doIndex) {
        this.doIndex = doIndex;
    }

    public void setTokenize(boolean tokenize) {
        this.tokenize = tokenize;
    }

    public void setDoStore(boolean doStore) {
        this.doStore = doStore;
    }

    public void setDoCompress(boolean doCompress) {
        this.doCompress = doCompress;
    }

    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }

    public void setQueryBoost(float queryBoost) {
        this.queryBoost = queryBoost;
    }

    public void setIndexBoost(float indexBoost) {
        this.indexBoost = indexBoost;
    }

    public void setSortLocale(String sortLocale) {
        this.sortLocale = sortLocale;
    }

    public void setInFreetext(boolean inFreetext) {
        this.inFreetext = inFreetext;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setIndexAnalyzer(A analyzer) {
        indexAnalyzer = analyzer;
    }

    public void setQueryAnalyzer(A analyzer) {
        queryAnalyzer = analyzer;
    }
}



