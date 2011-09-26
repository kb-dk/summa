/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.facetbrowser;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import dk.statsbiblioteket.summa.common.util.CollatorFactory;
import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Locale;

/**
 * Verifying behavious of the ICU Collator.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CollatorTest extends TestCase {
    private static Log log = LogFactory.getLog(CollatorTest.class);

    public CollatorTest(String name) {
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

    public void testSpace() {
        Locale DA = new Locale("da");
        assertTrue("Standard compareTo should sort space first",
                   "a b".compareTo("aa") < 0);

        Collator manual = Collator.getInstance(DA);
        //manual.setStrength(Collator.QUATERNARY);
        if (manual instanceof RuleBasedCollator) {
            ((RuleBasedCollator)manual).setAlternateHandlingShifted(true);
        } else {
            fail("Expected a RuleBasedCollator, got " + manual.getClass());
        }
        assertTrue("Manual Collator should ignore space at tertiary",
                   manual.compare("ab", "a c") < 0);
//        assertTrue("Manual Collator should prioritize space at quaternary",
//                   manual.compare("a d", "ad") < 0);

        Collator standard = CollatorFactory.createCollator(DA);
        assertTrue("Standard Collator should ignore space at tertiary",
                   standard.compare("ab", "a c") < 0);
//        assertTrue("Standard Collator should prioritize space at quaternary",
//                   standard.compare("a d", "ad") < 0);
    }
}
