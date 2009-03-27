package dk.statsbiblioteket.summa.common.lucene.analysis;

import dk.statsbiblioteket.util.reader.CharArrayReplacer;

import java.io.Reader;
import java.util.Map;

/**
 *
 */
public class TransliteratingReader extends CharArrayReplacer {

    public TransliteratingReader(Reader in, String rules, boolean keepDefaults){
        this(in, RuleParser.parse(sanitizeRules(rules, keepDefaults)));
    }

    public TransliteratingReader(Reader in, Map<String, String> rules) {
        super(in, rules);
    }

    private static String sanitizeRules(String rules, boolean keepDefaults) {
        if (keepDefaults) {
            if (rules == null || "".equals(rules)) {
                return TransliterationFilter.ALL_TRANSLITERATIONS;
            } else {
                return rules + TransliterationFilter.ALL_TRANSLITERATIONS;
            }
        } else if (rules == null) {
            return "";
        } else {
            return rules;
        }
    }
}
