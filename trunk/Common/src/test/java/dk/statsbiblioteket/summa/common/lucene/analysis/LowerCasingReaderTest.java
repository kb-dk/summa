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
package dk.statsbiblioteket.summa.common.lucene.analysis;

import junit.framework.TestCase;

import java.io.StringReader;

import dk.statsbiblioteket.util.Strings;

/**
 *
 */
public class LowerCasingReaderTest extends TestCase {

    LowerCasingReader r;

    public void testEmptyStream() throws Exception {
        r = new LowerCasingReader(new StringReader(""));
        assertEquals("", Strings.flushLocal(r));
    }

    public void testSingleChar() throws Exception {
        r = new LowerCasingReader(new StringReader("a"));
        assertEquals("a", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("A"));
        assertEquals("a", Strings.flushLocal(r));
    }

    public void testVarCase() throws Exception {
        r = new LowerCasingReader(new StringReader("ab"));
        assertEquals("ab", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("aB"));
        assertEquals("ab", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("Ab"));
        assertEquals("ab", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("AB"));
        assertEquals("ab", Strings.flushLocal(r));
    }

    public void testSymbols() throws Exception {
        r = new LowerCasingReader(new StringReader("#"));
        assertEquals("#", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("$"));
        assertEquals("$", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("£"));
        assertEquals("£", Strings.flushLocal(r));

        r = new LowerCasingReader(new StringReader("*"));
        assertEquals("*", Strings.flushLocal(r));
    }

}

