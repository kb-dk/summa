/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2008  The State and University Library
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
package dk.statsbiblioteket.summa.common.unittest;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.StringWriter;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 *
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ExtraAsserts extends TestCase {
    private static Log log = LogFactory.getLog(ExtraAsserts.class);

    /**
     * Compares the two arrays and gives a detailed fail if not equal.
     * </p><p>
     * Note: Don't use this for humongous arrays as they are converted to
     *       Strings for output.
     * @param message  what to fail with if the arreys are not equal.
     * @param expected the expected result.
     * @param actual   the actuak result.
     */
    public static void assertEquals(String message, int[] expected,
                                    int[] actual) {
        Arrays.sort(expected);
        if (!Arrays.equals(expected, actual)) {
            //noinspection DuplicateStringLiteralInspection
            fail(message + ". Expected " + dump(expected)
                 + " got " + dump(actual));
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

}
