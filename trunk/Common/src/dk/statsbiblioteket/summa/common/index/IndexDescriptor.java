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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.File;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.text.ParseException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ResourceListener;
import dk.statsbiblioteket.summa.common.util.ParseUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
 * querying can be used in Summa instead of raw Lucene at some point in time.
 * @see {@url http://wiki.apache.org/solr/SchemaXml}
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class IndexDescriptor<F extends IndexField> implements
                                   Configurable, FieldProvider<F> {
    private static Log log = LogFactory.getLog(IndexDescriptor.class);

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
    public static final String CONF_LOCATION_ROOT =
            "summa.common.index.location-root";
    public static final String CURRENT = "current.txt";

    /**
     * If an absolute location is specified in the configuration, the state of
     * the IndexDescriptor is fetched from there.
     * </p><p>
     * This property is an URL.
     * </p><p>
     * Either this property or {@link #CONF_LOCATION_ROOT} must be present.
     */
    public static final String CONF_ABSOLUTE_LOCATION =
            "summa.common.index.absolute-location";

    /**
     * How often the IndexDescriptor should be re-read from the resolved
     * absolute location, in milliseconds. A value of -1 turns off re-reading.
     * </p><p>
     * This property is optional. Default is
     */
    public static final String CONF_CHECK_INTERVAL =
            "summa.common.index.check-interval";
    public static final int DEFAULT_CHECK_INTERVAL = 5*60*1000; // 5 minutes

    public static enum OPERATOR {and, or}

    private ResourceListener listener;
    private URL absoluteLocation;


    /**
     * All Fields mapped from field name => Field object.
     */
    private Map<String, F> allFields = new LinkedHashMap<String, F>(20);

    /**
     * All Groups mapped from group name => Group object. All the Fields
     * contained in the groups MUST be present in {@link #allFields}.
     */
    private Map<String, IndexGroup<F>> groups =
            new LinkedHashMap<String, IndexGroup<F>>(20);

    // TODO: Assign this based on XML
    private F defaultField = createNewField();
    private String defaultLanguage = "en";
    private String uniqueKey = "id";
    private List<String> defaultFields = Arrays.asList(IndexField.FREETEXT, uniqueKey);
    private OPERATOR defaultOperator = OPERATOR.or;

    /**
     * Extracts a locationRoot or an absoluteLocation from configuration,
     * depending on which of the keys are present, and uses that to load
     * persistent data for the descriptor.
     * </p><p>
     * If both keys are present, absoluteLocation takes precedence.
     * </p><p>
     * @param configuration contains the location of a stored IndexDescriptor.
     * @throws IOException if no persistent data could be loaded and parsed.
     */
    public IndexDescriptor(Configuration configuration) throws IOException {
        String locationRoot = configuration.getString(CONF_LOCATION_ROOT, null);
        String absoluteLocationString =
                configuration.getString(CONF_ABSOLUTE_LOCATION, null);
        if (locationRoot == null && absoluteLocationString == null) {
            //noinspection DuplicateStringLiteralInspection
            throw new ConfigurationException("Either " + CONF_LOCATION_ROOT
                                             + " or " + CONF_ABSOLUTE_LOCATION
                                             + " must be present in the "
                                             + "configuration");
        }
        if (locationRoot != null && absoluteLocationString != null) {
            log.debug("Both " + CONF_LOCATION_ROOT + "(" + locationRoot
                     + ") and " + CONF_ABSOLUTE_LOCATION + "("
                     + absoluteLocationString + ") is defined. "
                     + CONF_ABSOLUTE_LOCATION + " will be used");
        }
        if (absoluteLocation == null) {
            try {
                absoluteLocation = resolveAbsoluteLocation(locationRoot);
            } catch (IOException e) {
                throw new IOException("Exception resolving location root "
                                      + locationRoot + "' to absolute location",
                                      e);
            }
        }
        int checkInterval = configuration.getInt(CONF_CHECK_INTERVAL,
                                                 DEFAULT_CHECK_INTERVAL);
        fetchStateAndActivateListener(checkInterval);
    }

    /**
     * Constructs a new empty IndexDescriptor. This can be used to set up an
     * IndexDescriptor programatically, instead of XML-based.
     */
    public IndexDescriptor() {
        log.debug("Empty descriptor created");
        init();
    }

    /**
     * Constructs an IndexDescriptor based on the stated resource. The resource
     * will only be loaded once.
     * @param absoluteLocation the location of the XML-representation of the
     *                         descriptor.
     * @throws IOException if no persistent data could be loaded and parsed.
     */
    public IndexDescriptor(URL absoluteLocation) throws IOException {
        log.trace("Creating descriptor based on '" + absoluteLocation + "'");
        init();
        this.absoluteLocation = absoluteLocation;
        fetchStateAndActivateListener(0);
    }

    /**
     * Construct an IndexDescriptor based on the given xml.
     * @param xml an XML-representation of an IndexDescriptor.
     * @throws ParseException if the xml could not be parsed peoperly.
     */
    public IndexDescriptor(String xml) throws ParseException {
        log.trace("Creating descriptor based on XML");
        init();
        parse(xml);
        log.debug("Descriptor created based on XML");
    }

    private void fetchStateAndActivateListener(
            final int checkInterval) throws IOException {
        listener =
                new ResourceListener(absoluteLocation, checkInterval, false) {

                    public void resourceChanged(String newContent) throws
                                                                   Exception {
                        parse(newContent);
                    }
                };
        if (!listener.performCheck()) {
            throw new IOException("Could not load description from '"
                                  + absoluteLocation + "'",
                                  listener.getLastException());
        }
        if (checkInterval >= 0) {
            listener.setActive(true);
        }
    }

    private static URL resolveAbsoluteLocation(String locationRoot) throws
                                                                   IOException {
        log.debug("Resolving " + CURRENT + " in location root '"
                  + locationRoot + "'");
        String indirection = locationRoot + CURRENT;
        URL url = new URL(indirection);
        String content = ResourceListener.getUTF8Content(url);
        String tokens[] = content.split("\n");
        URL absoluteLocation = new URL(tokens[0].trim());
        log.debug("fetchDescription: Got absoluteLocation '"
                  + absoluteLocation + "' from '" + indirection + "'");
        return absoluteLocation;
    }

    /**
     * The init-method will be called before any IndexDescriptor-specific
     * initialization takes place. Override this method to provide
     * implementation-specific initialization, such as creating default
     * field definitions.
     * </p><p>
     * Note: Implementations should ensure that the fields "freetext" and
     * "summa_default" are created. Freetext will be stored in the field
     * "freetext" and "summa_default" will be used as the base default if
     * no parent is given in the field Node.
     */
    public void init() {
        log.trace("init for IndexDescriptor called - does nothing");
    }

    /**
     * Parses the provided XML and sets the state of this IndexDecriptor
     * accordingly. A parse replaces the previous state completely. If an
     * exception is thrown, the state is guaranteed to be the same as before
     * parse was called. See the class documentation for the format of the XML.
     * @param xml an XML representation of an IndexDescriptor.
     * @throws ParseException if there was an error parsing the xml.
     */
    public synchronized void parse(String xml) throws ParseException {
        //noinspection DuplicateStringLiteralInspection
        log.trace("parse called");

        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        builderFactory.setValidating(false);
        DocumentBuilder builder;
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw (ParseException)new ParseException(
                    "Could not create document builder", -1).initCause(e);
        }
        Document document;
        try {
            document = builder.parse(new InputSource(
                    new ByteArrayInputStream(xml.getBytes("utf-8"))));
        } catch (SAXException e) {
            throw (ParseException) new ParseException(
                    "Could not create Document from xml '"
                    + xml + "'", -1).initCause(e);
        } catch (UnsupportedEncodingException e) {
            //noinspection DuplicateStringLiteralInspection
            throw (ParseException) new ParseException(
                    "utf-8 not supported", -1).initCause(e);
        } catch (IOException e) {
            throw (ParseException) new ParseException(
                    "Could not create ByteArrayInputStream from xml '"
                    + xml + "'", -1).initCause(e);
        }

        defaultLanguage = ParseUtil.getValue(xPath, document,
                                         "IndexDescriptor/defaultLanguage",
                                         defaultLanguage);

        uniqueKey = ParseUtil.getValue(xPath, document,
                                   "IndexDescriptor/uniqueKey",
                                   uniqueKey);
        if ("".equals(uniqueKey)) {
            throw new ParseException("Unique key specified to empty string",
                                     -1);
        }

        String defaultSearchFields =
                ParseUtil.getValue(xPath, document,
                               "IndexDescriptor/defaultSearchFields",
                               Strings.join(defaultFields, " "));
        defaultFields = Arrays.asList(defaultSearchFields.trim().split(" +"));
        if (defaultFields.size() == 0) {
            log.warn("No default fields specified");
        }

        String dop = ParseUtil.getValue(xPath, document,
                "IndexDescriptor/QueryParser/@defaultOperator",
                defaultOperator.toString());
        if ("or".equals(dop.toLowerCase())) {
            defaultOperator = OPERATOR.or;
        } else if ("and".equals(dop.toLowerCase())) {
            defaultOperator = OPERATOR.and;
        } else {
            log.warn("Unexpected value '" + dop
                     + "' found in QueryParser#defaultOperator");
        }


        NodeList fieldNodes;
        final String FIELD_EXPR = "/IndexDescriptor/fields/field";
        try {
            fieldNodes = (NodeList)xPath.evaluate(FIELD_EXPR, document,
                                                 XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new ParseException(String.format(
                    "Expression '%s' for selecting fields was invalid",
                    FIELD_EXPR), -1);
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Located " + fieldNodes.getLength() + " field nodes");
        for (int i = 0 ; i < fieldNodes.getLength(); i++) {
            addField(createNewField(fieldNodes.item(i)));
        }

        NodeList groupNodes;
        //noinspection DuplicateStringLiteralInspection
        final String GROUP_EXPR = "/IndexDescriptor/groups/group";
        try {
            groupNodes = (NodeList)xPath.evaluate(GROUP_EXPR, document,
                                                  XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new ParseException(String.format(
                    "Expression '%s' for selecting groups was invalid",
                    GROUP_EXPR), -1);
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Located " + groupNodes.getLength() + " group nodes");
        for (int i = 0 ; i < groupNodes.getLength(); i++) {
            addGroup(new IndexGroup<F>(groupNodes.item(i), this));
        }

        // Sanity check
        for (String defaultField: defaultFields) {
            if (allFields.get(defaultField) == null
                && groups.get(defaultField) == null) {
                log.warn("The specified default field '" + defaultField
                         + "' did not have any corresponding field or group");
            }
        }
    }

    private XPath xPath = XPathFactory.newInstance().newXPath();

    /**
     * Stores an XML representation of this IndexDescriptor to the given
     * location. See the class documentation for the format of the XML.
     * @param location where to store the XML representation.
     * @throws IOException if the representation could not be stored.
     */
    public synchronized void save(File location) throws IOException {
        log.debug("Storing descriptor to '" + location + "'");
        Files.saveString(toXML(), location);
    }

    /**
     * XML-representation usable for persistence. This is the format that
     * {@link #parse} accepts.
     * @return a well-formed XML representation of the descriptor.
     * @see {@link #parse(String)}.
     */
    public String toXML() {
        StringWriter sw = new StringWriter(10000);
        sw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        sw.append("<IndexDescriptor version=\"1.0\">\n");

        sw.append("<groups>\n");
        for (IndexGroup<F> g: groups.values()){
           sw.append(g.toXMLFragment());
        }
        sw.append("</groups>\n");

        sw.append("<fields>\n");
        for (Map.Entry<String, F> entry : allFields.entrySet()){
            sw.append(entry.getValue().toXMLFragment());
        }
        sw.append("</fields>\n");

        sw.append("<defaultLanguage>").append(defaultLanguage);
        sw.append("</defaultLanguage>\n");

        sw.append("<uniqueKey>").append(uniqueKey).append("</uniqueKey>\n");

        sw.append("<defaultSearchFields>");
        sw.append(Strings.join(defaultFields, " "));
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
     *       added, it will be the new {@link #defaultField}.
     * @param field the field to add to the descriptor.
     * @return true if the Field was added, else false.
     * @see {@link #allFields}.
     */
    public synchronized boolean addField(F field) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("addField(" + field + ") called");
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

    /**
     * Adds the given IndexGroup to the descriptor, if the group is not already
     * present. Presence is defined by the existence of a Group with the same
     * name.
     * </p><p>
     * Note: Adding a Group automatically adds all contained Fields to
     *       {@link #allFields}.
     * @param group the group to add to the descriptor.
     * @return true if the Group was added, else false.
     * @see {@link #groups}.
     */
    public synchronized boolean addGroup(IndexGroup<F> group) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("addGroup(" + group + ") called");
        if (groups.get(group.getName()) != null) {
            log.warn("A Group with name '" + group.getName()
                     + "' is already present");
            return false;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding " + group);
        groups.put(group.getName(), group);
        log.trace("Adding Fields contained in " + group);
        for (F field: group.getFields()) {
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
     * @param group the field will be added to this group.
     * @param field this will be added to the group.
     */
    public synchronized void addFieldToGroup(IndexGroup<F> group,
                                             F field) {
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
     * @param groupName the field will be added to this group, which will be
     *                  created if it is not already present.
     * @param field     this will be added to the group.
     */
    public synchronized void addFieldToGroup(String groupName,
                                             F field) {
        IndexGroup<F> group = groups.get(groupName);
        if (group == null) {
            group = new IndexGroup<F>(groupName);
        }
        addFieldToGroup(group, field);
    }

    /**
     * Locates and returns a field from the internal list of all fields.
     * The look-up is performed with alias-expansion with language null (all
     * languages match).
     * @param fieldName the name or alias of the wanted field.
     * @return the field corresponding to the name or alias.
     * @throws IllegalArgumentException if the field could not be located.
     * @see {@link #allFields}.
     */
    public F getField(String fieldName) throws IllegalArgumentException {
        return getField(fieldName, null);
    }

    /**
     * Locates and returns a field from the internal list of all fields.
     * The look-up is performed with alias-expansion. Name-matches takes
     * precedence over alias-matches.
     * @param fieldName the name or alias of the wanted field.
     * @param language  the language for alias-lookup. If the language is null,
     *                  it is ignored.
     * @return the field corresponding to the name or alias.
     * @throws IllegalArgumentException if the field could not be located.
     * @see {@link #allFields}.
     */
    public F getField(String fieldName, String language) throws
                                                      IllegalArgumentException {
        F field = allFields.get(fieldName);
        if (field != null) {
            return field;
        }
        // TODO: Optimize alias-lookup
        for (Map.Entry<String, F> entry: allFields.entrySet()) {
            if (entry.getValue().isMatch(fieldName, language)) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException(String.format(
                "Could not locate a field based on name '%s' and language "
                + "'%s'", fieldName, language));
    }
    /**
     * Returns the field where the name (no alias-lookup) matches the fieldName.
     * If no field can be found, {@link #defaultField} is returned.
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
        log.debug("No field with name '" + fieldName
                  + "' found. Returning default field");
        return defaultField;
    }

    /* Mutators */

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public void setDefaultFields(List<String> defaultFields) {
        this.defaultFields = defaultFields;
    }

    public void setDefaultOperator(OPERATOR defaultOperator) {
        this.defaultOperator = defaultOperator;
    }

    public String getUniqueKey() {
        return uniqueKey;
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
     * @param groupName the name or alias of the wanted group.
     * @return the group corresponding to the name or alias.
     * @throws IllegalArgumentException if the group could not be located.
     * @see {@link #groups}.
     */
    public IndexGroup<F> getGroup(String groupName) throws
                                                      IllegalArgumentException {
        return getGroup(groupName, null);
    }

    /**
     * Locates and returns a group from the internal list of groups.
     * The look-up is performed with alias-expansion. Name-matches takes
     * precedence over alias-matches.
     * @param groupName the name or alias of the wanted group.
     * @param language  the language for alias-lookup. If the language is null,
     *                  it is ignored.
     * @return the group corresponding to the name or alias.
     * @throws IllegalArgumentException if the group could not be located.
     * @see {@link #groups}.
     */
    public IndexGroup<F> getGroup(String groupName, String language) throws 
                                                      IllegalArgumentException {
        IndexGroup<F> group = groups.get(groupName);
        if (group != null) {
            return group;
        }
        // TODO: Optimize alias-lookup
        for (Map.Entry<String, IndexGroup<F>> entry: groups.entrySet()) {
            if (entry.getValue().isMatch(groupName, language)) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException(String.format(
                "Could not locate a group based on name '%s' and language "
                + "'%s'", groupName, language));
    }

    public Map<String, F> getFields() {
        return allFields;
    }
}
