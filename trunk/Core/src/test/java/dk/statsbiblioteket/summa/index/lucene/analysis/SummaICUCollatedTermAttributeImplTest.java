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
package dk.statsbiblioteket.summa.index.lucene.analysis;

import com.ibm.icu.text.Collator;
import dk.statsbiblioteket.summa.common.lucene.analysis.SummaICUCollatedTermAttributeImpl;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.util.BytesRef;

import java.util.Locale;

@SuppressWarnings({"DuplicateStringLiteralInspection"})
public class SummaICUCollatedTermAttributeImplTest extends TestCase {
    private static Log log = LogFactory.getLog(SummaICUCollatedTermAttributeImplTest.class);

    public SummaICUCollatedTermAttributeImplTest(String name) {
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
        return new TestSuite(SummaICUCollatedTermAttributeImplTest.class);
    }

    public void testFillBytesRef() {
        testFillBytesRef("æblegrød");
        testFillBytesRef("Æblegrød");
        testFillBytesRef("omelet");
        testFillBytesRef("");
    }
    public void testFillBytesRef(String input) {
        SummaICUCollatedTermAttributeImpl impl = new SummaICUCollatedTermAttributeImpl(
                Collator.getInstance(new Locale("da")));
        impl.append(input);
        impl.fillBytesRef();
        BytesRef concat = impl.getBytesRef();
        String reverse = SummaICUCollatedTermAttributeImpl.getOriginalString(concat, null).utf8ToString();
        for (int i = 0 ; i < concat.length ; i++) {
            System.out.print((concat.bytes[concat.offset+i]  & 0xFF) + " ");
        }
        System.out.println("(" + reverse + ")");
        assertEquals("The extracted String should match input ", input, reverse);
    }
}
