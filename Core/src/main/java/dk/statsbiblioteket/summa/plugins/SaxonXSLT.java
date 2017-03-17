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
package dk.statsbiblioteket.summa.plugins;

import dk.statsbiblioteket.util.xml.NamespaceRemover;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.w3c.dom.Document;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Saxon-specific helper methods for XSLT-tranformations.
 */
public class SaxonXSLT {
    private static Log log = LogFactory.getLog(SaxonXSLT.class);
    // http://www.saxonica.com/html/documentation/extensibility/integratedfunctions/ext-full-J.html
    /**
     * Creates a new transformer based on the given XSLTLocation and registers knows extensions (aka Java Callbacks).
     *
     * @param xslt the location of the XSLT.
     * @return a Xalan Transformer based on the given XSLT.
     * @throws javax.xml.transform.TransformerException thrown if a Transformer could not be instantiated.
     *         This is normally due to problems with the {@code xslt} URL
     */
    public static Transformer createTransformer(URL xslt) throws TransformerException {
        log.debug("Creating Saxon Transformer from XSLT '" + xslt + "'");

        InputStream in = null;
        Transformer transformer;
        try {
            if (xslt == null) {
                throw new NullPointerException("xslt URL is null");
            }
            in = xslt.openStream();
            transformer = getTransformerFactory().newTransformer(new StreamSource(in, xslt.toString()));
            transformer.setErrorListener(getErrorListener());
        } catch (TransformerException e) {
            throw new TransformerException(String.format(
                    "Unable to instantiate Transformer, a system configuration error for XSLT at '%s'", xslt), e);
        } catch (MalformedURLException e) {
            throw new TransformerException(String.format(
                    "The URL to the XSLT is not a valid URL: '%s'", xslt), e);
        } catch (IOException e) {
            throw new TransformerException(String.format(
                    "Unable to open the XSLT resource due to IOException '%s'", xslt), e);
        } catch (Exception e) {
            throw new TransformerException(String.format("Unable to open the XSLT resource '%s'", xslt), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.warn("Non-fatal IOException while closing stream to '" + xslt + "'");
            }
        }
        return transformer;
    }

