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
package dk.statsbiblioteket.summa.common.util;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 */
public class UnicodeUtil {
    private static Log log = LogFactory.getLog(UnicodeUtil.class);

    /**
     * Checks the input for unicode-points above 0xFFFF as the axis framework produces invalid XML entities with
     * those. Any such high-order codepoints are removed and a warning is logged.
     * @param designation identification of the String (the record is was takes from or similar). Used for logging
     * @param in the String to prune.
     * @param warn if true, removal of characters will result in a warn in the log, else a debug.
     * @param log the log to used if codepoints ware removed. If null, no logging is performed.
     */
    public static String pruneHighOrderUnicode(String designation, String in, boolean warn, Log log) {
        final int LIMIT = 0xFFFF;
        if (in == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(in.length());
        int removes = 0;
        for (int offset = 0; offset < in.length(); ) {
            final int codepoint = in.codePointAt(offset);
            if (codepoint <= LIMIT) {
                sb.append((char)codepoint);
            } else {
                removes++;
            }
            offset += Character.charCount(codepoint); // Probably faster to special case this in the if above
        }
        if (log != null && removes > 0) {
            String message = "Encountered and removed " + removes + " chars with codepoint > " + LIMIT +
                             " from " + designation;
            if (warn) {
                log.warn(message);
            } else {
                log.debug(message);
            }
        }
        return sb.toString();
    }

}
