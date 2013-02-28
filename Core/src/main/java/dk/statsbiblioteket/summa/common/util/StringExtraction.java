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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper utils for extracting sub strings from strings.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class StringExtraction {
    private static Log log = LogFactory.getLog(StringExtraction.class);

    /**
     * Extract all sub Strings from input that matches the regular expression. Note that this will compile a new Pattern
     * at each call, so do not use in tight loops or similar.
     * @param input  the String to extract sub Strings from.
     * @param regexp a regular expression, intended for use with {@link Pattern}.
     * @return all sub Strings that matches the given regexp. No matches means empty list.
     */
    public static List<String> getStrings(CharSequence input, String regexp) {
        return getStrings(input, Pattern.compile(regexp, Pattern.DOTALL));
    }

    /**
     * Extract all sub Strings that matches the Pattern. Normally one wants to compile the Pattern with DOTALL.
     * @param input  the String to extract sub Strings from.
     * @param pattern all sub Strings that matches will be returned.
     * @return all sub Strings that matches the given regexp. No matches means empty list.
     */
    public static List<String> getStrings(CharSequence input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        List<String> matches = new ArrayList<String>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    /**
     * Extract matches for outerRegexp, then perform matching of innerRegexp on all outer matches. Note that this will
     * compile two new Patterns for each call. Do not use in tight loops or similar.
     * @param input  the String to extract sub Strings from.
     * @param outerRegexp a regular expression, intended for use with {@link Pattern}.
     * @param innerRegexp a regular expression, intended for use with {@link Pattern}.
     * @return all sub Strings that matches the given regexp. No matches means empty list.
     */
    public static List<String> getStrings(CharSequence input, String outerRegexp, String innerRegexp) {
        return getStrings(input,
                Pattern.compile(outerRegexp, Pattern.DOTALL), Pattern.compile(innerRegexp, Pattern.DOTALL));
    }
    /**

     * Extract matches for outerRegexp, then perform matching of innerRegexp on all outer matches.
     * @param input  the String to extract sub Strings from.
     * @param outerPattern used to locale outer text blocks.
     * @param innerPattern used to locate inner text snippets.
     * @return all sub Strings that matches the given regexp. No matches means empty list.
     */
    public static List<String> getStrings(CharSequence input, Pattern outerPattern, Pattern innerPattern) {
        List<String> merged = new ArrayList<String>();
        final Matcher outer = outerPattern.matcher(input);
        while(outer.find()) {
            merged.addAll(getStrings(outer.group(), innerPattern));
        }
        return merged;
    }
}
