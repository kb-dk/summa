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
import javax.xml.transform.TransformerException;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.filter.object.ObjectFilterImpl;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.util.XSLTUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    private String xsltLocation;
    private Transformer transformer;

    /**
     * Sets up the transformer stated in the configuration.
     * @param conf contains the location of the XSLT to use.
     * @see {@link #CONF_XSLT}.
     */
    public XMLTransformer(Configuration conf) {
        super(conf);
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
            transformer = XSLTUtil.createTransformer(xsltLocation);
        } catch (TransformerException e) {
            throw new ConfigurationException("Unable to create transformer", e);
        }
        log.info("Transformer for '" + xsltLocation + "' ready for use");
    }


    /**
     * Transform the content of Record from one XML-block to another, using the
     * transformer from {@link dk.statsbiblioteket.summa.common.util.XSLTUtil#createTransformer(String)}.
     * @param payload the wrapper containing the Record with the content.
     */
    @Override
    protected synchronized void processPayload(Payload payload) {
        //noinspection DuplicateStringLiteralInspection
        log.trace("processPayload(" + payload + ") called");
        if (payload.getRecord() == null) {
            log.warn("No record defined. Discarding payload " + payload);
            return;
        }
        try {
            payload.getRecord().setRawContent(XSLTUtil.transformContent(
                    transformer, payload.getRecord().getContent()));
        } catch (TransformerException e) {
            log.warn("Transformer problems. Discarding payload " + payload, e);
            if (log.isTraceEnabled()) {
                log.trace("Problematic record was:\n"
                          + payload.getRecord().toString(true));
            }
        }
    }

    @Override
    public synchronized void close(boolean success) {
        super.close(success);
        log.info("Closing down XMLTransformer for '" + xsltLocation + "'. "
                 + getProcessStats());
    }
}



