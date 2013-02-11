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

import dk.statsbiblioteket.util.Strings;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;

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
                 + " but got " + actual.size() + "\nValues: "
                 + Strings.join(actual, ", "));
        }
        Iterator expectedI = expected.iterator();
        Iterator actualI = actual.iterator();
        int counter = 0;
        while (expectedI.hasNext()) {
            Object expectedO = expectedI.next();
            Object actualO = actualI.next();
            if (!expectedO.equals(actualO)) {
                String extra = expected.size() < 20 && actual.size() < 20 ?
                               "\nExpected [" + Strings.join(expected, ", ")
                               + "]\nActual   [" + Strings.join(actual, ", ")
                               + "]" : "";


                fail(message + ". The objects at position " + counter
                     + " were not equal. Expected '" + expectedO
                     + "', got '" + actualO + "'" + extra);
            }
            counter++;
        }
    }

    /**
     * Compares list1 with list2 and gives a detailed description of the permutations required to transform list1 to
     * list2.
     * @param message the message to write in case on non-equality.
     * @param list1 primary objects to compare.
     * @param list2 secondary objects to compare.
     */
    public static void assertPermutations(String message, List list1, List list2) {
        assertEquals(message + ". The lists should be of equal length", list1.size(), list2.size());
        boolean mismatch = false;
        for (int i = 0 ; i < list1.size() ; i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                mismatch = true;
                break;
            }
        }
        if (!mismatch) {
            return;
        }
        assertTrue(message + ".The lists should have content but did not", !list1.isEmpty());
        int matches = 0;
        int permutations = 0;
        int perfect = 0;
        for (int i = 0 ; i < list1.size() ; i++) {
            Object o1 = list1.get(i);
            if (list2.contains(o1)) {
                matches++;
                permutations += Math.abs(i - list2.indexOf(o1));
                if (i == list2.indexOf(o1)) {
                    perfect++;
                }
            }
        }
        fail(String.format(message + ". Mismatched lists. Matches: %d/%d (%d at same position), total permutations of "
                           + "matches between lists: %d (average %.1f). Hits were\n%s\n%s",
                           matches, list1.size(), perfect, permutations, permutations * 1.0 / matches,
                           Strings.join(list1, ", "), Strings.join(list2, ", ")));
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
        if (!saxProblems.isEmpty()) {
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

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        saxProblems.add(exception);
    }
    @Override
    public void error(SAXParseException exception) throws SAXException {
        saxProblems.add(exception);
    }
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        saxProblems.add(exception);
    }
}