    /**
     * Performs a transformation from Stream to Stream with the transformer.
     * @throws TransformerException if the transformation failed.
     */
    public static void transform(Transformer transformer, InputStream in, OutputStream out)
            throws TransformerException {
        transformer.transform(new StreamSource(in), new StreamResult(out));
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the
     * transformation.
     *
     * @param xslt       the location of the XSLT to use.
     * @param in         the content to transform.
     * @param parameters for the Transformer. The keys must be Strings. If the map is null, it will be ignored.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     */
    public static String transform(URL xslt, String in, Map parameters) throws TransformerException {
        return transform(xslt, in, parameters, false);
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the
     * transformation.
     *
     * @param xslt                the location of the XSLT to use.
     * @param in                  the content to transform.
     * @param ignoreXMLNamespaces if true, namespaces in the input content will be stripped. This is not recommended,
     *                            but a lot of XML and XSLTs does not match namespaces correctly. Setting this to true
     *                            will have an impact on performance.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     */
    public static String transform(URL xslt, String in, boolean ignoreXMLNamespaces) throws TransformerException {
        return transform(xslt, in, null, ignoreXMLNamespaces);
    }



    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt                the location of the XSLT to use.
     * @param in                  the content to transform.
     * @param parameters          for the Transformer. The keys must be Strings.
     *                            If the map is null, it will be ignored.
     * @param ignoreXMLNamespaces if true, namespaces in the input content will
     *                            be stripped. This is not recommended, but a lot of XML and XSLTs
     *                            does not match namespaces correctly. Setting this to true will
     *                            have an impact on performance.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     */
    public static String transform(URL xslt, String in, Map parameters, boolean ignoreXMLNamespaces)
            throws TransformerException {
        StringWriter sw = new StringWriter();
        if (!ignoreXMLNamespaces) {
            transform(xslt, new StringReader(in), sw, parameters);
        } else {
            // More than twice as fast as DOM base NS stripping
            Reader noNamespace = new NamespaceRemover(new StringReader(in));
            transform(getLocalTransformer(xslt, parameters), noNamespace, sw);
        }
        return sw.toString();
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs a transformation from Reader to Writer.
     *
     * @param xslt       the location of the XSLT to use.
     * @param in         input.
     * @param out        output.
     * @param parameters for the Transformer. The keys must be Strings.
     *                   If the map is null, it will be ignored.
     * @throws TransformerException if the transformation failed.
     */
    public static void transform(URL xslt, Reader in, Writer out, Map parameters) throws TransformerException {
        transform(getLocalTransformer(xslt, parameters), in, out);
    }

    /**
     * Performs a transformation from Reader to Writer with the transformer.
     *
     * @param transformer probably retrieved by {@link #getLocalTransformer}.
     * @param in          input.
     * @param out         output.
     * @throws TransformerException if the transformation failed.
     */
    public static void transform(Transformer transformer, Reader in, Writer out) throws TransformerException {
        transformer.transform(new StreamSource(in), new StreamResult(out));
    }


    /**
     * Create or re-use a Transformer for the given xsltLocation.
     * The Transformer is {@link ThreadLocal}, so the method is thread-safe.
     * </p><p>
     * Warning: A list is maintained for all XSLTs so changes to the xslt will
     * not be reflected. Call {@link #clearTransformerCache} to clear
     * the list.
     *
     * @param xslt       the location of the XSLT.
     * @param parameters for the Transformer. The keys must be Strings. If the map is null, it will be ignored.
     * @return a Transformer using the given XSLT.
     * @throws TransformerException if the Transformer could not be constructed.
     */
    public static Transformer getLocalTransformer(URL xslt, Map parameters) throws TransformerException {
        if (xslt == null) {
            throw new NullPointerException("The xslt was null");
        }
        Map<String, Transformer> map = localMapCache.get();
        Transformer transformer = map.get(xslt.toString());
        if (transformer == null) {
            transformer = createTransformer(xslt);
            map.put(xslt.toString(), transformer);
        }
        transformer.clearParameters(); // Is this safe? Any defaults lost?
        if (parameters != null) {
            for (Object entryObject : parameters.entrySet()) {
                Map.Entry entry = (Map.Entry) entryObject;
                transformer.setParameter((String) entry.getKey(), entry.getValue());
            }
        }
        return transformer;
    }
    private static ThreadLocal<Map<String, Transformer>> localMapCache = createLocalMapCache();
    private static ThreadLocal<Map<String, Transformer>> createLocalMapCache() {
        return new ThreadLocal<Map<String, Transformer>>() {
            @Override
            protected Map<String, Transformer> initialValue() {
                return new HashMap<>();
            }
        };
    }
    /**
     * Create or re-use a Transformer for the given xsltLocation.
     * The Transformer is {@link ThreadLocal}, so the method is thread-safe.
     * </p><p>
     * Warning: A list is maintained for all XSLTs so changes to the xslt will
     * not be reflected. Call {@link #clearTransformerCache} to clear
     * the list.
     *
     * @param xslt the location of the XSLT.
     * @return a Transformer using the given XSLT.
     * @throws TransformerException if the Transformor could not be constructed.
     */
    public static Transformer getLocalTransformer(URL xslt) throws TransformerException {
        return getLocalTransformer(xslt, null);
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the transformation.
     *
     * @param xslt       the location of the XSLT to use.
     * @param dom        the content to transform.
     * @param parameters for the Transformer. The keys must be Strings. If the map is null, it will be ignored.
     * @return the transformed content. Note that the correct charset must be
     *         supplied to toString("charset") to get proper String results.
     *         The charset is specified by the XSLT.
     * @throws TransformerException if the transformation failed.
     */
    public static ByteArrayOutputStream transform(URL xslt, Document dom, Map parameters) throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transform(getLocalTransformer(xslt, parameters), dom, out);
        return out;
    }

    /**
     * Requests a cached ThreadLocal Transformer and performs the
     * transformation.
     *
     * @param xslt the location of the XSLT to use.
     * @param in   the content to transform.
     * @return the transformed content.
     * @throws TransformerException if the transformation failed.
     */
    public static String transform(URL xslt, String in) throws TransformerException {
        return transform(xslt, in, null, false);
    }

    /**
     * Performs a transformation from Document to Stream with the transformer.
     *
     * @param transformer probably retrieved by {@link #getLocalTransformer}.
     * @param dom         input.
     * @param out         output.
     * @throws TransformerException if the transformation failed.
     */
    public static void transform(Transformer transformer, Document dom, OutputStream out) throws TransformerException {
        transformer.transform(new DOMSource(dom), new StreamResult(out));
    }

    /**
     * Clears the cache used by {@link #getLocalTransformer(java.net.URL)}.
     * This is safe to call as it only affects performance. Clearing the cache
     * means that changes to underlying XSLTs will be reflected and that any
     * memory allocated for caching is freed.
     * </p><p>
     * Except for special cases, such as a huge number of different XSLTs,
     * the cache should only be cleared when the underlying XSLTs are changed.
     */
    public static void clearTransformerCache() {
        localMapCache = createLocalMapCache();
    }

    private static TransformerFactoryImpl saxonFactory = null;
    private synchronized static TransformerFactoryImpl getTransformerFactory() {
        if (saxonFactory == null) {
            log.info("Creating Saxon TransformerFactory with Summa callback functions");
            net.sf.saxon.Configuration saxonConf = new net.sf.saxon.Configuration();
            saxonConf.registerExtensionFunction(new ISBNDefinition());
            saxonConf.registerExtensionFunction(new DatetimeSolrDefinition());
            saxonConf.registerExtensionFunction(new DatetimeExpandDefinition());
            saxonConf.registerExtensionFunction(new DatetimeExpandDateDefinition());
            saxonConf.registerExtensionFunction(new DatetimeExpandTimeDefinition());
            saxonConf.registerExtensionFunction(new CDataDefinition());
            saxonConf.registerExtensionFunction(new MD5Definition());
            saxonConf.registerExtensionFunction(new NetmusikDefinition());
            saxonConf.registerExtensionFunction(new NormalizeDefinition());
            saxonConf.registerExtensionFunction(new OpenUrlEscapeDefinition());
            saxonConf.registerExtensionFunction(new YearRangeDefinition());
            saxonFactory = new TransformerFactoryImpl(saxonConf);
        }
        return saxonFactory;
    }

    /**
     * Normally not something one would do. Primarily a hack for unit testing.
     */
    public static void setSaxonFactory(TransformerFactoryImpl saxonFactory) {
        SaxonXSLT.saxonFactory = saxonFactory;
    }

    private static ErrorListener ERRORLISTENER;
    private static ErrorListener getErrorListener() {
        if (ERRORLISTENER == null) {
            ERRORLISTENER = new ErrorListener() {
                @Override
                public void warning(TransformerException exception)
                        throws TransformerException {
                    log.debug("A transformer warning occured", exception);
                }

                @Override
                public void error(TransformerException exception)
                        throws TransformerException {
                    throw new TransformerException("A Transformer error occured", exception);
                }

                @Override
                public void fatalError(TransformerException exception)
                        throws TransformerException {
                    throw new TransformerException("A Transformer exception occurred", exception);
                }
            };
        }
        return ERRORLISTENER;
    }

    /* Saxon function definitions below */

    private static abstract class SingleStringDefinition extends ExtensionFunctionDefinition {
        @Override
        public StructuredQName getFunctionQName() {
            // We mimick Xalan's namespace to get direct compatibility
            return new StructuredQName("java", "http://xml.apache.org/xalan/java", getID());
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
                    return new StringValue(transform((arguments[0].head().getStringValue())));
                }
            };
        }

        protected abstract String getID();
        protected abstract String transform(String in);
    }

    private static abstract class DualStringDefinition extends ExtensionFunctionDefinition {
        @Override
        public StructuredQName getFunctionQName() {
            // We mimick Xalan's namespace to get direct compatibility
            return new StructuredQName("java", "http://xml.apache.org/xalan/java", getID());
//            return new StructuredQName("kbext", "http://kb.dk/saxon-extension", getID());
        }
        @Override
        public SequenceType[] getArgumentTypes() {
            return new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING};
        }
        @Override
        public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
            return SequenceType.SINGLE_STRING;
        }
        @Override
        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                    return new StringValue(
                            transform(arguments[0].head().getStringValue(),
                                      arguments[1].head().getStringValue()));
                }
            };
        }

        protected abstract String getID();
        protected abstract String transform(String strA, String strB);
    }

    private static class ISBNDefinition extends SingleStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.ISBN.isbnNorm";
        }
        @Override
        protected String transform(String in) {
            return ISBN.isbnNorm(in);
        }
    }

    private static class DatetimeSolrDefinition extends SingleStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.Datetime.solrDateTime";
        }
        @Override
        protected String transform(String datetime) {
            return Datetime.solrDateTime(datetime);
        }
    }

    private static class DatetimeExpandDefinition extends DualStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.Datetime.dateAndTimeExpand";
        }
        @Override
        protected String transform(String isoDateTime, String locale) {
            return Datetime.dateAndTimeExpand(isoDateTime, locale);
        }
    }

    private static class DatetimeExpandDateDefinition extends DualStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.Datetime.dateExpand";
        }
        @Override
        protected String transform(String isoDateTime, String locale) {
            return Datetime.dateExpand(isoDateTime, locale);
        }
    }

    private static class DatetimeExpandTimeDefinition extends DualStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.Datetime.timeExpand";
        }
        @Override
        protected String transform(String isoDateTime, String locale) {
            return Datetime.timeExpand(isoDateTime, locale);
        }
    }

    private static class CDataDefinition extends SingleStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.CDATASectionKeepEntityEncoding.encode";
        }
        @Override
        protected String transform(String str) {
            return CDATASectionKeepEntityEncoding.encode(str);
        }
    }

    private static class MD5Definition extends SingleStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.MD5.md5sum";
        }
        @Override
        protected String transform(String str) {
            return MD5.md5sum(str);
        }
    }

    private static class NetmusikDefinition extends SingleStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.NetmusikGenre.getGenre";
        }
        @Override
        protected String transform(String genreOrg) {
            return NetmusikGenre.getGenre(genreOrg);
        }
    }

    private static class NormalizeDefinition extends SingleStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.Normalize.normalize";
        }
        @Override
        protected String transform(String str) {
            return Normalize.normalize(str);
        }
    }

    private static class OpenUrlEscapeDefinition extends SingleStringDefinition {
        @Override
        protected String getID() {
            return "dk.statsbiblioteket.summa.plugins.OpenUrlEscape.escape";
        }
        @Override
        protected String transform(String url) {
            return OpenUrlEscape.escape(url);
        }
    }

    private static class YearRangeDefinition extends ExtensionFunctionDefinition {
        @Override
        public StructuredQName getFunctionQName() {
            // We mimick Xalan's namespace to get direct compatibility
            return new StructuredQName("java", "http://xml.apache.org/xalan/java",
                                       "dk.statsbiblioteket.summa.plugins.YearRange.makeRange");
//            return new StructuredQName("kbext", "http://kb.dk/saxon-extension", getID());
        }
        @Override
        public int getMinimumNumberOfArguments() {
            return 1;
        }
        @Override
        public int getMaximumNumberOfArguments() {
            return 2;
        }
        @Override
        public SequenceType[] getArgumentTypes() {
            return new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING};
        }
        @Override
        public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
            return SequenceType.SINGLE_STRING;
        }
        @Override
        public ExtensionFunctionCall makeCallExpression() {
            return new ExtensionFunctionCall() {
                @Override public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                    return arguments.length == 1 ?
                            new StringValue(YearRange.makeRange(arguments[0].head().getStringValue())) :
                            new StringValue(YearRange.makeRange(arguments[0].head().getStringValue(),
                                                                arguments[1].head().getStringValue()));
                }
            };
        }
    }

}
