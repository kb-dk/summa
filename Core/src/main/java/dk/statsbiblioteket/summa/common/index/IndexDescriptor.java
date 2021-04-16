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

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.configuration.SubConfigurationsNotSupportedException;
import dk.statsbiblioteket.summa.common.lucene.index.IndexUtils;
import dk.statsbiblioteket.summa.common.util.ResourceListener;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Logs;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/**
 * A description of the layout of the index, such as default fields, groups
 * and analyzers. This is Summa's equivalent of SOLR's schema.xml.
 * </p><p>
 * The IndexDescriptor is needed for index document building and for query
 * expansion. The IndexDescriptor is kept fairly index-implementation agnostic,
 * although it was modelled with Lucene in mind. As long as the index
 * implementation is field-based with stored and indexed fields et al,
 * it should be easy to extend the IndexDescriptor.
 * </p><p>
 * The IndexDescriptor has groups, which schema.xml does not, and is otherwise
 * somewhat simpler. It is envisioned that SOLR's classes for indexing and
 * querying can be used in Summa instead of raw Lucene at some point in time. *
 * </p><p>
 * The IndexDescriptor is abstract. Besides implementing the abstract methods,
 * sub classes will normally need to override {@link #createBaseField(String)}.
 *
 * @see <a href="http://wiki.apache.org/solr/SchemaXml">Solr Schema XML</a>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
// TODO: Warn if aliases clashes with field- or group-names
public abstract class IndexDescriptor<F extends IndexField> implements Configurable, FieldProvider<F> {
    private static Log log = LogFactory.getLog(IndexDescriptor.class);

    public static final String DESCRIPTOR_NAMESPACE = "http://statsbiblioteket.dk/summa/2008/IndexDescriptor";
    public static final String DESCRIPTOR_NAMESPACE_PREFIX = "id";

    /**
     * If a location root is specified, the resource locationRoot+current.txt
     * will be fetched. The resource current.txt must contain an absolute URL
     * to the persistent data for the IndexDescriptor.
     * </p><p>
     * This is by the "add another level of indirection"-principle and allows
     * for a centralized pointer to the current version.
     * </p><p>
     * This property is an URL. It must end in "/".<br />
     * Example 1: http://example.org/summa/index_descriptors/<br />
     * Example 2: file:///home/summa/descriptors/
     * </p><p>
     * Either this property or {@link #CONF_ABSOLUTE_LOCATION} must be present.
     */
    public static final String CONF_LOCATION_ROOT = "summa.common.indexdescriptor.locationroot";
    public static final String CURRENT = "current.txt";

    /**
     * If an absolute location is specified in the configuration, the state of
     * the IndexDescriptor is fetched from there.
     * </p><p>
     * This property is an URL.
     * </p><p>
     * Either this property or {@link #CONF_LOCATION_ROOT} must be present.
     */
    public static final String CONF_ABSOLUTE_LOCATION = "summa.common.indexdescriptor.absolutelocation";

    /**
     * How often the IndexDescriptor should be re-read from the resolved
     * absolute location, in milliseconds. A value of -1 turns off re-reading.
     * </p><p>
     * This property is optional. Default is -1.
     */
    public static final String CONF_CHECK_INTERVAL = "summa.common.index.checkinterval";
    public static final int DEFAULT_CHECK_INTERVAL = -1;

    /**
     * The property-key for a substorage containing the properties for the
     * IndexDescriptor. This is used for inlining descriptor setup in other
     * configurations.
     */
    public static final String CONF_DESCRIPTOR = "summa.index.descriptorsetup";

    /**
     * If the IndexDescriptor is to be stored, this is the default file name.
     */
    public static final String DESCRIPTOR_FILENAME = "IndexDescriptor.xml";

    public static enum OPERATOR {and, or}

    public static final String KEYWORD = "keyword";
    public static final String STORED_KEYWORD = "storedKeyword";
    public static final String LOWERCASE = "lowercase";
    public static final String VERBATIM = "verbatim";
    public static final String STORED_VERBATIM = "storedVerbatim";
    public static final String TEXT = "text";
    public static final String SORTKEY = "sortkey";
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String STORED = "stored";
    public static final String DATE = "date";
    public static final String NUMBER = "number";

    /**
     * The base fields must be defined in all implementations of the
     * IndexDescriptor. This is enforced by the IndexDescriptor calling
     * {@link #createBaseField} with all BASE_FIELDS.
     */
    public static final String[] BASE_FIELDS = new String[]{IndexField.SUMMA_DEFAULT, // Index, store
            IndexUtils.RECORD_FIELD,  // Index (no analyze), store
            IndexUtils.RECORD_BASE,   // Index (no analyze), store
            IndexField.FREETEXT,      // Index
            KEYWORD, STORED_KEYWORD,  // Index / Index, store
            VERBATIM, STORED_VERBATIM,// Index / Index, store
            LOWERCASE,                // Index
            TEXT,                     // Index
            SORTKEY,                  // Index
            STORED,                   // Store
            DATE,                     // Index
            NUMBER                   // Index, store
    };

    private ResourceListener listener;
    private URL absoluteLocation;


    /**
     * All Fields mapped from field name => Field object.
     */
    private Map<String, F> allFields = new LinkedHashMap<>();

    /**
     * Fields that performs prefix matching (specified with [@code fieldprefix*} in field name in the index descriptor).
     * Note that these fields cannot be part of a group.
     */
    private List<F> prefixFields = new ArrayList<>();

    /**
     * All Groups mapped from group name => Group object. All the Fields
     * contained in the groups MUST be present in {@link #allFields}.
     */
    private Map<String, IndexGroup<F>> groups = new LinkedHashMap<>(20);

    // TODO: Assign this based on XML
    protected F defaultField = createNewField();
    private String defaultLanguage = "en";
    private List<String> defaultFields = Arrays.asList(IndexField.FREETEXT);
    private OPERATOR defaultOperator = OPERATOR.and;

    /**
     * Extracts a locationRoot or an absoluteLocation from configuration,
     * depending on which of the keys are present, and uses that to load
     * persistent data for the descriptor.
     * </p><p>
     * If both keys are present, absoluteLocation takes precedence.
     * </p><p>
     *
     * @param configuration contains the location of a stored IndexDescriptor.
     * @throws IOException if no persistent data could be loaded and parsed.
     */
    public IndexDescriptor(Configuration configuration) throws IOException {
        init();
        createBaseFields();
        String locationRoot = configuration.getString(CONF_LOCATION_ROOT, null);
        String absoluteLocationString = configuration.getString(CONF_ABSOLUTE_LOCATION, null);
        if (locationRoot == null && absoluteLocationString == null) {
            //noinspection DuplicateStringLiteralInspection
            log.error("Either " + CONF_LOCATION_ROOT + " or " + CONF_ABSOLUTE_LOCATION + " must be present in the "
                      + "configuration. Using default index descriptor. It is highly recommended to specify the " 
                      + "location of a descriptor setup");
            return;
        }
        if (absoluteLocationString != null) {
            absoluteLocation = Resolver.getURL(absoluteLocationString);
            if (absoluteLocation == null) {
                throw new IOException(String.format(Locale.ROOT, "Could not resolve property %s with value '%s'",
                                                    CONF_ABSOLUTE_LOCATION, absoluteLocationString));
            }
        }
        if (locationRoot != null && absoluteLocationString != null) {
            log.debug("Both " + CONF_LOCATION_ROOT + "(" + locationRoot + ") and " + CONF_ABSOLUTE_LOCATION + "("
                      + absoluteLocationString + ") is defined. " + CONF_ABSOLUTE_LOCATION + " will be used");
        }
        if (absoluteLocation == null) {
            try {
                absoluteLocation = resolveAbsoluteLocation(locationRoot);
            } catch (IOException e) {
                throw new IOException("Cannot resolve location root " + locationRoot + "' to absolute location", e);
            }
        }
        int checkInterval = configuration.getInt(CONF_CHECK_INTERVAL, DEFAULT_CHECK_INTERVAL);
        fetchStateAndActivateListener(checkInterval);
    }

    /**
     * Constructs a new empty IndexDescriptor. This can be used to set up an
     * IndexDescriptor programatically, instead of XML-based.
     */
    public IndexDescriptor() {
        log.debug("Empty descriptor created");
        init();
        createBaseFields();
    }

    /**
     * Constructs an IndexDescriptor based on the stated resource. The resource
     * will only be loaded once.
     *
     * @param absoluteLocation the location of the XML-representation of the
     *                         descriptor.
     * @throws IOException if no persistent data could be loaded and parsed.
     */
    public IndexDescriptor(URL absoluteLocation) throws IOException {
        log.trace("Creating descriptor based on '" + absoluteLocation + "'");
        init();
        createBaseFields();
        this.absoluteLocation = absoluteLocation;
        fetchStateAndActivateListener(0);
    }

    /**
     * Construct an IndexDescriptor based on the given xml.
     *
     * @param xml an XML-representation of an IndexDescriptor.
     * @throws ParseException if the xml could not be parsed peoperly.
     */
    public IndexDescriptor(String xml) throws ParseException {
        log.trace("Creating descriptor based on XML");
        init();
        createBaseFields();
        parse(xml);
        log.debug("Descriptor created based on XML");
    }

    private void fetchStateAndActivateListener(final int checkInterval) throws IOException {
        listener = new ResourceListener(absoluteLocation, checkInterval, false) {

            @Override
            public void resourceChanged(String newContent) throws Exception {
                parse(newContent);
            }
        };
        if (!listener.performCheck()) {
            throw new IOException(
                    "Could not load description from '" + absoluteLocation + "'", listener.getLastException());
        }
        listener.setActive(checkInterval > 0);
    }

    private static URL resolveAbsoluteLocation(String locationRoot) throws IOException {
        log.debug("Resolving " + CURRENT + " in location root '" + locationRoot + "'");
        if (locationRoot == null) {
            throw new IOException("The locationRoot is null");
        }
        String indirection = locationRoot + CURRENT;
        URL url = Resolver.getURL(indirection);
        String content;
        try {
            content = Resolver.getUTF8Content(url);
        } catch (IOException e) {
            throw new IOException(String.format(Locale.ROOT, "Unable to get content from URL '%s', resolved from '%s'", url,
                                                locationRoot), e);
        }
        String tokens[] = content.split("\n");
        URL absoluteLocation = Resolver.getURL(tokens[0].trim());
        log.debug("fetchDescription: Got absoluteLocation '" + absoluteLocation + "' from '" + indirection + "'");
        return absoluteLocation;
    }

    /**
     * The init-method will be called before any IndexDescriptor-specific
     * initialization takes place. Override this method to provide
     * implementation-specific initialization.
     * </p><p>
     * Note: base fields are created explicitely by {@link #createBaseFields}.
     */
    public void init() {
        log.trace("init() does nothing in the default method");
    }

    /**
     * Iterator over {@link #BASE_FIELDS} that calls {@link #createBaseField}
     * and adds results to {@link #allFields}. Called by all constructors.
     */
    private void createBaseFields() {
        for (String baseFieldName : BASE_FIELDS) {
            F field = createBaseField(baseFieldName);
            allFields.put(field.getName(), field);
        }
    }

    /**
     * Create a base field from the given name. One special field is the
     * {@link IndexField#SUMMA_DEFAULT} which is the field that all other
     * fields should inherit from. The first time this method is called, it
     * will be with this field name.
     * </p><p>
     * The basic implementation of the is method does create valid IndexFields
     * which can be used for analysis of the IndexDescriptor. However, it is
     * expected that implementations will normally want to override the method.
     *
     * @param baseFieldName the name of the field. It is guaranteed that all the
     *                      names from {@link #BASE_FIELDS} will be fed to this
     *                      method.
     * @return a base field from the given name.
     */
    protected F createBaseField(String baseFieldName) {
        log.debug(String.format(Locale.ROOT, "createBaseField(%s) called", baseFieldName));
        if (baseFieldName.equals(IndexField.SUMMA_DEFAULT) || baseFieldName.equals(IndexUtils.RECORD_FIELD)
            || baseFieldName.equals(IndexUtils.RECORD_BASE) || baseFieldName.equals(NUMBER)) {
            return createNewField(baseFieldName, true, true);
        }
        if (baseFieldName.equals(IndexField.FREETEXT) || baseFieldName.equals(KEYWORD) || baseFieldName.equals(VERBATIM)
            || baseFieldName.equals(LOWERCASE) || baseFieldName.equals(TEXT) || baseFieldName.equals(SORTKEY)
            || baseFieldName.equals(DATE)) {
            return createNewField(baseFieldName, true, false);
        }
        if (baseFieldName.equals(STORED_KEYWORD) || baseFieldName.equals(STORED_VERBATIM)
            || baseFieldName.equals(STORED)) {
            return createNewField(baseFieldName, false, true);
        }

        throw new IllegalArgumentException(String.format(Locale.ROOT, "The base field '%s' is unknown", baseFieldName));
    }

    private F createNewField(String name, boolean index, boolean store) {
        F field = createNewField();
        field.setName(name);
        field.setDoIndex(index);
        field.setDoStore(store);
        return field;
    }

    /**
     * Parses the provided XML and sets the state of this IndexDecriptor
     * accordingly. A parse replaces the previous state completely. If an
     * exception is thrown, the state is guaranteed to be the same as before
     * parse was called. See the class documentation for the format of the XML.
     *
     * @param xml an XML representation of an IndexDescriptor.
     * @return the input parsed as a Document, for further processing.
     * @throws ParseException if there was an error parsing the xml.
     */
    public synchronized Document parse(String xml) throws ParseException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("parse called");
        if (xml == null) {
            throw new IllegalArgumentException("The xml for parse was null");
        }
        Document document = DOM.stringToDOM(xml);

        parseDefaultLanguage(document);

        parseDefaultSearchFields(document);

        parseQueryParser(document);

        parseFields(document);

        parseGroups(document);

        // Sanity check
        for (String defaultField : defaultFields) {
            if (allFields.get(defaultField) == null && groups.get(defaultField) == null) {
                log.warn("The specified default field '" + defaultField
                         + "' did not have any corresponding field or group");
            }
        }
        return document;
    }

    @SuppressWarnings("UnusedDeclaration")
    private Document parseXMLToDocument(String xml) throws ParseException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        builderFactory.setValidating(false);
        DocumentBuilder builder;
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw (ParseException) new ParseException("Could not create document builder", -1).initCause(e);
        }

        try {
            return builder.parse(new InputSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
        } catch (SAXException e) {
            throw (ParseException) new ParseException(
                    "Could not create Document from xml '" + xml + "'", -1).initCause(e);
        } catch (UnsupportedEncodingException e) {
            //noinspection DuplicateStringLiteralInspection
            throw (ParseException) new ParseException("utf-8 not supported", -1).initCause(e);
        } catch (IOException e) {
            throw (ParseException) new ParseException(
                    "Could not create ByteArrayInputStream from xml '" + xml + "'", -1).initCause(e);
        }
    }

    private void parseDefaultSearchFields(Document document) throws ParseException {
        NodeList defaultNodes;

        final String DEFAULT_EXPR = "/IndexDescriptor/defaultSearchFields/field";
        defaultNodes = DOM.selectNodeList(document, DEFAULT_EXPR);

        defaultFields = new ArrayList<>(defaultNodes.getLength());
        //noinspection DuplicateStringLiteralInspection
        log.trace("Located " + defaultNodes.getLength() + " default search field nodes");
        for (int i = 0; i < defaultNodes.getLength(); i++) {
            String dField = DOM.selectString(defaultNodes.item(i), "@ref", null);
            if (dField == null) {
                log.warn("No ref-attribute for field in defaultSearchFields");
            } else {
                defaultFields.add(dField);
            }
        }
        if (defaultFields.isEmpty()) {
            log.warn("No default fields specified");
        } else {
            log.info("Default search fields: " + Logs.expand(defaultFields, 20));
        }
    }

    private void parseDefaultLanguage(Document document) throws ParseException {
        defaultLanguage = DOM.selectString(document, "/IndexDescriptor/defaultLanguage", defaultLanguage);
    }

    private void parseGroups(Document document) throws ParseException {
        NodeList groupNodes;
        //noinspection DuplicateStringLiteralInspection
        final String GROUP_EXPR = "/IndexDescriptor/groups/group";

        //noinspection DuplicateStringLiteralInspection
        groupNodes = DOM.selectNodeList(document, GROUP_EXPR);
        log.trace("Located " + groupNodes.getLength() + " group nodes");

        for (int i = 0; i < groupNodes.getLength(); i++) {
            log.debug(groupNodes.item(i).getNodeName());
            addGroup(new IndexGroup<>(groupNodes.item(i), this));
        }
    }

    private void parseFields(Document document) throws ParseException {
        NodeList fieldNodes;
        final String FIELD_EXPR = "/IndexDescriptor/fields/field";

        fieldNodes = DOM.selectNodeList(document, FIELD_EXPR);
        //noinspection DuplicateStringLiteralInspection
        log.trace("Located " + fieldNodes.getLength() + " field nodes");

//        long startTime = System.currentTimeMillis();
  /*      Does not work
        Node node = fieldNodes.getLength() == 0 ? null : fieldNodes.item(0);
        while (node != null) {
            node = node.getNextSibling();
            if (node.hasAttributes()) {
                System.out.println("No attributes");
            }
        }
*/
        /* ~20ms on i7
        for (int i = 0; i < fieldNodes.getLength(); i++) {
            Node node = fieldNodes.item(i);
            node.getParentNode().removeChild(node);
            if (!node.hasAttributes()) {
                System.out.println("No attributes");
            }
        }

        System.out.println("Clean run: " + (System.currentTimeMillis()-startTime) + "ms");
*/
        /* No speed difference
            for (int i = 0; i < fieldNodes.getLength(); i++) {
            Node node = fieldNodes.item(i);
            node.getParentNode().removeChild(node);
            addField(createNewField(node));
        }*/


        for (int i = 0; i < fieldNodes.getLength(); i++) {
            addField(createNewField(fieldNodes.item(i)));
        }
    }

    private void parseQueryParser(Document document) throws ParseException {
        //String dop = ParseUtil.getValue(xPath, document,
        //        "id:IndexDescriptor/id:QueryParser/@defaultOperator",
        //        defaultOperator.toString());
        String dop = DOM.selectString(document, "/IndexDescriptor/QueryParser/@defaultOperator", 
                                      defaultOperator.toString());
        if ("or".equals(dop.toLowerCase())) {
            defaultOperator = OPERATOR.or;
        } else if ("and".equals(dop.toLowerCase())) {
            defaultOperator = OPERATOR.and;
        } else {
            log.warn("Unexpected value '" + dop + "' found in QueryParser#defaultOperator");
        }
    }

    /*private XPath xPath = createXPath();
    private XPath createXPath() {
        DefaultNamespaceContext nsCon = new DefaultNamespaceContext();
        nsCon.setNameSpace(DESCRIPTOR_NAMESPACE, DESCRIPTOR_NAMESPACE_PREFIX);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(nsCon);
        return xPath;
    } */

    /**
     * Stores an XML representation of this IndexDescriptor to the given
     * location. See the class documentation for the format of the XML.
     *
     * @param location where to store the XML representation.
     * @throws IOException if the representation could not be stored.
     */
    public synchronized void save(File location) throws IOException {
        log.debug("Storing descriptor to '" + location + "'");
        Files.saveString(toXML(), location);
    }

    /**
     * XML-representation usable for persistence. This is the format that {@link #parse} accepts.
     *
     * @return a well-formed XML representation of the descriptor.
     * @see #parse(String)
     */
    public String toXML() {
        StringWriter sw = new StringWriter(10000);
        sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sw.append("<IndexDescriptor version=\"1.0\">\n");

        sw.append("<groups>\n");
        for (IndexGroup<F> g : groups.values()) {
            sw.append(g.toXMLFragment());
        }
        sw.append("</groups>\n");

        sw.append("<fields>\n");
        for (Map.Entry<String, F> entry : allFields.entrySet()) {
            sw.append(entry.getValue().toXMLFragment());
        }
        // TODO: Add prefix fields to this
        sw.append("</fields>\n");

        sw.append("<defaultLanguage>").append(defaultLanguage);
        sw.append("</defaultLanguage>\n");

//        sw.append("<uniqueKey>").append(uniqueKey).append("</uniqueKey>\n");

        sw.append("<defaultSearchFields>\n");
        for (String defaultField : defaultFields) {
            sw.append("  <field ref=\"").append(defaultField).append("\"/>\n");
        }
        sw.append("</defaultSearchFields>\n");

        sw.append("<QueryParser defaultOperator=\"");
        sw.append(defaultOperator.toString());
        sw.append("\"/>\n");

        //noinspection DuplicateStringLiteralInspection
        sw.append("</IndexDescriptor>");
        return sw.toString();
    }

    /**
     * Close down the IndexDescriptor, freeing underlying threads.
     */
    public synchronized void close() {
        //noinspection DuplicateStringLiteralInspection
        log.trace("close() called");
        if (listener != null) {
            listener.setActive(false);
        }
    }

    /**
     * Adds the given IndexField to the descriptors fields, if the
     * Field-object is not already present. Existence is defined by the
     * existence of a Field with the same name.
     * </p><p>
     * Note: Id a field with the name {@link IndexField#SUMMA_DEFAULT} is
     * added, it will be the new {@link #defaultField}.
     *
     * @param field the field to add to the descriptor.
     * @return true if the Field was added, else false.
     * @see #allFields
     */
    public synchronized boolean addField(F field) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("addField(" + field + ") called");
        if (field.getName().endsWith("*")) {
            return addPrefixField(field);
        }
        if (allFields.get(field.getName()) != null) {
            log.debug("A Field with name '" + field.getName() + "' is already "
                      + "present in allFields. The old field will be replaced");
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding " + field + " to allFields");
        allFields.put(field.getName(), field);
        if (field.getName().equals(IndexField.SUMMA_DEFAULT)) {
            log.debug("Assigning new default field");
            defaultField = field;
        }
        return true;
    }

    private boolean addPrefixField(F field) {
    // TODO: We should throw a parse error is someone attempts to alias a dynamic field
        field.setName(field.getName().substring(0, field.getName().length()-1)); // remove *
        // TODO: Add duplicate detection and warning
        //noinspection DuplicateStringLiteralInspection
        log.debug("Adding " + field + " to prefixFields");
        prefixFields.add(field);
        return true;
    }

    /**
     * Adds the given IndexGroup to the descriptor, if the group is not already
     * present. Presence is defined by the existence of a Group with the same
     * name.
     * </p><p>
     * Note: Adding a Group automatically adds all contained Fields to
     * {@link #allFields}.
     *
     * @param group the group to add to the descriptor.
     * @return true if the Group was added, else false.
     * @see #groups
     */
    public synchronized boolean addGroup(IndexGroup<F> group) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("addGroup(" + group + ") called");
        if (groups.get(group.getName()) != null) {
            log.warn("A Group with name '" + group.getName() + "' is already present");
            return false;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding " + group);
        groups.put(group.getName(), group);
        log.trace("Adding Fields contained in " + group);
        for (F field : group.getFields()) {
            if (!allFields.containsKey(field.getName())) {
                addField(field);
            }
        }
        return true;
    }

    /**
     * Add a Field to a Group. This is the preferred way of adding a Field to
     * a Group, as it handles the updating of {@link #allFields} as a
     * side-effect. Also: If the Group does not already exist in
     * {@link #groups}, it is added to that list.
     *
     * @param group the field will be added to this group.
     * @param field this will be added to the group.
     */
    public synchronized void addFieldToGroup(IndexGroup<F> group, F field) {
        group.addField(field);
        if (!allFields.containsKey(field.getName())) {
            addField(field);
        }
        if (!groups.containsKey(group.getName())) {
            addGroup(group);
        }
    }

    /**
     * Wrapper for {@link #addFieldToGroup(IndexGroup, IndexField)}. If the
     * Group does not already exist, it is created first.
     *
     * @param groupName the field will be added to this group, which will be
     *                  created if it is not already present.
     * @param field     this will be added to the group.
     */
    public synchronized void addFieldToGroup(String groupName, F field) {
        IndexGroup<F> group = groups.get(groupName);
        if (group == null) {
            group = new IndexGroup<>(groupName);
        }
        addFieldToGroup(group, field);
    }

    /**
     * Locates and returns a field from the internal list of all fields.
     * The look-up is performed with alias-expansion with language null (all
     * languages match).
     *
     * @param fieldName the name or alias of the wanted field.
     * @return the field corresponding to the name or alias or null if a field could not be found.
     * @see #allFields
     */
    @Override
    public F getField(String fieldName) {
        return getField(fieldName, null);
    }

    /**
     * Locates and returns a field from the internal list of all fields.
     * The look-up is performed with alias-expansion. Name-matches takes precedence over alias-matches.
     *
     * @param fieldName the name or alias of the wanted field.
     * @param language  the language for alias-lookup. If the language is null, it is ignored.
     * @return the field corresponding to the name or alias or null if a field could not be found.
     * @see #allFields
     */
    public F getField(String fieldName, String language) {
        F field = allFields.get(fieldName);
        if (field != null) {
            return field;
        }
        // TODO: Optimize alias-lookup

        // Alias lookup
        for (Map.Entry<String, F> entry : allFields.entrySet()) {
            if (entry.getValue().isMatch(fieldName, language)) {
                return entry.getValue();
            }
        }
        // Prefix lookup
        for (F prefixField: prefixFields) {
            if (fieldName.startsWith(prefixField.getName())) {
                return prefixField;
            }
        }

        log.debug("getField: No field with name '" + fieldName + "' found. Returning null");
        return null;
    }

    /**
     * Returns the field where the name (no alias-lookup) matches the fieldName.
     * If no field can be found, {@link #defaultField} is returned.
     *
     * @param fieldName the name of the field to get.
     * @return a field corresponding to the name or the default field.
     */
    public F getFieldForIndexing(String fieldName) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("getFieldForIndexing(" + fieldName + ") called");
        F field = allFields.get(fieldName);
        if (field != null) {
            return field;
        }
        // Prefix lookup
        for (F prefixField: prefixFields) {
            if (fieldName.startsWith(prefixField.getName())) {
                return prefixField;
            }
        }
        log.debug("No field with name '" + fieldName + "' found. Returning default field");
        return defaultField;
    }

    /* Mutators */

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public void setDefaultFields(List<String> defaultFields) {
        this.defaultFields = defaultFields;
    }

    public void setDefaultOperator(OPERATOR defaultOperator) {
        this.defaultOperator = defaultOperator;
    }

    public List<String> getDefaultFields() {
        return defaultFields;
    }

    public OPERATOR getDefaultOperator() {
        return defaultOperator;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public Map<String, IndexGroup<F>> getGroups() {
        return groups;
    }

    /**
     * Locates and returns a group from the internal list of groups.
     * The look-up is performed with alias-expansion and language null.
     *
     * @param groupName the name or alias of the wanted group.
     * @return the group corresponding to the name or alias or null if no group
     *         could be found.
     * @see #groups
     */
    public IndexGroup<F> getGroup(String groupName) {
        return getGroup(groupName, null);
    }

    /**
     * Locates and returns a group from the internal list of groups.
     * The look-up is performed with alias-expansion. Name-matches takes
     * precedence over alias-matches.
     *
     * @param groupName the name or alias of the wanted group.
     * @param language  the language for alias-lookup. If the language is null,
     *                  it is ignored.
     * @return the group corresponding to the name or alias or null if no group
     *         could be found.
     * @see #groups
     */
    public IndexGroup<F> getGroup(String groupName, String language) {
        IndexGroup<F> group = groups.get(groupName);
        if (group != null) {
            return group;
        }
        // TODO: Optimize alias-lookup
        for (Map.Entry<String, IndexGroup<F>> entry : groups.entrySet()) {
            if (entry.getValue().isMatch(groupName, language)) {
                return entry.getValue();
            }
        }
        if (log.isTraceEnabled()) {
            log.trace(String.format(Locale.ROOT, "Could not locate a group based on name '%s' and language '%s'",
                                    groupName, language));
        }
        return null;
    }

    public Map<String, F> getFields() {
        return allFields;
    }

    public List<F> getPrefixFields() {
        return prefixFields;
    }

    public Map<String, F> getPrefixFieldsAsMap() {
        Map<String, F> fields = new HashMap<>(prefixFields.size());
        for (F prefixField: prefixFields) {
            fields.put(prefixField.getName(), prefixField);
        }
        return fields;
    }

    /**
     * If the sub-configuration {@link #CONF_DESCRIPTOR} is present in the
     * given conf, it is copied to all the subConfs. If the CONF_DESCRIPTOR is
     * not present, nothing is done.
     *
     * @param conf     the configuration that might contain IndexDescriptor
     *                 setup.
     * @param subConfs the configurations to copy the setup to, if present.
     */
    public static void copySetupToSubConfigurations(Configuration conf, List<Configuration> subConfs) {
        if (!conf.valueExists(CONF_DESCRIPTOR)) {
            log.debug(String.format(Locale.ROOT, "No %s found in configuration. No copy of the index "
                                    + "descriptor setup is done", CONF_DESCRIPTOR));
            return;
        }
        log.debug("IndexDescriptor setup found. Copying to sub configurations");
        Configuration id;
        try {
            id = conf.getSubConfiguration(CONF_DESCRIPTOR);
        } catch (SubConfigurationsNotSupportedException e) {
            throw new ConfigurationException("Storage doesn't support sub configurations", e);
        } catch (NullPointerException e) {
            throw new ConfigurationException(String.format(Locale.ROOT, "Unable to extract %s from configuration",
                                                           CONF_DESCRIPTOR), e);
        }
        for (Configuration subConf : subConfs) {
            if (subConf.valueExists(CONF_DESCRIPTOR)) {
                log.debug("Skipping assignment og index descriptor setup for subConf as it is already present");
                continue;
            }
            try {
                subConf.createSubConfiguration(CONF_DESCRIPTOR).importConfiguration(id);
            } catch (IOException e) {
                throw new ConfigurationException("Unable to insert index description in sub configuration", e);
            }
        }
    }

    @Override
    public String toString() {
        return "IndexDescriptor(absoluteLocation=" + absoluteLocation
               + ", listener:" + (listener == null ? "none" : "present")
               + ", defaultFields=" + Strings.join(defaultFields, ", ")
                + ", defaultOperator=" + defaultOperator + ", #fields=" + allFields.size()
                + ", #groups=" + groups.size() + ")";
    }
}
