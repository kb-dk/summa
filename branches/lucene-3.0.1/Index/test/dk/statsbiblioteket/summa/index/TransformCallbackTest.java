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
package dk.statsbiblioteket.summa.index;

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XSLT;
import junit.framework.TestCase;
import org.w3c.dom.Document;

import java.util.Properties;

/**
 *
 */
@SuppressWarnings({"DuplicateStringLiteralInspection"})
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TransformCallbackTest extends TestCase {
//    private static Log log = LogFactory.getLog(TransformCallbackTest.class);

    private static final String XSLTLocationString =
            "data/transformCallback/getLikes.xsl";
    private static final String XMLLocationString =
            "data/transformCallback/callback_input.xml";
    private static final String XMLLocationStringNoName =
            "data/transformCallback/callback_input_nonamespace.xml";

    public void testCallback() throws Exception {
        Properties prop = new Properties();
        prop.put("bundle_global", "globals");
        prop.put("bundle_availability", "availability");
        prop.put("locale", "da");

        System.out.println(XSLT.transform(
                Resolver.getURL(XSLTLocationString),
                Resolver.getUTF8Content(XMLLocationString),
                prop));
    }

    public void testCallbackNoNamespace() throws Exception {
        Properties prop = new Properties();
        prop.put("bundle_global", "globals");
        prop.put("bundle_availability", "availability");
        prop.put("locale", "da");

        System.out.println(XSLT.transform(
                Resolver.getURL(XSLTLocationString),
                Resolver.getUTF8Content(XMLLocationStringNoName),
                prop));
    }

    public void testCallbackDOM() throws Exception {
        Properties prop = new Properties();
        prop.put("bundle_global", "globals");
        prop.put("bundle_availability", "availability");
        prop.put("locale", "da");

        Document dom = DOM.stringToDOM(
                Resolver.getUTF8Content(XMLLocationString));
        System.out.println(XSLT.transform(
                Resolver.getURL(XSLTLocationString), dom, prop));
    }



    public static String getMessage(String bundle, String locale, String key) {
        return String.format(
                "Resolved getMessage to bundle %s, locale %s and key %s",
                bundle, locale, key);
    }
}

