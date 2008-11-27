/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.ingest.split;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;

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
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public String recordElement = "record";
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
        recordElement = configuration.getString(
                XMLSplitterFilter.CONF_RECORD_ELEMENT, recordElement);
        idElement = configuration.getString(
                XMLSplitterFilter.CONF_ID_ELEMENT, idElement);
        if (idElement.contains("#")) {
            if (!idElement.endsWith("#") || idElement.startsWith("#")) {
                String oldIdElement = idElement;
                idTag = idElement.substring(idElement.indexOf("#")+1);
                idElement = idElement.substring(0, idElement.indexOf("#"));
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
    }
}
