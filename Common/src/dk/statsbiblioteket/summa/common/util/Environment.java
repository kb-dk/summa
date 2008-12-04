package dk.statsbiblioteket.summa.common.util;

import java.util.Map;

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
     * @param s the string to escape
     * @return the string with any system property references replaces by their
     *         actual values
     */
    public static String escapeSystemProperties (String s) {
        String result = s;

        // This is ridiculously inefficient, but it gets the job done...
        for (Map.Entry entry: System.getProperties().entrySet()){
            String pattern = "${" + entry.getKey().toString() + "}";
            String newVal = entry.getValue().toString();

            result = result.replace(pattern, newVal);
        }

        return result;
    }
        
}
