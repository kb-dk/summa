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

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.MalformedURLException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.lucene.index.IndexServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Transform arbitrary XML in Payload.Record.content using XSLT. This is
 * normally used to convert source-specific XML to SummaDocumentXML (ready
 * for further transformation to a Lucene Document object by CreateDocument).
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
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

    private String xsltLocation;
    private Transformer transformer;

    /**
     * Sets up the transformer stated in the configuration.
     * @param conf contains the location of the XSLT to use.
     * @see {@link #CONF_XSLT}.
     */
    public XMLTransformer(Configuration conf) {
        try {
            xsltLocation = conf.getString(CONF_XSLT);
            // Extract XSLT
        } catch (Exception e) {
            //noinspection DuplicateStringLiteralInspection
            throw new ConfigurationException("Could not extract property "
                                             + CONF_XSLT
                                             + " from configuration", e);
        }
        if (xsltLocation == null || "".equals(xsltLocation)) {
            throw new ConfigurationException("Property " + CONF_XSLT
                                             + " is not defined");
        }
        //noinspection DuplicateStringLiteralInspection
        log.debug("Extracted XSLT location '" + xsltLocation
                  + "' from properties");
        try {
            createTransformer(xsltLocation);
        } catch (IndexServiceException e) {
            throw new ConfigurationException("Unable to create transformer", e);
        }
        log.info("Transformer for '" + xsltLocation + "' ready for use");
    }

    /**
     * Create a transformer based on the given XSLTLocation. The transformer
     * will be used for the lifetime of the object or until a new transformer
     * is created by an outside call to createTransformer.
     * @param xsltLocation the location of the XSLY.
     * @throws IndexServiceException thrown if for some reason a Transformer
     *                               could not be instantiated.
     *                               This is normally due to problems with the
     *                               xsltLocation.
     */
    public synchronized void createTransformer(String xsltLocation) throws
                                                         IndexServiceException {

        log.debug("Requesting and compiling XSLT from '" + xsltLocation + "'");

        TransformerFactory tfactory = TransformerFactory.newInstance();
        InputStream in = null;
        try {
            URL url = Resolver.getURL(xsltLocation);
            if (url == null) {
                throw new ConfigurationException(
                        "Unable to resolve '" + xsltLocation + "' to URL");
            }
            in = url.openStream();
            transformer = tfactory.newTransformer(
                    new StreamSource(in, url.toString()));
        } catch (MalformedURLException e) {
            throw new IndexServiceException("The URL to the XSLT is not a "
                                            + "valid URL: " + xsltLocation, e);
        } catch (IOException e) {
            throw new IndexServiceException("Unable to open the XSLT resource, "
                                            + "check the destination: "
                                            + xsltLocation, e);
        } catch (TransformerConfigurationException e) {
            throw new IndexServiceException("Wrongly configured transformer for"
                                            + "XSLT at '" + xsltLocation + "'",
                                            e);
        } catch (TransformerException e) {
            throw new IndexServiceException("Unable to instantiate Transformer,"
                                            + " a system configuration error?",
                                            e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.warn("Non-fatal IOException while closing stream to '"
                         + xsltLocation + "'");
            }
        }
    }


    /**
     * Transform the content of Record from one XML-block to another, using the
     * transformer from {@link #createTransformer(String)}.
     * @param payload the wrapper containing the Record with the content.
     */
    protected synchronized void processPayload(Payload payload) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("processPayload(" + payload + ") called");
        if (payload.getRecord() == null) {
            log.warn("No record defined. Discarding payload " + payload);
            return;
        }
        try {
            StreamResult input = new StreamResult();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            input.setOutputStream(out);
            Source so = new StreamSource(
                    new ByteArrayInputStream(payload.getRecord().getContent()));
            transformer.transform(so, input);
/*            System.out.println("************************************");
            System.out.println(payload.getRecord().getContentAsUTF8());*/
            payload.getRecord().setContent(out.toByteArray());
/*            System.out.println("------------------------------------");
            System.out.println(payload.getRecord().getContentAsUTF8());
            System.out.println("************************************");*/
            if (log.isTraceEnabled()) {
                try {
                    log.trace(String.format(
                        "Transformed %s using XSLT from %s\n***** Source *****"
                            + "\n%s\n***** Result *****\n%s",
                            payload, xsltLocation,
                            payload.getRecord().getContentAsUTF8(),
                            new String(out.toByteArray(), "utf-8")));
                } catch (UnsupportedEncodingException e) {
                    log.error("Unable to convert byte-array to UTF-8", e);
                }
            }
/*            if (log.isTraceEnabled()) {
                log.trace("Transformed content for " + payload + ": "
                          + payload.getRecord().getContentAsUTF8());
            }*/
        } catch (TransformerException e) {
            log.warn("Transformer problems. Discarding payload " + payload, e);
        }
    }

    public synchronized void close(boolean success) {
        super.close(success);
        log.info("Closing down XMLTransformer for '" + xsltLocation + "'. "
                 + getProcessStats());
    }
}



