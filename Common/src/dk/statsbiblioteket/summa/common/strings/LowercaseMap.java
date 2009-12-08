/* $Id$
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
package dk.statsbiblioteket.summa.common.strings;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Has a base map from char -> char which converts all chars from 0 to 65535 to
 * lowercase. Given a map char -> char or char -> char[], and ensures that all
 * rules handles both upper- and lowercase to the same result. Also ensures that
 * the result is always lowercase.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LowercaseMap {
    private static Log log = LogFactory.getLog(LowercaseMap.class);

    private static Map<String, String> LOWER;
    static { // Create lower
        LOWER = new HashMap<String, String>(65535);
        for (int cInt = 0 ; cInt < Character.MAX_VALUE ; cInt++) {
            String s = String.valueOf((char)cInt);
            if (!toLower(s).equals(s)) {
                LOWER.put(s, toLower(s));
            }
        }
        log.trace("Finished constructing base TOLOWER with " + LOWER.size()
                  + " rules");
    }

    // Technically speaking we should do this with a custom Locale.
    private static String toLower(String in) {
        return in.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Extends and updates the rules in the given replaceMap so that it converts
     * every output to lowercase. For all chars without rules, a new rule is
     * added if the value should be different from the source.
     * </p><p>
     * The replaceMap itself is not changed.
     * @param replaceMap a map with replacements. The keys must be of length 1.
     * @return a map conforming to the given rules.
     * @throws IllegalArgumentException if one or more keys are not of length 1.
     */
    public static Map<String, String> getLowercaseMap(
            Map<String, String> replaceMap) {
        Map<String, String> result = new HashMap<String, String>(
                replaceMap.size() * 2 + LOWER.size());
        // Handle existing
        for (Map.Entry<String, String> entry: replaceMap.entrySet()) {
            String key = entry.getKey();
            String value = toLower(entry.getValue());
            if (!key.equals(toLower(key))) {
                // Check for alternative value
                String lowerValue = toLower(replaceMap.get(toLower(key)));
//                if (!lowerValue.)
            }
            // Ensure destination is lowercase
            if (!value.equals(toLower(value))) {
                value = toLower(value);
                entry.setValue(toLower(entry.getValue()));
            }
            // Check for both upper- and lowercase keys
        }

        // Fill missing
        for (Map.Entry<String, String> entry: LOWER.entrySet()) {
            if (!replaceMap.containsKey(entry.getKey())) {
                replaceMap.put(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }
}
