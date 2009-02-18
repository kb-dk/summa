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
package dk.statsbiblioteket.summa.common.unittest;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;

import junit.framework.TestCase;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;

@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ExtraAsserts extends TestCase implements ErrorHandler {
//    private static Log log = LogFactory.getLog(ExtraAsserts.class);

    /**
     * Compares the two arrays and gives a detailed fail if not equal.
     * </p><p>
     * Note: Don't use this for humongous arrays as they are converted to
     *       Strings for output.
     * @param message  what to fail with if the arreys are not equal.
     * @param expected the expected result.
     * @param actual   the actuak result.
     */
    public static void assertEquals(String message, int[] expected,
                                    int[] actual) {
        Arrays.sort(expected);
        if (!Arrays.equals(expected, actual)) {
            //noinspection DuplicateStringLiteralInspection
            fail(message + ". Expected " + dump(expected)
                 + " got " + dump(actual));
        }
    }

    /**
     * Converts the given ints to a comma-separated String.
     * @param ints the integers to dump.
     * @return the integers as a String.
     */
    public static String dump(int[] ints) {
        StringWriter sw = new StringWriter(ints.length * 4);
        sw.append("[");
        for (int i = 0 ; i < ints.length ; i++) {
            sw.append(Integer.toString(ints[i]));
            if (i < ints.length - 1) {
                sw.append(", ");
            }
        }
        sw.append("]");
        return sw.toString();
    }

    private static final String JAXP_SCHEMA_LANGUAGE =
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String W3C_XML_SCHEMA =
            "http://www.w3.org/2001/XMLSchema";
    private static final String JAXP_SCHEMA_SOURCE =
            "http://java.sun.com/xml/jaxp/properties/schemaSource";

    // Expects W3C Schema
    public synchronized static void assertValidates(String message, URL schema,
                                                    String xml) {
        saxProblems.clear();
        assertNotNull(message + ". The schema-URL '" + schema
                      + " 'must be resolvable to a File",
                      schema == null ? null : schema.getFile());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        try {
            factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            factory.setAttribute(JAXP_SCHEMA_SOURCE, schema.getFile());
        }
        catch (IllegalArgumentException x) {
            fail(message + ". This runtime does not support JAXP 1.2");
        }
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ExtraAsserts());
            builder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException e) {
            fail(message + ". ParserConfigurationException " + e.getMessage()
                 + "," + getSAXExceptions());
        } catch (IOException e) {
            fail(message + ". Unable to stream the XML: " + e.getMessage()
                 + "," + getSAXExceptions());
        } catch (SAXException e) {
            fail(message + ". SAXExcaption " + e.getMessage() + "," 
                 + getSAXExceptions());
        }
        if (saxProblems.size() > 0) {
            fail(message + ". Exceptions encountered:" + getSAXExceptions());
        }
    }
    private static final List<SAXParseException> saxProblems =
            new ArrayList<SAXParseException>(100);
    private static String getSAXExceptions() {
        StringWriter sw = new StringWriter(1000);
        for (SAXParseException exception: saxProblems) {
            sw.append(" ").append(exception.getMessage());
        }
        return sw.toString();
    }

    public void warning(SAXParseException exception) throws SAXException {
        saxProblems.add(exception);
    }
    public void error(SAXParseException exception) throws SAXException {
        saxProblems.add(exception);
    }
    public void fatalError(SAXParseException exception) throws SAXException {
        saxProblems.add(exception);
    }
}
