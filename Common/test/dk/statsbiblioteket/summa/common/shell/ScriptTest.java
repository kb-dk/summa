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
package dk.statsbiblioteket.summa.common.shell;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

import java.util.Iterator;

/**
 *
 * @since Aug 18, 2010
 * @author Henrik Kirk <hbk@statsbiblioteket.dk>
 */
@QAInfo(author = "hbk",
        level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT)
public class ScriptTest extends TestCase {
    private static final String[] args = new String[]{"status;", "Test\n", "MoreStatus;"};
    private static final String[] argsEscaped = new String[]{"status", "Test", "MoreStatus"};
    private static final String stringArgs = "status;Test\nMoreStatus";

    Script script1;
    Script script2;
    Script script3;
    @Override
    public void setUp() {
        script1 = new Script(args);
        script2 = new Script(stringArgs);
        script3 = new Script(args, 1);
    }

    @Override
    public void tearDown() {
        
    }

    public void testGetIterator() {

        Iterator<String> iter1 = script1.iterator();
        Iterator<String> iter2 = script2.iterator();
        int i = 0;
        while(iter1.hasNext()) {
            assertTrue(iter2.hasNext());
            assertEquals(iter2.next(), iter1.next());
            i++;
        }
        assertEquals("Size of commands should be", args.length, i);


        Iterator<String> iter3 = script3.iterator();

        for(i=0; iter3.hasNext(); i++) {
            assertEquals("iter3.next() shuold be equal to args[" +(i+1) + "] ",
                         argsEscaped[i+1], iter3.next());
        }
    }

    public void testGetList() {
        assertEquals("Size of script1 should be ", args.length,
                     script1.getStatements().size());
        assertEquals("Size of script2 should be ", args.length,
                     script2.getStatements().size());
        assertEquals("Size of script3 should be ", args.length-1,
                     script3.getStatements().size());
    }
}
