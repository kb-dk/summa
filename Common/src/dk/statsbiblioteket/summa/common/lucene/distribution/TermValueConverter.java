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
package dk.statsbiblioteket.summa.common.lucene.distribution;

import dk.statsbiblioteket.summa.common.pool.ValueConverter;
import dk.statsbiblioteket.util.qa.QAInfo;

import java.io.UnsupportedEncodingException;

/**
 * Converts TermEntries to and from bytes.
 * </p><p>
 * A TermEntry contains a term (String) and a term count (int).
 * The byte-representation is {@code
   the term
   space
   the term count as a String representation of the integer
   newline
 } all stored as UTF-8 bytes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermValueConverter implements ValueConverter<TermEntry> {
//    private static Log log = LogFactory.getLog(TermValueConverter.class);

    private static String DELIMITER = " ";

    public byte[] valueToBytes(TermEntry value) {
        try {
            return (value.getTerm() + DELIMITER
                    + Integer.toString(value.getCount())
                    + "\n").getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Conversion to UTF-8 failed", e);
        }
    }

    public TermEntry bytesToValue(byte[] buffer, int length) {
        String content;
        try {
            content = new String(buffer, 0, length, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Conversion from UTF-8 failed", e);
        }
        int pos = content.lastIndexOf(DELIMITER);
        // No explicit check for existence of delimiter as we don't care whether
        // a "No DELIMITER found or an ArrayIndexOutOfBounds is thrown
        // The same goes for the Integer.parseInt-call. In all three cases, we
        // can infer what happened.
        return new TermEntry(content.substring(0, pos), Integer.parseInt(
                        content.substring(pos + 1, content.length()-1)));
        // Don't use termEntryBuilder as one thread can use multiple entries
        // at the same time
    }

/*    private ThreadLocal<TermEntry> termEntryBuilder =
            new ThreadLocal<TermEntry>() {
                @Override
                protected TermEntry initialValue() {
                    return new TermEntry("termentrydummy", 0);
                }
            };
            */
}

