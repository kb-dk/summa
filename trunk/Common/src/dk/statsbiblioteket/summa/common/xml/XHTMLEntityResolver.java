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
                    "xhtml1-strict.dtd"},
            {"xhtml-lat1.ent",
                    "xhtml-lat1.ent"},
            {"xhtml-special.ent",
                    "xhtml-special.ent"},
            {"xhtml-symbol.ent",
                    "xhtml-symbol.ent"},
            {"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd",
                    "xhtml1-frameset.dtd"},
            {"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd",
                    "xhtml1-transitional.dtd"}
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

