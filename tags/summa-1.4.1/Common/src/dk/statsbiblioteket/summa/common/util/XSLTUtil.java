/* $Id$
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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.summa.common.configuration.Resolver;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Convenience methods for XSLT-handling.
 * @deprecated use {@link dk.statsbiblioteket.util.xml.XSLT} instead.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class XSLTUtil {
    private static Log log = LogFactory.getLog(XSLTUtil.class);


    /**
     * Create a transformer based on the given XSLTLocation.
     * @param xsltLocation the location of the XSLT.
     * @throws TransformerException thrown if for some reason a Transformer
     *                               could not be instantiated.
     *                               This is normally due to problems with the
     *                               xsltLocation.
     * @return a Transformer based on the given XSLT.
     */
    public static Transformer createTransformer(String xsltLocation) throws
                                                          TransformerException {

        log.debug("Requesting and compiling XSLT from '" + xsltLocation + "'");

        TransformerFactory tfactory = TransformerFactory.newInstance();
        InputStream in = null;
        Transformer transformer;
        try {
            URL url = Resolver.getURL(xsltLocation);
            if (url == null) {
                throw new NullPointerException(String.format(
                        "Unable to resolve '%s' to URL", xsltLocation));
            }
            in = url.openStream();
            transformer = tfactory.newTransformer(
                    new StreamSource(in, url.toString()));
        } catch (MalformedURLException e) {
            throw new TransformerException(String.format(
                    "The URL to the XSLT is not a valid URL: '%s'",
                    xsltLocation), e);
        } catch (IOException e) {
            throw new TransformerException(String.format(
                    "Unable to open the XSLT resource '%s', check the "
                    + "destination", xsltLocation), e);
        } catch (TransformerConfigurationException e) {
            throw new TransformerException(String.format(
                    "Wrongly configured transformer for XSLT at '%s'",
                    xsltLocation), e);
        } catch (TransformerException e) {
            throw new TransformerException(
                    "Unable to instantiate Transformer, a system configuration"
                    + " error?", e);
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
        return transformer;
    }

    /**
     * Transform the given content using the Transformer.transform-method,
     * taking care of all book-keeping.
     * @param transformer the Transformer used for the Transformation.
     * @param content     the content to transform.
     * @return the transformed content.
     * @throws TransformerException if an error happened during transformation.
     */
    public static byte[] transformContent(Transformer transformer,
                                          byte[] content)
                                                   throws TransformerException {
        log.trace("Transforming " + content.length + " bytes");
        long startNano = System.nanoTime();
        StreamResult input = new StreamResult();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        input.setOutputStream(out);
        Source so = new StreamSource(
                new ByteArrayInputStream(content));
        transformer.transform(so, input);
        byte[] output = out.toByteArray();
        if (log.isTraceEnabled()) {
            try {
                log.trace(String.format(
                        "Performed transformation in %s ms.\n*** Input:\n%s\n\n"
                        + "*** Output:\n%s",
                        (System.nanoTime() - startNano)/1000000f,
                        new String(content, "utf-8"),
                        new String(output, "utf-8")));
            } catch (UnsupportedEncodingException e1) {
                //noinspection DuplicateStringLiteralInspection
                throw new TransformerException("utf-8 not supported");
            }
        } else if (log.isDebugEnabled()) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("Transformed " + content.length + " bytes to "
                      + output.length + " bytes in "
                      + (System.nanoTime() - startNano)/1000000f + "ms.");
        }
        return output;
    }

}
