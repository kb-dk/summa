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
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.xml.sax.ext.EntityResolver2;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.net.URL;

/**
 * Entity resolver for XHTML 1.0. Handles the named entities defined by the
 * standard (lat1, symbol and special) as well as directly specified unicodes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SummaEntityResolver implements EntityResolver2 {
    private static Log log = LogFactory.getLog(SummaEntityResolver.class);

    /**
     * A map from URIs to local resources in the form of a list of entries of
     * the form "URI localresource".
     * </p><p>
     * Example:
     * {@code http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd strict.dtd}
     * will resolve {@code http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd} to
     * the local resource {@code strict.dtd}.
     * </p><p>
     * This property is optional.
     */
    public static final String CONF_RESOURCE_MAP = "entityresolver.resourcemap";

    private Map<String, String> resources;

    public SummaEntityResolver(Configuration conf) {
        this(conf, null);
    }

    /**
     * Specify the resource map using either Configuration, as described by
     * {@link #CONF_RESOURCE_MAP}, an already created map with resources or a
     * combination of the two.
     * @param conf      Configuration-based resource map.
     *                  If null, this argument is ignored.
     * @param resources already created map with resources.
     *                  If null, this argument is ignored.
     */
    public SummaEntityResolver(
            Configuration conf, Map<String, String> resources) {
        this.resources = new HashMap<String, String>(20);
        if (resources != null) {
            this.resources.putAll(resources);
        }
        if (conf != null && conf.valueExists(CONF_RESOURCE_MAP)) {
            for (String resource: conf.getStrings(CONF_RESOURCE_MAP)) {
                String tokens[] = resource.split(" ");
                if (tokens.length != 2) {
                    throw new IllegalArgumentException(String.format(
                            "Expected two strings separated by space from "
                            + "property %s. Got '%s'",
                            CONF_RESOURCE_MAP, resource));
                }
            }
        }
        log.debug(String.format(
                "Finished creating SummaEntityResolver with %d resources",
                this.resources.size()));
        if (this.resources.size() == 0) {
            log.warn("No resources specified for the SummaEntityResolver");
        }
    }

    public InputSource getExternalSubset(String name, String baseURI)
                                              throws SAXException, IOException {
        //System.out.println("*** " + name + ", " + baseURI);
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace("getExternalSubset(name=" + name + ", baseURI="
                      + baseURI + ")");
        }
        return null; // Just let the caller handle it
    }

    public InputSource resolveEntity(String name, String publicId,
                                     String baseURI, String systemId)
                                              throws SAXException, IOException {
        //System.out.println("***1 " + name + ", " + publicId + ", " + baseURI + ", " + systemId);
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace(
                    "resolveEntity(name=" + name + ", publicId=" + publicId
                    + ", baseURI=" + baseURI + ", systemId=" + systemId + ")");
        }
        String resource = resources.get(systemId);
        if (resource == null) {
            return null;
        }
        URL source = Resolver.getURL(resource);
        if (source == null) {
            throw new FileNotFoundException(String.format(
                    "Unable to get URL for '%s'", resource));
        }
        return new InputSource(source.openStream());
    }

    public InputSource resolveEntity(String publicId, String systemId)
                                              throws SAXException, IOException {
//        System.out.println("***2");
        if (log.isTraceEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.trace(
                    "resolveEntity(publicId=" + publicId
                    + ", systemId=" + systemId + ")");
        }
        return resolveEntity(null, publicId, null, systemId);
    }
}

