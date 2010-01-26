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




