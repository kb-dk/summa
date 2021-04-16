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
package dk.statsbiblioteket.summa.common.index;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
     * The boost used when no explicit boost is specified.
     */
    public final static Float DEFAULT_BOOST = 1.0F;

    /**
     * If the field is to be used as a sort field, it can either be lazy or
     * active. If active, the sort-structure is generated upon index open.
     * </p><p>
     * Default is lazy.
     */
    public enum SORT_CACHE {
        lazy, active;

        public static SORT_CACHE defaultCache() {
            return lazy;
        }

        public static SORT_CACHE parse(String cache) {
            if (active.toString().equals(cache)) {
                return active;
            }
            return lazy;
        }
    }

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
    private boolean analyze = true;

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
     *
     * @see #indexBoost
     */
    private float queryBoost = DEFAULT_BOOST;

    /**
     * The boost used when indexing. This boost will be applied to any
     * instantiated field. Boosts specified explicitly for a given instantiated
     * field, will be multiplied to this boost.
     * </p><p>
     * This is used at index-time.
     *
     * @see #queryBoost
     */
    private float indexBoost = DEFAULT_BOOST;

    /**
     * A ISO 639-1 code specifying which locale that should be used for sorting.
     * Note that sorting with locale is a lot heavier than sorting without.
     * </p><p>
     * This is used at search-time.
     *
     * @see <a href="http://www.loc.gov/standards/iso639-2/php/code_list.php">Code List</a>
     */
    private String sortLocale = null;

    private SORT_CACHE sortCache = SORT_CACHE.defaultCache();

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
    private boolean required = true;

    /**
     * Aliases for this index-field. If alias-expansion is used at query-time,
     * aliases are resolved to field-names in the index.
     * </p><p>
     * This is used at query-time.
     */
    private List<IndexAlias> aliases = new ArrayList<>(10);

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
    private List<F> indexFilters = new ArrayList<>(10);

    /**
     * The filters to use for query expansion. The type of filter is defined by
     * the user of this class.
     * </p><p>
     * This is used at query-time.
     */
    private List<F> queryFilters = new ArrayList<>(10);

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
     *
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
     *
     * @param node          a representation of a Field.
     * @param fieldProvider if a parent name is specified, the fieldProvider is
     *                      queried for the parent.
     * @throws ParseException if the node could not be parsed properly.
     */
    public IndexField(Node node, FieldProvider fieldProvider) throws ParseException {
        log.trace("Creating field based on node " + node);
        parse(node, fieldProvider);
    }

    /**
     * Assigns all the values from parent to the newly created field, then
     * assigns the {@link #parent} attribute to the parent field.
     *
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
     *
     * @param parent the field to get values from.
     */
    protected void assignFrom(IndexField<A, T, F> parent) {
        log.trace("Assigning from " + parent);
        name = parent.getName();
        this.parent = parent.getParent();
        doIndex = parent.isDoIndex();
        analyze = parent.isAnalyze();
        doStore = parent.isDoStore();
        doCompress = parent.isDoCompress();
        multiValued = parent.isMultiValued();
        queryBoost = parent.getQueryBoost();
        indexBoost = parent.getIndexBoost();
        sortLocale = parent.getSortLocale();
        sortCache = parent.sortCache;
        inFreetext = parent.isInFreetext();
        required = parent.isRequired();
        aliases = new ArrayList<>(parent.getAliases());
        indexAnalyzer = parent.getIndexAnalyzer();
        queryAnalyzer = parent.getQueryAnalyzer();
        indexFilters = new ArrayList<>(parent.getIndexFilters());
        queryFilters = new ArrayList<>(parent.getQueryFilters());
    }

    public String toString() {
        return "Field(" + name + ")";
    }

    /**
     * Construct an XML fragment describing this field, suitable for insertion
     * in IndexDescriptor XML or similar persistent structure.
     *
     * @return an XML for this field.
     */
    public String toXMLFragment() {
        StringWriter sw = new StringWriter(1000);
        sw.append(String.format(
                Locale.ROOT,
                "<field name=\"%s\" parent=\"%s\" indexed=\"%s\" tokenized=\"%s\" stored=\"%s\" compressed=\"%s\" "
                        + "multiValued=\"%s\" queryBoost=\"%s\" indexBoost=\"%s\" sortLocale=\"%s\" sortCache=\"%s\" " +
                        "inFreeText=\"%s\" required=\"%s\" tokenized=\"%s\">\n",
                name, parent == null ? "" : parent.getName(), doIndex, analyze, doStore, doCompress,
                multiValued, queryBoost, indexBoost, sortLocale, sortCache, inFreetext, required, analyze));
        for (IndexAlias alias : aliases) {
            sw.append(alias.toXMLFragment());
        }
        if (indexAnalyzer != null || !indexFilters.isEmpty()) {
            sw.append("<analyzer type=\"index\"\n>");
            sw.append(String.format(Locale.ROOT, "%s\n", analyzerToXMLFragment(indexAnalyzer)));
            for (F filter : indexFilters) {
                sw.append(String.format(Locale.ROOT, "%s\n", filterToXMLFragment(filter)));
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
     *
     * @param node          a representation of a Field.
     * @param fieldProvider if a parent name is specified, the fieldProvider is
     *                      queried for the parent.
     * @throws ParseException if there was an error parsing.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection", "unchecked"})
    public void parse(Node node, FieldProvider fieldProvider) throws ParseException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("parse called");
        long startTime = System.nanoTime();
        //String nameVal = ParseUtil.getValue(xPath, node, "@name", (String)null);
        //String nameVal = DOM.selectString(node, "@name", null);
        String nameVal = getString(node, "name", null);

        if (nameVal == null) {
            throw new ParseException("No name defined for field", -1);
        }

        if (!nameVal.equals(SUMMA_DEFAULT)) {
            //String parentName = ParseUtil.getValue(xPath, node, "@parent",
            //                                       (String)null);
            //String parentName = DOM.selectString(node, "@parent", null);
            String parentName = getString(node, "parent", null);
            if (parentName == null) {
                parentName = SUMMA_DEFAULT;
                if (fieldProvider.getField(SUMMA_DEFAULT) == null) {
                    log.warn("Could not locate default field '" + SUMMA_DEFAULT + "'");
                }
            }
            log.trace("parse: Inheriting from parent '" + parentName + "'");
            IndexField<A, T, F> parentField;
            try {
                // TODO: Generify this
                //noinspection unchecked
//                parentField = (IndexField<A, T, F>) fieldProvider.getFieldWithLocale( // concat needs locale
//                        nameVal, parentName, DOM.selectString(node, "@sortLocale", null));
                parentField = (IndexField<A, T, F>) fieldProvider.getFieldWithLocale( // concat needs locale
                        nameVal, parentName, getString(node, "sortLocale", null));
            } catch (ClassCastException e) {
                throw (ParseException) new ParseException(
                        "The FieldProvider did not provide the right type", -1).initCause(e);
            }
            if (parentField == null) {
                log.warn("parse: Could not locate parent '" + parentName + "'");
            } else {
                assignFrom(parentField);
                parent = parentField;
            }
        }
        name = nameVal;
        aliases = new ArrayList<>(IndexAlias.getAliases(node));
        //doIndex =     ParseUtil.getValue(xPath, node, "@indexed",
        //                                 doIndex);
//        doIndex = DOM.selectBoolean(node, "@indexed", doIndex);
        doIndex = getBoolean(node, "indexed", doIndex);
        //doStore =     ParseUtil.getValue(xPath, node, "@stored",
        //                                 doStore);
//        doStore = DOM.selectBoolean(node, "@stored", doStore);
        doStore = getBoolean(node, "stored", doStore);
        //multiValued = ParseUtil.getValue(xPath, node, "@multiValued",
        //                                 multiValued);
//        multiValued = DOM.selectBoolean(node, "@multiValued", multiValued);
        multiValued = getBoolean(node, "multiValued", multiValued);
        //queryBoost =  ParseUtil.getValue(xPath, node, "@queryBoost",
        //                                 queryBoost);
//        queryBoost = DOM.selectDouble(node, " @queryBoost", new Float(queryBoost).doubleValue()).floatValue();
        queryBoost = Double.valueOf(getDouble(node, "queryBoost", new Float(queryBoost).doubleValue())).floatValue();
        //indexBoost =  ParseUtil.getValue(xPath, node, "@indexBoost",
        //                                 indexBoost);
        //indexBoost = DOM.selectDouble(node, "@indexBoost", new Float(indexBoost).doubleValue()).floatValue();
        indexBoost = Double.valueOf(getDouble(node, "indexBoost", new Float(indexBoost).doubleValue())).floatValue();
        //analyze =    ParseUtil.getValue(xPath, node, "@analyzed",
        //                                analyze);
//        analyze = DOM.selectBoolean(node, "@analyzed", analyze);
        analyze = getBoolean(node, "analyzed", analyze);
        //doCompress =  ParseUtil.getValue(xPath, node, "@compressed",
        //                                 doCompress);
        //doCompress = DOM.selectBoolean(node, "@compressed", doCompress);
        doCompress = getBoolean(node, "compressed", doCompress);
        //sortLocale =  ParseUtil.getValue(xPath, node, "@sortLocale",
        //                                 sortLocale);
        //sortLocale = DOM.selectString(node, "@sortLocale", sortLocale);
        sortLocale = getString(node, "sortLocale", sortLocale);
        //sortCache =  SORT_CACHE.parse(ParseUtil.getValue(
        //        xPath, node, "@sortCache", sortCache.toString()));
//        sortCache = SORT_CACHE.parse(DOM.selectString(node, "@sortCache", sortCache.toString()));
        sortCache = SORT_CACHE.parse(getString(node, "sortCache", sortCache.toString()));
        //inFreetext =  ParseUtil.getValue(xPath, node, "@inFreeText",
        //                                 inFreetext);
//        inFreetext = DOM.selectBoolean(node, "@inFreeText", inFreetext);
        inFreetext = getBoolean(node, "inFreeText", inFreetext);
        //required =    ParseUtil.getValue(xPath, node, "@required",
        //                                 required);
        //required = DOM.selectBoolean(node, "@required", required);
        required = getBoolean(node, "required", required);

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName() != null && "analyzer".equals(child.getNodeName())) {
                parseAnalyzer(child);
            }
        }
        log.debug("Finished parsing node to construct field " + this + " in " + (System.nanoTime()-startTime) + "ns");
    }

    private boolean getBoolean(Node node, String attribute, boolean defaultValue) {
        Node value = node.getAttributes().getNamedItem(attribute);
        return value == null ? defaultValue : Boolean.parseBoolean(value.getNodeValue());
    }

    private double getDouble(Node node, String attribute, double defaultValue) {
        Node value = node.getAttributes().getNamedItem(attribute);
        return value == null ? defaultValue : Double.parseDouble(value.getNodeValue());
    }

    private String getString(Node node, String attribute, String defaultValue) {
        Node value = node.getAttributes().getNamedItem(attribute);
        return value == null ? defaultValue : value.getNodeValue();
    }

    private void parseAnalyzer(Node node) throws ParseException {
        //String typeAttr = ParseUtil.getValue(xPath, node, "@type",
        //                                     (String)null);
        //String typeAttr = DOM.selectString(node, "@type", null);
        String typeAttr = getString(node, "type", null);
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
            if ((Boolean) xPath.evaluate(TOKENIZER, node, XPathConstants.BOOLEAN)) {
                tokenizer = createTokenizer((Node) xPath.evaluate(TOKENIZER, node, XPathConstants.BOOLEAN));
            }
            String FILTER = "filter";
            if ((Boolean) xPath.evaluate(FILTER, node, XPathConstants.BOOLEAN)) {
                NodeList filterNodes = (NodeList) xPath.evaluate(FILTER, node, XPathConstants.NODESET);
                filters = new ArrayList<>(filterNodes.getLength());
                for (int i = 0; i < filterNodes.getLength(); i++) {
                    F filter = createFilter(filterNodes.item(i));
                    if (filter != null) {
                        filters.add(filter);
                    }
                }
            }
        } catch (XPathExpressionException e) {
            throw (ParseException) new ParseException(
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
     *
     * @param name the name to match.
     * @param lang the language to match. This can be null.
     * @return true is the field matches the name (and language, if specified).
     */
    public boolean isMatch(String name, String lang) {
        if (this.name.equals(name)) {
            return true;
        }
        for (IndexAlias alias : aliases) {
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
     *
     * @param analyzer the analyzer to create an XML fragment for.
     * @return an XML fragment representing the analyzer.
     */
    protected String analyzerToXMLFragment(A analyzer) {
        throw new UnsupportedOperationException(String.format(Locale.ROOT,
                "XML fragment creation for analyzer '%s' not supported in '%s'",
                analyzer, getClass().toString()));
    }

    /**
     * Creates an analyzer based on the given Document Node.
     *
     * @param node a node representing an analyzer as defined by
     *             {@link #analyzerToXMLFragment(Object)}.
     * @return an analyzer based on the given node.
     */
    protected A createAnalyzer(Node node) {
        throw new UnsupportedOperationException(String.format(Locale.ROOT,
                "Creation of analyzer based on Node '%s' not supported in '%s'",
                node, getClass().toString()));
    }

    /**
     * Creates an XML fragment for the given filter. The fragment _must_ be
     * in the form of an element named filter and must be a mirror of
     * {@link #createFilter}.
     * </p><p>
     * Sample output: <filter class="summa.StopWordFilter" words="words.txt"/>.
     *
     * @param filter the filter to create an XML fragment for.
     * @return an XML fragment representing the analyzer.
     */
    protected String filterToXMLFragment(F filter) {
        throw new UnsupportedOperationException(String.format(Locale.ROOT,
                "XML fragment creation for filter '%s' not supported in '%s'",
                filter, getClass().toString()));
    }

    /**
     * Creates a filter based on the given Document Node.
     *
     * @param node a node representing a filter as defined by
     *             {@link #filterToXMLFragment(Object)}.
     * @return a filter based on the given node.
     */
    protected F createFilter(Node node) {
        throw new UnsupportedOperationException(String.format(Locale.ROOT,
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
     *
     * @param tokenizer the tokenizer to create an XML fragment for.
     * @return an XML fragment representing the tokenizer.
     */
    protected String tokenizerToXMLFragment(T tokenizer) {
        throw new UnsupportedOperationException(String.format(Locale.ROOT,
                "XML fragment creation for tokenizer '%s' not supported in '%s'",
                tokenizer, getClass().toString()));
    }

    /**
     * Creates a tokenizer based on the given Document Node.
     *
     * @param node a node representing a filter as defined by
     *             {@link #tokenizerToXMLFragment}.
     * @return a tokenizer based on the given node.
     */
    protected T createTokenizer(Node node) {
        throw new UnsupportedOperationException(String.format(Locale.ROOT,
                "Creation of tokenizer based on Node '%s' not supported in '%s'",
                node, getClass().toString()));
    }

    /* Fundamental methods */
    @SuppressWarnings({"unchecked"})
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
            other = (IndexField<A, T, F>) o;
        } catch (ClassCastException e) {
            return false;
        }
        return nullCompare(name, other.getName())
                && doIndex == other.isDoIndex()
                && analyze == other.isAnalyze()
                && doStore == other.doStore
                && doCompress == other.isDoCompress()
                && multiValued == other.isMultiValued()
                && equals(queryBoost, other.getQueryBoost(), 0.001f)
                && equals(indexBoost, other.getIndexBoost(), 0.001f)
                && nullCompare(sortLocale, other.getSortLocale())
                && sortCache == other.getSortCache()
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

    private boolean equals(float f1, float f2, float maxDelta) {
        return Math.abs(f1-f2) < maxDelta;
    }

    @SuppressWarnings({"unchecked"})
    // TODO can this be generified?
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

    public boolean isAnalyze() {
        return analyze;
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

    public SORT_CACHE getSortCache() {
        return sortCache;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setDoIndex(boolean doIndex) {
        this.doIndex = doIndex;
    }

    public void setAnalyze(boolean analyze) {
        this.analyze = analyze;
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

    public void setSortCache(SORT_CACHE sortCache) {
        this.sortCache = sortCache;
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
