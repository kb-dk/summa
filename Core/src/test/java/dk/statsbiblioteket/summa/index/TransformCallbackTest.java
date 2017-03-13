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
import dk.statsbiblioteket.summa.plugins.SaxonXSLT;
import dk.statsbiblioteket.summa.plugins.YearRange;
import dk.statsbiblioteket.util.xml.DOM;
import junit.framework.TestCase;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import java.util.Properties;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class TransformCallbackTest extends TestCase {
    private static Log log = LogFactory.getLog(TransformCallbackTest.class);

    private static final String XSLTLocationString = "index/transformCallback/getLikes.xsl";
    private static final String XMLLocationString = "index/transformCallback/callback_input.xml";
    private static final String XMLLocationStringNoName = "index/transformCallback/callback_input_nonamespace.xml";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        log.info("Creating Saxon TransformerFactory with custom callback functions");
        net.sf.saxon.Configuration saxonConf = new net.sf.saxon.Configuration();
        saxonConf.registerExtensionFunction(new MessageDefinition());
        SaxonXSLT.setSaxonFactory(new TransformerFactoryImpl(saxonConf));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        SaxonXSLT.setSaxonFactory(null);
    }

    public void testCallback() throws Exception {
        Properties prop = new Properties();
        prop.put("bundle_global", "globals");
        prop.put("bundle_availability", "availability");
        prop.put("locale", "da");

        log.info(SaxonXSLT.transform(
                Resolver.getURL(XSLTLocationString),
                Resolver.getUTF8Content(XMLLocationString),
                prop));
        // No need for assert as it fails if callback does not work
    }

    public void testCallbackNoNamespace() throws Exception {
        Properties prop = new Properties();
        prop.put("bundle_global", "globals");
        prop.put("bundle_availability", "availability");
        prop.put("locale", "da");

        log.info(SaxonXSLT.transform(
                Resolver.getURL(XSLTLocationString),
                Resolver.getUTF8Content(XMLLocationStringNoName),
                prop));
        // No need for assert as it fails if callback does not work
    }

    public void testCallbackDOM() throws Exception {
        Properties prop = new Properties();
        prop.put("bundle_global", "globals");
        prop.put("bundle_availability", "availability");
        prop.put("locale", "da");

        Document dom = DOM.stringToDOM(
                Resolver.getUTF8Content(XMLLocationString));
        log.info(SaxonXSLT.transform(
                Resolver.getURL(XSLTLocationString), dom, prop));
        // No need for assert as it fails if callback does not work
    }

    public static String getMessage(String bundle, String locale, String key) {
        return String.format(
                "Resolved getMessage to bundle %s, locale %s and key %s",
                bundle, locale, key);
    }

private static class MessageDefinition extends ExtensionFunctionDefinition {
    @Override
    public StructuredQName getFunctionQName() {
        // We mimick Xalan's namespace to get direct compatibility
        return new StructuredQName(
                "message", "http://xml.apache.org/xalan/java/dk.statsbiblioteket.summa.index.TransformCallbackTest",
                "getMessage");
//            return new StructuredQName("kbext", "http://kb.dk/saxon-extension", getID());
    }
    @Override
    public int getMinimumNumberOfArguments() {
        return 1;
    }
    @Override
    public int getMaximumNumberOfArguments() {
        return 3;
    }
    @Override
    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_STRING};
    }
    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_STRING;
    }
    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            @Override public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                if (arguments.length != 3) {
                    throw new RuntimeException("Expected 3 arguments, got " + arguments.length);
                }
                return new StringValue(getMessage(arguments[0].head().getStringValue(),
                                                  arguments[1].head().getStringValue(),
                                                  arguments[2].head().getStringValue()));
            }
        };
    }
}

}