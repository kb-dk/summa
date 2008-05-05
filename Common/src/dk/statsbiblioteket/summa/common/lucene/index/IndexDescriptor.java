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
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import dk.statsbiblioteket.util.qa.QAInfo;
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
     * The free fields is all fields not explicitely attached to a group.
     */
    private IndexGroup freeFields = new IndexGroup("ungrouped");
    private IndexGroup rootGroup = new IndexGroup("root");
    /**
     * IndexTerms contains query-time boosts based on terms. An example:
     * An index-term with (title, nature, 2.0) is created. A query string is
     * expanded to a Query. The Query is searched recursively for the term
     * matching title, nature. If found, the boost for the query gets multiplied
     * by 2.0.
     */
    private Map<String, IndexTerm> terms = new HashMap<String, IndexTerm>(20);

    private String defaultLanguage = "en";
    private String uniqueKey = "id";
    private List<String> defaultFields = Arrays.asList(FREETEXT);
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

    /**
     * Constructs a new empty IndexDescriptor. This can be used to set up an
     * IndexDescriptor programatically, instead of XML-based.
     */
    public IndexDescriptor() {
        log.debug("Empty descriptor created");
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
        // TODO: Implement parsing og IndexDescriptor XML
    }

    /**
     * Stores an XML representation of this IndexDescriptor to the given
     * location. See the class documentation for the format of the XML.
     * @param location where to store the XML representation.
     * @throws IOException if the representation could not be stored.
     */
    public synchronized void save(File location) throws IOException {
        // TODO: Implement save
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
     * Adds the given IndexField to the descriptors free fields, if the
     * Field-object is not already present and if no existing field matches
     * the name of the new field.
     * @param field the field to add to the descriptor.
     * @return true if the Field was added, else false.
     * @see {@link #freeFields}.
     */
    public boolean addFreeField(IndexField field) {
        if (freeFields.getField(field.getName(), null, false) != null) {
            log.debug("A Field with name '" + field.getName() + "' is already "
                      + "present in freeFields");
            return false;
        }
        //noinspection DuplicateStringLiteralInspection
        log.trace("Adding " + field + " to freeFields");
        freeFields.addField(field);
        return true;
    }

    /**
     * Adds the given IndexTerm to the list of query-time boosting terms.
     * @param term the term to add to the descriptor.
     * @see {@link #terms}.
     */
    public void addTerm(IndexTerm term) {
        terms.put(term.getKey(), term);
    }

    /**
     * See {@link IndexDescriptor#terms} for details on this class.
     */
    public class IndexTerm {
        private String field;
        private String text;

        private float boost;

        public IndexTerm(String field, String text, float boost) {
            log.trace("Creating IndexTerm(" + field + ", " + text + ", " + boost
                      + ")");
            if (field == null) {
                throw new IllegalArgumentException("Field must never be null "
                                                   + "for IndexTerms");
            }
            if (text == null) {
                throw new IllegalArgumentException("Text for IndexTerm for "
                                                   + "Field '" + field + "'"
                                                   + " must never be null ");
            }
            this.field = field;
            this.text = text;
            this.boost = boost;
        }

        public float getBoost() {
            return boost;
        }

        public String getField() {
            return field;
        }

        public String getText() {
            return text;
        }

        public String getKey() {
            return field + ":" + text;
        }

        public String toString() {
            //noinspection DuplicateStringLiteralInspection
            return "IndexTerm(" + field + ", " + text + ", " + boost + ")";
        }
    }
}
