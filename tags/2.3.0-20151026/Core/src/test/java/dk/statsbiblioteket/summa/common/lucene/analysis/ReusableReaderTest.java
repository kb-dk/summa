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

import dk.statsbiblioteket.util.Strings;
import junit.framework.TestCase;

import java.io.IOException;

public class ReusableReaderTest extends TestCase {

    public void testBasic() throws IOException {
        ReusableReader reader = new ReusableReader("foo");
        assertEquals("Full empty should match", "foo", Strings.flush(reader));
        reader.reset();
        assertEquals("After reset(), full empty should still match", "foo", Strings.flush(reader));
        reader.set("bar");
        assertEquals("After new content", "bar", Strings.flush(reader));
        reader.reset();
        assertEquals("After reset() with new content", "bar", Strings.flush(reader));
    }
}

