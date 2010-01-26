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
package dk.statsbiblioteket.summa.web.services;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.util.regex.Matcher;

/**
 * SearchWS Tester.
 *
 * @author <Authors name>
 * @since <pre>05/07/2009</pre>
 * @version 1.0
 */
public class SearchWSTest extends TestCase {
    public SearchWSTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(SearchWSTest.class);
    }

    public void testProcessingOptions() throws Exception {
        assertTrue("Simple case should match",
                   SearchWS.PROCESSING_OPTIONS.matcher(
                           "<:test:>foo").matches());
        Matcher matcher = SearchWS.PROCESSING_OPTIONS.matcher("<:test  t:>foo");
        matcher.matches();
        assertEquals("Options should be correct", "test  t", matcher.group(1));
        assertEquals("The rest should be correct", "foo", matcher.group(2));
    }
}

