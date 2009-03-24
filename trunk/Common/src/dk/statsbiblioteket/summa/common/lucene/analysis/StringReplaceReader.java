package dk.statsbiblioteket.summa.common.lucene.analysis;

import dk.statsbiblioteket.util.reader.StringReplacer;

import java.io.Reader;
import java.util.Map;

/**
 *
 */
public class StringReplaceReader extends StringReplacer {

    public StringReplaceReader (Reader in, String rules, boolean keepDefaults) {
        this(in, RuleParser.parse(sanitizeRules(rules, keepDefaults)));
    }

    public StringReplaceReader(Reader in, Map<String, String> replacements) {
        super(in, replacements);
    }

    private static String sanitizeRules(String rules, boolean keepDefaults) {
        if (keepDefaults) {
            if (rules == null || "".equals(rules)) {
                return TokenReplaceFilter.DEFAULT_REPLACE_RULES;
            } else {
                return rules + TokenReplaceFilter.DEFAULT_REPLACE_RULES;
            }
        } else if (rules == null) {
            return "";
        } else {
            return rules;
        }
    }
}
