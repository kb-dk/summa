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
package dk.statsbiblioteket.summa.index;

import javax.xml.transform.TransformerException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.XSLT;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;

/**
 * Transform arbitrary XML in Payload.Record.content using XSLT. This is
 * normally used to convert source-specific XML to SummaDocumentXML (ready
 * for further transformation to a Lucene Document object by CreateDocument).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.QA_NEEDED,
        author = "te")
public class XMLTransformer extends ObjectFilterImpl {
    private static Log log = LogFactory.getLog(XMLTransformer.class);

    /**
     * The location of the XSLT to use for XML transformation. This is in the
     * form of an URL, typically file: or http: based.
     * </p><p>
     * This property is mandatory.
     */
    public static final String CONF_XSLT = "summa.xmltransformer.xslt";

    /**
     * If true, all namespaces in the XML is stripped prior to transformation.
     * This is not recommended, but in The Real World, there are a lot of XML
     * and XSLT's with garbled namespace matching.
     * </p><p>
     * Note: Using true might have a noticeable impact on processor-load and
     *       temporary object allocation.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_STRIP_XML_NAMESPACES =
            "summa.xmltransformer.ignorexmlnamespaces";
    public static final boolean DEFAULT_STRIP_XML_NAMESPACES = true;

    private URL xsltLocation;
    private boolean stripXMLNamespaces = DEFAULT_STRIP_XML_NAMESPACES;

    /**
     * Sets up the transformer stated in the configuration.
     * @param conf contains the location of the XSLT to use.
     * @see {@link #CONF_XSLT}.
     */
    public XMLTransformer(Configuration conf) {
        super(conf);
        String xsltLocationString = conf.getString(CONF_XSLT, null);
        if (xsltLocationString == null || "".equals(xsltLocationString)) {
            throw new ConfigurationException(String.format(
                    "The property %s must be defined", CONF_XSLT));
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Extracted XSLT location '" + xsltLocationString
                  + "' from properties");
        xsltLocation = Resolver.getURL(xsltLocationString);
        if (xsltLocation == null) {
            throw new ConfigurationException(String.format(
                    "The xsltLocation '%s' could not be resolved to a URL",
                    xsltLocation));
        }
        stripXMLNamespaces = conf.getBoolean(CONF_STRIP_XML_NAMESPACES,
                                             stripXMLNamespaces);
        log.info("XMLTransformer for '" + xsltLocation + "' ready for use. "
                 + "Namespaces will " + (stripXMLNamespaces ? "" : "not ") 
                 + "be stripped from input before transformation");
    }


    /**
     * Transform the content of Record from one XML-block to another, using the
     * [@link #xsltLocation}.
     * @param payload the wrapper containing the Record with the content.
     */
    @Override
    protected void processPayload(Payload payload) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("processPayload(" + payload + ") called for " + this);
        if (payload.getRecord() == null) {
            log.warn("No record defined for " + payload + " in " + this);
            return;
        }
        try {
            payload.getRecord().setRawContent(XSLT.transform(
                    xsltLocation, payload.getRecord().getContent(), null,
                    stripXMLNamespaces).
                    toByteArray());
        } catch (TransformerException e) {
            log.warn("Transformer problems in " + this
                     + ". Discarding payload " + payload, e);
            if (log.isTraceEnabled()) {
                log.trace("Problematic record in " + this + " was:\n"
                          + payload.getRecord().toString(true)
                          + " with content\n"
                          + payload.getRecord().getContentAsUTF8());
            }
        }
    }

    @Override
    public synchronized void close(boolean success) {
        super.close(success);
        log.info("Closing down XMLTransformer for '" + xsltLocation + "'. "
                 + getProcessStats());
    }

    @Override
    public String toString() {
        return "XMLTransformer '" + getName() + "' (" + xsltLocation + ", "
               + stripXMLNamespaces + ")";
    }
}
