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
 * This plugin contains methods for converting ISBN numbers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class ISBN {

    private static final Log log = LogFactory.getLog(ISBN.class);

    /**
     * This will try to convert a string into an ISBN-13 number.<br>
     * Valid input is a String representation of an ISBN-10 number
     *
     * WARNING: the method makes no effort to validate the input.
     *
     * if something goes wrong when parsing the input - the input will be returned.
     *
     * @param in  an ISBN-10 String
     * @return if input was ISBN-10 the ISBN-13 number is returned
     */
    public static String isbnNorm(String in) {
        try {
            String out = in.trim();
            out = out.replace("-", "");
            if (out.trim().length() == 10) {
                out = out.substring(0, 9);
                out = "978" + out;
                int sum = 0;
                for (int i = 0; i < out.length(); i++) {
                    if ((i % 2 == 0)) {
                        sum += Integer.parseInt(out.substring(i, i + 1));
                    } else {
                        sum += 3 * Integer.parseInt(out.substring(i, i + 1));
                    }
                }
                int chcckSum = sum % 10;
                if (chcckSum != 0 ) {chcckSum = 10-chcckSum;}
                return  out + chcckSum;
            }

        } catch (Exception e) {
            log.warn("Error parsing isbn '"+in+"': " + e.getMessage());
            return in;
        }
        return in;
    }
}




