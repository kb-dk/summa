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
package dk.statsbiblioteket.summa.common.unittest;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.io.*;
import java.util.*;
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
     * Note: The arrays are sorted before comparison, so the order of the
     *       elements does not matter.
     * @param message  what to fail with if the arreys are not equal.
     * @param expected the expected result.
     * @param actual   the actuak result.
     */
    public static void assertEquals(
            String message, int[] expected, int[] actual) {
        Arrays.sort(expected);
        if (!Arrays.equals(expected, actual)) {
            //noinspection DuplicateStringLiteralInspection
            fail(message + ". Expected " + dump(expected)
                 + " got " + dump(actual));
        }
    }

    public static void assertEqualsNoSort(
            String message, int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            //noinspection DuplicateStringLiteralInspection
            fail(message + ". Expected " + dump(expected)
                 + " got " + dump(actual));
        }
    }

    /**
     * Compares the two arrays and gives a detailed fail if not completely
     * equal in content as well as order.
     * </p><p>
     * Note: Don't use this for humongous arrays as they are converted to
     *       Strings for output.
     * @param message  what to fail with if the arrays are not equal.
     * @param expected the expected result.
     * @param actual   the actual result.
     */
    public static void assertEquals(
            String message, long[] expected, long[] actual) {
        if (!Arrays.equals(expected, actual)) {
            //noinspection DuplicateStringLiteralInspection
            fail(message + ". Expected " + dump(expected)
                 + " got " + dump(actual));
        }
    }

    /**
     * Compares the given Collections for size, order and content using
     * {@link Object#equals(Object)}.
     * @param message  the message to fail wit if the Collections are not equal.
     * @param expected the expected content.
     * @param actual   the actual content.
     */
    public static void assertEquals(
            String message, Collection expected, Collection actual) {
        if (expected.size() != actual.size()) {
            fail(message + ". Expected size " + expected.size()
                 + " but got " + actual.size());
        }
        Iterator expectedI = expected.iterator();
        Iterator actualI = actual.iterator();
        int counter = 0;
        while (expectedI.hasNext()) {
            Object expectedO = expectedI.next();
            Object actualO = actualI.next();
            if (!expectedO.equals(actualO)) {
                fail(message + ". The objects at position " + counter
                     + " were not equal. Expected '" + expectedO
                     + "', got '" + actualO + "'");
            }
            counter++;
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

    /**
     * Converts the given values to a comma-separated String.
     * @param values the values to dump.
     * @return the values as a String.
     */
    public static String dump(long[] values) {
        StringWriter sw = new StringWriter(values.length * 4);
        sw.append("[");
        for (int i = 0 ; i < values.length ; i++) {
            sw.append(Long.toString(values[i]));
            if (i < values.length - 1) {
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
    public synchronized static void assertValidates(
            String message, URL schema, String xml) {
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

