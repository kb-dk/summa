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
package dk.statsbiblioteket.summa.ingest.stream;

import java.io.IOException;
import java.util.NoSuchElementException;

import dk.statsbiblioteket.summa.common.Record;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Filter;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilter;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Uses a streaming XML-parser to split the given streams into {@link Record}s.
 * </p><p>
 * The input should be a valid XML-document where the individual records are
 * listed sequentially.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLSplitterFilter implements ObjectFilter {
    private static Log log = LogFactory.getLog(XMLSplitterFilter.class);

    /**
     * The prefix to prepend to any calcuopenilated Record-ids.
     * </p><p>
     * Default: "".
     */
    public static final String CONF_ID_PREFIX =
            "summa.ingest.xmlsplitter.id_prefix";

    /**
     * If true, extracted ids which beginning matches {@link #CONF_ID_PREFIX}
     * will not have the prefix prepended.
     * </p><p>
     * Example: Prefix is "foo:" and collapse is true:<br />
     *          "barbarella"   => "foo:barbarella"<br />
     *          "foo:fighters" => "foo:fighters"<br />
     *          Prefix is "foo:" and collapse is false:<br />
     *          "barbarella"   => "foo:barbarella"<br />
     *          "foo:fighters" => "foo:foo:fighters"<br />
     * </p><p>
     * Default: "true".
     */
    public static final String CONF_COLLAPSE_PREFIX =
            "summa.ingest.xmlsplitter.collapse_profix";


    /**
     * The xml-element containing the individual records. The input stream will
     * be processed sequentially and all elements matching this value will be
     * regarded as a record.
     * </p><p>
     * Default: "record".
     */
    public static final String CONF_RECORD_ELEMENT =
            "summa.ingest.xmlsplitter.record_element";

    /**
     * The id-element containing the id for a given record. The element must be
     * present inside the record-element. If a tag inside an element is used for
     * id, the syntag is element#tag.
     * </p><p>
     * Example 1: "foo" matches <foo>myid</foo> and returns myid.<br />
     * Example 2: "foo#bar" matches <foo bar="myid">whatever</foo> and returns
     *             myid.<br />
     * </p><p>
     * The element can be qualified or non-qualified. Non-qualified ids will
     * match qualified documents, but not the other way around.
     * </p><p>
     * Example 3: "foo" matches <baz:foo>myid</baz:foo> and returns myid. 
     * Example 4: "baz:foo" does not match <foo>myid</foo>.
     * </p><p>
     * Default: "id".
     */
    public static final String CONF_ID_ELEMENT =
            "summa.ingest.xmlsplitter.id_element";

    /**
     * The base for the constructed Records.
     * </p><p>
     * This option is mandatory.
     */
    public static final String CONF_BASE = "summa.ingest.xmlsplitter.base";

    /**
     * If true, name space definitions are copied to the Record XML blocks.
     * If false, name spaces are stripped.
     * </p><p>
     * Default: "true".
     */
    public static final String CONF_PRESERVE_NAMESPACES =
            "summa.ingest.xmlsplitter.preserve_namespaces";

    /**
     * If true, the XML-blocks must be valid. That means, other than being
     * well-formed, that at least one DTD must be specified and that the XML
     * must conform to those.
     * </p><p>
     * If CONF_REQUIRE_VALID is true, a fitting value for VALID_XML will be
     * added to the meta-info for the generated Records. If CONF_REQUIRE_VALID
     * is false, no value will be added to meta-info.
     * </p><p>
     * Default: "false".
     */
    public static final String CONF_REQUIRE_VALID =
            "summa.ingest.xmlsplitter.require_valid";

    // TODO: Properties with default namespaces?

    /**
     * The key for meta-info in Record. Valid values are "true" and "false".
     */
    public static final String VALID_XML = "valid_xml";

    /**
     * Contains all target-specific settings.
     */
    public static class Target {
        public String idPrefix = "";
        public boolean collapsePrefix = true;
        @SuppressWarnings({"DuplicateStringLiteralInspection"})
        public String recordElement = "record";
        public String idElement ="id";
        public String idTag = "";
        public String base;
        public boolean preserveNamespaces = true;
        public boolean requireValid = false;

        /**
         * Create a new Target containing the target-specific information from
         * configuration.
         * @param configuration setup for the target.
         */
        public Target(Configuration configuration) {
            idPrefix = configuration.getString(CONF_ID_PREFIX, idPrefix);
            collapsePrefix =
                    configuration.getBoolean(CONF_COLLAPSE_PREFIX, collapsePrefix);
            recordElement =
                    configuration.getString(CONF_RECORD_ELEMENT, recordElement);
            idElement = configuration.getString(CONF_ID_ELEMENT, idElement);
            if (idElement.contains("#")) {
                if (!idElement.endsWith("#") || idElement.startsWith("#")) {
                    String oldIdElement = idElement;
                    idTag = idElement.substring(idElement.indexOf("#")+1);
                    idElement = idElement.substring(0, idElement.indexOf("#"));
                    log.debug("split idElement '" + oldIdElement
                              + "' into '" + idElement + "' # '" + idTag + "'");
                } else {
                    log.warn("Suspeciously looking idElement for Target: '"
                             + idElement + "'");
                }
            }
            try {
                base = configuration.getString(CONF_BASE);
                if ("".equals(base)) {
                    throw new ConfigurationException("Base, as defined by "
                                                     + CONF_BASE
                                                     + ", must not be empty");
                }
            } catch (NullPointerException e) {
                //noinspection DuplicateStringLiteralInspection
                throw new ConfigurationException("Could not get " + CONF_BASE
                                                 + " from configuration");
            }
            preserveNamespaces = configuration.getBoolean(CONF_PRESERVE_NAMESPACES,
                                                          preserveNamespaces);
            requireValid =
                    configuration.getBoolean(CONF_REQUIRE_VALID, requireValid);
        }

    }

    private Target target;
    private ObjectFilter source;
    private XMLSplitterParser parser = null;
    private Payload payload = null;

    public XMLSplitterFilter(Configuration configuration) {
        target = new Target(configuration);
        log.info("Created XMLSplitterFilter for base '" + target.base + "'");
    }

    public boolean hasNext() {
        if (source == null) {
            log.warn("No source specified");
            return false;
        }
        if (payload == null) {
            makePayload();
        }
        return payload != null;
    }

    public Payload next() {
        if (source == null) {
            throw new NoSuchElementException("No source specified for "
                                             + "XMLSplitterFilter");
        }
        makePayload();
        Payload newPayload = payload;
        //noinspection AssignmentToNull
        payload = null;
        return newPayload;
    }

    public void remove() {
        log.warn("Remove not supported in XMLSplitterFilter");
    }

    public void setSource(Filter filter) {
        if (parser != null) {
            throw new IllegalStateException("Parser already activated on old"
                                            + " source");
        }
        if (filter instanceof ObjectFilter) {
            source = (ObjectFilter)filter;
        } else {
            throw new IllegalArgumentException("XMLSplitterFilter can only be "
                                               + "chained to ObjectFilters");
        }
    }

    public boolean pump() throws IOException {
        if (!hasNext()) {
            return false;
        }
        Payload next = next();
        if (next == null) {
            return false;
        }
        next.close();
        return true;
    }

    public void close(boolean success) {
        if (source == null) {
            log.warn("Cannot close as no source is specified");
        } else {
            if (parser != null) {
                parser.stopParsing();
            }
            source.close(success);
        }
    }

    /**
     * If {@link #payload} is not already assigned, this method tries to
     * generate the next payload, based on data from the source. If payload
     * is already assigned, this method does nothing.
     * </p><p>
     * Newly created payloads will have a reference to the stream and the
     * meta-info from the source, will have a newly created Record and will
     * have no Document assigned.
     */
    private void makePayload() {
        if (payload != null) {
            log.trace("makePayload: payload already assigned");
            return;
        }
        if (source == null) {
            throw new IllegalStateException("No source defined");
        }
        while (payload == null) {
            if (parser == null) {
                if (!source.hasNext()) {
                    log.debug("Source has no more stream payloads");
                    return;
                }
                log.debug("Creating new parser for target " + target);
                parser = new XMLSplitterParser(target);
                Payload streamPayload = source.next();
                log.debug("Opening stream payload " + payload);
                parser.openPayload(streamPayload);
            }
            if (parser.hasNext()) {
                try {
                    payload = parser.next();
                    return;
                } catch (Exception e) {
                    log.warn("Exception requesting payload from parser, "
                             + "skipping to next stream payload");
                    parser.stopParsing();
                }
            }
            if (source.hasNext()) {
                Payload streamPayload = source.next();
                log.debug("Opening stream payload " + payload);
                parser.openPayload(streamPayload);
            } else {
                log.debug("No more stream payloads available");
                return;
            }
        }
    }

}
