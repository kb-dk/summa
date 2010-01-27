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
package dk.statsbiblioteket.summa.common.search;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * SimpleResponse Tester.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class SimpleResponseTest extends TestCase {
    public SimpleResponseTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
    // TODO: This should be deprecated. Check before delete
                       /*
    public void testGetErrorID() throws Exception {
        SimpleResponse response = new SimpleResponse();
        response.error(87, "Flam");
        assertTrue("Response should be an error", response.isError());
        assertEquals("Error ID should be 87", 87, response.getErrorID());
    }

    public void testGetErrorMessage() throws Exception {
        SimpleResponse response = new SimpleResponse();
        response.error(87, "Flam");
        assertEquals("Error message should be Flam",
                     "Flam", response.getErrorMessage());
    }

    public void testGetSortKey() throws Exception {
        SimpleResponse response = new SimpleResponse();
        float[] sortValues = new float[2];
        sortValues[0] = 42;
        sortValues[0] = 87;
        response.initiateResponse(sortValues);
        assertEquals("The sort-key should be float",
                     ResponseWriter.PRIMITIVE_COMPARABLE._float,
                     response.getSortKey());
    }

    public void testSortValuesException() throws Exception {
        SimpleResponse response = new SimpleResponse();
        float[] sortValues = new float[2];
        sortValues[0] = 42;
        sortValues[0] = 87;
        response.initiateResponse(sortValues);
        try {
            response.getIntegerSortValues();
            fail("It should not be legal to request integer sortValues when " +
                 "float sortValues was added");
        } catch (IllegalStateException e) {
            // Expected
        }

        assertEquals("The sort-key should be float",
                     ResponseWriter.PRIMITIVE_COMPARABLE._float,
                     response.getSortKey());
    }

    public void testGetFloatSortValues() throws Exception {
        SimpleResponse response = new SimpleResponse();
        float[] sortValues = new float[2];
        sortValues[0] = 42.5f;
        sortValues[1] = 87.8f;
        response.initiateResponse(sortValues);
        assertNotNull("The added sortValues should be something",
                      response.getFloatSortValues());
        assertEquals("The number of sortValues should be ",
                     sortValues.length, response.getFloatSortValues());
        assertEquals("The first sortValue should be ",
                     response.getFloatSortValues()[0]);
    }

    public void testGetStringSortValues() throws Exception {
        SimpleResponse response = new SimpleResponse();
        String[] sortValues = new String[1];
        sortValues[0] = "Hello";
        response.initiateResponse(sortValues);
        assertNotNull("The added sortValues should be something",
                      response.getFloatSortValues());
        assertEquals("The number of sortValues should be ",
                     sortValues.length, response.getStringSortValues());
        assertEquals("The first sortValue should be ",
                     response.getStringSortValues()[0]);
    }

    public void testGetIntegerSortValues() throws Exception {
        SimpleResponse response = new SimpleResponse();
        int[] sortValues = new int[2];
        sortValues[0] = 42;
        sortValues[1] = 87;
        response.initiateResponse(sortValues);
        assertNotNull("The added sortValues should be something",
                      response.getFloatSortValues());
        assertEquals("The number of sortValues should be ",
                     sortValues.length, response.getIntegerSortValues());
        assertEquals("The first sortValue should be ",
                     response.getIntegerSortValues()[0]);
    }

    public void testGetNextContent() throws Exception {
        SimpleResponse response = new SimpleResponse();
        int[] sortValues = new int[2];
        sortValues[0] = 42;
        sortValues[1] = 87;
        response.initiateResponse(sortValues);
        assertFalse("There should be no content yet", response.hasContent());
        try {
            response.getNextContent();
            fail("Getting content prematurely should give an error");
        } catch (IOException e) {
            // Expected
        }
        response.putContent(new byte[234]);
        assertTrue("There should be content now", response.hasContent());
        assertNotNull("getcontent should return content",
                      response.getNextContent());

    }
                         */
    public static Test suite() {
        return new TestSuite(SimpleResponseTest.class);
    }
}




