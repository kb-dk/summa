/* $Id: Normalize.java,v 1.5 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/05 10:20:23 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.plugins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This plugin is used to remove 'noize' and normalize a String.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class Normalize {

    /**
     * A thread local StringBuffer that is reset on each get() request
     */
    private static final ThreadLocal<StringBuffer> threadLocalBuffer =
                                               new ThreadLocal<StringBuffer>() {
        @Override
        protected StringBuffer initialValue() {
            return new StringBuffer();
        }

        @Override
        public StringBuffer get() {
            StringBuffer buf = super.get();
            buf.setLength(0); // clear/reset the buffer
            return buf;
        }

    };

    /**
     * The String is trimed and:    - (  ) , . [ ]     is removed<br>
     * @param in the string to normalize
     * @return  the normalized string
     */
    public static String normalize(String in){
        StringBuffer buf = threadLocalBuffer.get();

        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);

            // This conditional is "ad-hoc-optimized" in the OR branching for
            // decreasingly frequent characters in the target strings
            if (c == '-' || c == ',' || c == '.' || c == '(' || c == ')'
                || c == '[' || c == ']') {
                continue;
            }

            buf.append(c);
        }

        return getTrimmedString(buf);
    }

    private static String getTrimmedString(StringBuffer buf) {
        int firstChar, lastChar;

        for (firstChar = 0; firstChar < buf.length(); firstChar++) {
            if (!Character.isWhitespace(buf.charAt(firstChar))) {
                break;
            }
        }

        for (lastChar = buf.length() - 1; lastChar >= 0; lastChar--) {
            if (!Character.isWhitespace(buf.charAt(lastChar))) {
                break;
            }
        }

        // String is all whitespace
        if (firstChar == buf.length()) {
            return "";
        }

        return buf.substring(firstChar, lastChar+1);
    }

    /*public static void main (String[] args) {
        System.out.println("'" + normalize("  foobar-fs[)sdf,-.ls  l   ")+ "'");
        System.out.println("'" + normalize("foobar")+ "'");
        System.out.println("'" + normalize("-  (.  ,")+ "'");
    }*/
}



