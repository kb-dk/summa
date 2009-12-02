/* $Id$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.common.xml;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Map;
import java.util.HashMap;

/**
 * Entity resolver for HTML 4.0 and XHTML 1.0. Handles the named entities
 * defined by the standard (lat1, symbol and special).
 * </p><p>
 * This resolver can be extended by specifying the property
 * {@link SummaEntityResolver#CONF_RESOURCE_MAP}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XHTMLEntityResolver extends SummaEntityResolver {
    private static Log log = LogFactory.getLog(XHTMLEntityResolver.class);

    public static final String[][] XHTML_RESOURCES = new String[][] {
            {"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd",
                    "dk/statsbiblioteket/summa/common/xml/xhtml1-strict.dtd"},
            {"xhtml-lat1.ent",
                    "dk/statsbiblioteket/summa/common/xml/xhtml-lat1.ent"},
            {"xhtml-special.ent",
                    "dk/statsbiblioteket/summa/common/xml/xhtml-special.ent"},
            {"xhtml-symbol.ent",
                    "dk/statsbiblioteket/summa/common/xml/xhtml-symbol.ent"},
            {"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd",
                    "dk/statsbiblioteket/summa/common/xml/xhtml1-frameset.dtd"},
            {"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd",
                "dk/statsbiblioteket/summa/common/xml/xhtml1-transitional.dtd"}
    };

    public XHTMLEntityResolver(Configuration conf) {
        super(conf, getXHTMLResources());
    }

    private static Map<String, String> getXHTMLResources() {
        Map<String, String> result =
                new HashMap<String, String>(XHTML_RESOURCES.length);
        for (String[] resource: XHTML_RESOURCES) {
            result.put(resource[0], resource[1]);
        }
        return result;
    }
}
