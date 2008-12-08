package dk.statsbiblioteket.summa.common.util;

import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class with static methods to support escaping system properties
 * and environment variables in strings.
 */
public class Environment {

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
}
