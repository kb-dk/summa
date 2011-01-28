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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Helper class with static methods to support escaping system properties
 * and environment variables in strings.
 */
public class Environment {
    private static Log log = LogFactory.getLog(Environment.class);

    /**
     * Escape any system properties references in Ant-like syntax, eg. the
     * string<br/>
     * <pre>
     *   "Your home dir is ${user.home}"
     * </pre>
     * escapes to<br/>
     * <pre>
     *    "Your home dir is /home/username"
     * </pre>
     *
     * @param s the string to escape, if {@code s} is {@code null} this method
     *          also returns {@code null}
     * @return the string with any system property references replaces by their
     *         actual values, or {@code null} if the input string is
     *         {@code null}
     */
    public static String escapeSystemProperties (String s) {
        if (s == null) {
            return null;
        }

        String result = s;

        // Micro optimization to not escape anything if there are no refs in s
        if (!s.contains("${")) {
            return s;
        }

        // This is ridiculously inefficient, but it gets the job done...
        for (Map.Entry entry: System.getProperties().entrySet()){
            String pattern = "${" + entry.getKey().toString() + "}";
            String newVal = entry.getValue().toString();

            result = result.replace(pattern, newVal);
        }

        return result;
    }

    /**
     * Return a copy of {@code a} with all system property references expanded
     * as described in {@link #escapeSystemProperties(String)}.
     *
     * @param a array from which to extract the strings to escape, if {@code a}
     *          is {@code null} this method also returns {@code null}
     * @return a copy of {@code a} with system property values expanded, or
     *         {@code null} if the input string is {@code null}
     */
    public static String[] escapeSystemProperties (String[] a) {
        if (a == null) {
            return null;
        }

        String[] result = new String[a.length];

        for (int i = 0; i < a.length; i++) {
            result[i] = escapeSystemProperties(a[i]);
        }

        return result;
    }

    /**
     *
     * Iterate through {@code iterable} and write all strings with system
     * property references expanded into a list. The system property expansion
     * occurs as described in {@link #escapeSystemProperties(String)}.
     *
     * @param iterable an iterable object to extract strings to escape from, if
     *                 {@code a} is {@code null} this method also returns
     *                 {@code null}
     * @return a list containing all the strings of {@code iterable}
     *         with system property values expanded, or
     *         {@code null} if the input string is {@code null}
     */
    public static List<String> escapeSystemProperties (
                                                    Iterable<String> iterable) {
        if (iterable == null) {
            return null;
        }

        ArrayList<String> result = new ArrayList<String>();

        for (String s : iterable) {
            result.add(escapeSystemProperties(s));
        }

        return result;
    }

    /**
     * Determines the version of the running JVM. If the version is incompatible
     * with Lucene (See
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6707044
     * for details) an Error is thrown. Running with an incompatible version
     * introduces biterrors in large Lucene indexes (statistically speaking).
     * </p><p>
     * It is highly recommended to call this method very early, to provide a
     * fail-fast handling of the incompatibility.
     * @return the version of the curent JVM or null if it could not be
     *         determined.
     * @throws Error if the version is incompatible with Lucene.
     */
    public static String checkJavaVersion() throws Error {
        String version = System.getProperty("java.runtime.version");
        Matcher matcher = version == null ? null :
                          VERSION_PATTERN.matcher(version);
        if (version == null || !matcher.matches()) {
            log.warn("Unable to determine Java runtime version by property "
                     + "'java.runtime.version'. Please check that the JVM does"
                     + " not have bug #6707044 (see http://bugs.sun.com/bugdat"
                     + "abase/view_bug.do?bug_id=6707044) for details");
            return version;
        }
        // version should be like "1.6.0_10-b33"
        if (!(matcher.group(1).equals("1") && matcher.group(2).equals("6")
              && matcher.group(3).equals("0"))) {
            return version; // Only versions 1.6.0_* are critical
        }
        int update = Integer.parseInt(matcher.group(4));
        int build = Integer.parseInt(matcher.group(5));
        if ((update >= 4 && update <= 10) && !(update == 10 && build > 25)) {
            throw new Error(
                    "Incompatible Java runtime version. Due to the bug http://"
                    + "bugs.sun.com/bugdatabase/view_bug.do?bug_id=6707044, "
                    + "running with Java runtime versions from 1.6.0_04 to "
                    + "1.6.0.10-b25 will sometimes result in corrupted Lucene "
                    + "indexes. The current Java runtime version is "
                    + version);
        }
        log.debug("Java runtime version is " + version + ". All OK");
        return version;
    }
    private static Pattern VERSION_PATTERN = Pattern.compile(
            "([0-9]+)\\.([0-9]+)\\.([0-9]+)\\_([0-9]+)\\-b([0-9]+)");
}

