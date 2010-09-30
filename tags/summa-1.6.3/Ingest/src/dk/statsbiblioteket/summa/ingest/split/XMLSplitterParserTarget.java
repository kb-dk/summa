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
package dk.statsbiblioteket.summa.ingest.split;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * Helper-class for XMLSplitterParser, containing information on how to
 * handle the splitting.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XMLSplitterParserTarget {
    private static Log log = LogFactory.getLog(XMLSplitterParserTarget.class);

    public String idPrefix = "";
    public String idPostfix = "";
    public boolean collapsePrefix = true;
    public boolean collapsePostfix = true;
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public String recordElement = "record";
    public String recordNamespace = null;
    public String idElement ="id";
    public String idTag = "";
    public String idNamespace = null;
    public String base;
    public boolean preserveNamespaces = true;
    public boolean requireValid = false;

    /**
     * Create a new Target containing the target-specific information from
     * configuration.
     * @param configuration setup for the target.
     */
    public XMLSplitterParserTarget(Configuration configuration) {
        idPrefix = configuration.getString(
                XMLSplitterFilter.CONF_ID_PREFIX, idPrefix);
        idPostfix = configuration.getString(
                XMLSplitterFilter.CONF_ID_POSTFIX, idPostfix);
        idNamespace = configuration.getString(
                XMLSplitterFilter.CONF_ID_NAMESPACE, idNamespace);
        collapsePrefix = configuration.getBoolean(
                XMLSplitterFilter.CONF_COLLAPSE_PREFIX, collapsePrefix);
        collapsePostfix = configuration.getBoolean(
                XMLSplitterFilter.CONF_COLLAPSE_POSTFIX, collapsePostfix);
        recordElement = configuration.getString(
                XMLSplitterFilter.CONF_RECORD_ELEMENT, recordElement);
        recordNamespace = configuration.getString(
                XMLSplitterFilter.CONF_RECORD_NAMESPACE, recordNamespace);
        idElement = configuration.getString(
                XMLSplitterFilter.CONF_ID_ELEMENT, idElement);
        if ("".equals(idElement)) {
            log.debug("The idElement is empty. A dummy ID will be assigned to "
                      + "generated Records");
        } else if (idElement.contains("#")) {
            if (!idElement.endsWith("#") || idElement.startsWith("#")) {
                String oldIdElement = idElement;
                idTag = idElement.substring(idElement.indexOf('#')+1);
                idElement = idElement.substring(0, idElement.indexOf('#'));
                log.debug("split idElement '" + oldIdElement
                          + "' into '" + idElement + "' # '" + idTag + "'");
            } else {
                log.warn("Suspiciously looking idElement for Target: '"
                         + idElement + "'");
            }
        }
        try {
            base = configuration.getString(XMLSplitterFilter.CONF_BASE);
            if ("".equals(base)) {
                throw new Configurable.ConfigurationException(String.format(
                        "Base, as defined by %s, must not be empty",
                        XMLSplitterFilter.CONF_BASE));
            }
        } catch (NullPointerException e) {
            //noinspection DuplicateStringLiteralInspection
            throw new Configurable.ConfigurationException(String.format(
                    "Could not get %s from configuration",
                    XMLSplitterFilter.CONF_BASE));
        }
        preserveNamespaces = configuration.getBoolean(
                XMLSplitterFilter.CONF_PRESERVE_NAMESPACES, preserveNamespaces);
        requireValid = configuration.getBoolean(
                XMLSplitterFilter.CONF_REQUIRE_VALID, requireValid);
        log.debug(String.format(
                "Created XMLSplitterParserTaget with idPrefix='%s', " 
                + "idPostfix='%s', idNamespace='%s', collapsePrefix=%b, "
                + "collapsePostfix=%b, recordElement='%s', idElement='%s', "
                + "idTag='%s', base='%s', preserveNamespaces=%b, "
                + "requireValid=%b",
                idPrefix, idPostfix, idNamespace, collapsePrefix,
                collapsePostfix, recordElement, idElement, idTag, base,
                preserveNamespaces, requireValid));
    }

    /**
     * Adjusts the id for the Payload to fit prefix and postfix.
     * @param payload The Payload to adjust the id for.
     */
    public void adjustID(Payload payload) {
        String id = payload.getId();
        if (id == null) {
            if (payload.getRecord() != null) {
                id = payload.getRecord().getId();
            }
            if (id == null) {
                log.warn("Encountered Payload with no ID. Skipping adjustID " 
                         + "for " + payload);
                return;
            }
        }
        String origin_tail = "";
        try {
            origin_tail = payload.getData(Payload.ORIGIN) == null ? "" :
                          new File((String)payload.getData(Payload.ORIGIN)).
                                  getParentFile().getName();
            log.trace("Located expand-folder '" + origin_tail + "'");
        } catch (Exception e) {
            log.debug("Exception while locating expand-folder for " + payload,
                      e);
        }
        String prefix = idPrefix.replace(XMLSplitterFilter.EXPAND_FOLDER,
                                         origin_tail);
        String postfix = idPostfix.replace(XMLSplitterFilter.EXPAND_FOLDER,
                                           origin_tail);
        if (collapsePrefix && id.startsWith(prefix)) {
            prefix = "";
        }
        if (collapsePostfix && id.endsWith(postfix)) {
            postfix = "";
        }
        id = prefix + id + postfix;
        log.trace("Correcting id '" + payload.getId() + "' to '" + id + "'");
        payload.setID(id);
        if (payload.getRecord() != null) {
            payload.getRecord().setId(payload.getId());
        }
    }
}