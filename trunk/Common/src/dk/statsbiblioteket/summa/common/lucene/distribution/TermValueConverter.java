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
package dk.statsbiblioteket.summa.common.lucene.distribution;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.pool.ValueConverter;

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
