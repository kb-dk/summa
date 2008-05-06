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
package dk.statsbiblioteket.summa.common.lucene.index;

import java.io.IOException;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashMap;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.Files;
import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.ResourceListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A description of the layout of the index, such as default fields, groups
 * and analyzers. This is Summa's equivalent of SOLR's schema.xml.
 * </p><p>
 * The IndexDescriptor is needed for Lucene Document building and for query
 * expansion.
 * </p><p>
 * The IndexDescriptor has groups, which schema.xml does not, and is otherwise
 * somewhat simpler. It is envisioned that SOLR's classes for indexing and
 * querying can be used in Summa instead of raw Lucene at some point in time.
 * @see {@url http://wiki.apache.org/solr/SchemaXml}
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexDescriptor implements Configurable {
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

    /**
     * The name of the Field containing unordered freely searchable terms.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String FREETEXT = "freetext";

    public static enum OPERATOR {and, or}

    private ResourceListener listener;
    private URL absoluteLocation;

    /**
     * All Fields mapped from field name => Field object.
     */
    private Map<String, IndexField> allFields =
            new LinkedHashMap<String, IndexField>(20);
    /**
     * All Groups mapped from group name => Group object. All the Fields
     * contained in the groups MUST be present in {@link #allFields}.
     */
    private Map<String, IndexGroup> groups =
            new LinkedHashMap<String, IndexGroup>(20);

    // TODO: Assign this based on XML
    private IndexField defaultField = new IndexField(new IndexDefaults());
    private String defaultLanguage = "en";
    private String uniqueKey = "id";
    private List<String> defaultFields = Arrays.asList(FREETEXT, uniqueKey);
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
        this.absoluteLocation = absoluteLocation;
        fetchStateAndActivateListener(0);
    }

    /**
     * Construct an IndexDescriptor based on the given xml.
     * @param xml an XML-representation of an IndexDescriptor.
     */
    public IndexDescriptor(String xml) {
        log.trace("Creating descriptor based on XML");
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
     * Parses the provided XML and sets the state of this IndexDecriptor
     * accordingly. A parse replaces the previous state completely. If an
     * exception is thrown, the state is guaranteed to be the same as before
     * parse was called. See the class documentation for the format of the XML.
     * @param xml an XML representation of an IndexDescriptor.
     */
    public synchronized void parse(String xml) {
        // TODO: Implement parsing of IndexDescriptor XML
        log.fatal("No parsing of XML yet: " + xml);
    }

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
        for (IndexGroup g: groups.values()){
           sw.append(g.toXMLFragment());
        }
        sw.append("</groups>\n");

        sw.append("<fields>\n");
        for (Map.Entry<String, IndexField> entry : allFields.entrySet()){
            sw.append(entry.getValue().toXMLFragment());
        }
        sw.append("</fields>\n");

        sw.append("<defaultLanguage>").append(defaultLanguage);
        sw.append("</defaultLanguage>\n");

        sw.append("<uniqueKey>").append(uniqueKey).append("</uniqueKey>\n");

        sw.append("<defaultSearchFields>");
        sw.append(Strings.join(defaultFields, " "));
        sw.append("</defaultSearchFields>\n");

        sw.append("<SummaQueryParser defaultOperator=\"");
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
     * @param field the field to add to the descriptor.
     * @return true if the Field was added, else false.
     * @see {@link #allFields}.
     */
    public synchronized boolean addField(IndexField field) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("addField(" + field + ") called");
        if (allFields.get(field.getName()) != null) {
            log.debug("A Field with name '" + field.getName() + "' is already "
                      + "present in allFields");
            return false;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding " + field + " to allFields");
        allFields.put(field.getName(), field);
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
    public synchronized boolean addGroup(IndexGroup group) {
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
        for (IndexField field: group.getFields()) {
            addField(field);
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
    public synchronized void addFieldToGroup(IndexGroup group,
                                             IndexField field) {
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
                                             IndexField field) {
        IndexGroup group = groups.get(groupName);
        if (group == null) {
            group = new IndexGroup(groupName);
        }
        addFieldToGroup(group, field);
    }

    /**
     * Returns the field where the name (no alias-lookup) matches the fieldName.
     * If no field can be found, {@link #defaultField} is returned.
     * @param fieldName the name of the field to get.
     * @return a field corresponding to the name or the default field.
     */
    public IndexField getFieldForIndexing(String fieldName) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("getFieldForIndexing(" + fieldName + ") called");
        IndexField field = allFields.get(fieldName);
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

}
