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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class YearRangeTest extends TestCase {
    public YearRangeTest(String name) {
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
        return new TestSuite(YearRangeTest.class);
    }

    public void testSimpleRange() throws Exception {
        assertEquals("Range from 2000 to 2003 should work",
                     "2000 2001 2002 2003",
                     YearRange.makeRange("2000", "2003"));
    }

    public void testWildcardRange() throws Exception {
        assertEquals("Range 200? should work",
                     "2000 2001 2002 2003 2004 2005 2006 2007 2008 2009", 
                     YearRange.makeRange("200?"));
    }
}

