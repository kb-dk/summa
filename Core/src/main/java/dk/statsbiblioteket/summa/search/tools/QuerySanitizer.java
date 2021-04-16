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
package dk.statsbiblioteket.summa.search.tools;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pre-processor for entered queries that handles common problems.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QuerySanitizer implements Configurable {
    private static Log log = LogFactory.getLog(QuerySanitizer.class);

    public enum ACTION {ignore, remove, escape}

    /**
     * How to handle smart quotes, as send by Apple iOS 11 devices or text copied from Microsoft Word.
     * </p><p>
     * Optional. Possible actions are ignore, remove and escape (aka replace with the common quote sign {@code "}).
     */
    public static final String CONF_FIX_SMARTQUOTES = "sanitizer.smartquotes";
    public static final ACTION DEFAULT_FIX_SMARTQUOTES = ACTION.escape;

    /**
     * The characters treated as quote signs, when {@link #CONF_FIX_EXCLAMATIONS} is true.
     * </p><p>
     * Optional. Default is the list from https://unicode-table.com/en/sets/quotation-marks/
     */
    public static final String CONF_SMARTQUOTES = "sanitizer.smartquotes.characters";
    public static final String DEFAULT_SMARTQUOTES = "«‹»›„‚“‟‘‛”’❛❜❟❝❞❮❯\u2E42〝〞〟";

    /**
     * How to handle unbalanced quotation marks-
     * </p><p>
     * Optional. Possible actions are ignore, remove (default) and escape.
     */
    public static final String CONF_FIX_QUOTES = "sanitizer.quotes";
    public static final ACTION DEFAULT_FIX_QUOTES = ACTION.remove;

    /**
     * How to handle mismatched parentheses.
     * </p><p>
     * Optional. Possible actions are ignore, remove (default) and escape.
     */
    public static final String CONF_FIX_PARENTHESES = "sanitizer.parentheses";
    public static final ACTION DEFAULT_FIX_PARENTHESES = ACTION.remove;

    /**
     * How to handle qualifiers without terms (foo: ).
     * </p><p>
     * Optional. Possible actions are ignore, remove and escape (default).
     */
    public static final String CONF_FIX_QUALIFIERS = "sanitizer.qualifierswithoutterms";
    public static final ACTION DEFAULT_FIX_QUALIFIERS = ACTION.escape;

    /**
     * How to handle trailing exclamation marks.
     * </p><p>
     * Optional. Possible actions are ignore, remove and escape (default).
     */
    public static final String CONF_FIX_EXCLAMATIONS = "sanitizer.trailingexclamationmarks";
    public static final ACTION DEFAULT_FIX_EXCLAMATIONS = ACTION.escape;

    /**
     * How to handle regexps. Regexps are signalled by the use of '/'.
     * </p><p>
     * Optional. Possible actions are ignore, remove (default) and escape.
     */
    public static final String CONF_FIX_REGEXP = "sanitizer.regexps";
    public static final ACTION DEFAULT_FIX_REGEXP = ACTION.escape;


    /**
     * How to handle regexps. Regexps are signalled by the use of '/'.
     * </p><p>
     * Optional. Possible actions are ignore, remove (default) and escape.
     */
    public static final String CONF_FIX_TRAILING_QUESTIONMARKS = "sanitizer.trailingquestionmarks";
    public static final boolean DEFAULT_FIX_TRAILING_QUSTIONMARKS = false;

    // TODO: Standalone dash (-), ampersand (&) and other special characters

    private final ACTION fixSmartQuotes;
    private final Pattern SMART_QUOTES;
    private final ACTION fixQuotes;
    private final ACTION fixParentheses;
    private final ACTION fixQualifiers;
    private final ACTION fixExclamations;
    private final ACTION fixRegexps;
    private final boolean fixTrailingQuestionmarks;

    @SuppressWarnings({"UnusedParameters"})
    public QuerySanitizer(Configuration conf) {

        fixSmartQuotes =  ACTION.valueOf(conf.getString(CONF_FIX_SMARTQUOTES, DEFAULT_FIX_SMARTQUOTES.toString()));
        fixQuotes =       ACTION.valueOf(conf.getString(CONF_FIX_QUOTES, DEFAULT_FIX_QUOTES.toString()));
        fixParentheses =  ACTION.valueOf(conf.getString(CONF_FIX_PARENTHESES, DEFAULT_FIX_PARENTHESES.toString()));
        fixQualifiers =   ACTION.valueOf(conf.getString(CONF_FIX_QUALIFIERS, DEFAULT_FIX_QUALIFIERS.toString()));
        fixExclamations = ACTION.valueOf(conf.getString(CONF_FIX_EXCLAMATIONS, DEFAULT_FIX_EXCLAMATIONS.toString()));
        fixRegexps =      ACTION.valueOf(conf.getString(CONF_FIX_REGEXP, DEFAULT_FIX_REGEXP.toString()));
        fixTrailingQuestionmarks = conf.getBoolean(CONF_FIX_TRAILING_QUESTIONMARKS, false);
        SMART_QUOTES = generateSmartQuotePattern(conf.getString(CONF_SMARTQUOTES, DEFAULT_SMARTQUOTES));

        log.debug(String.format(Locale.ROOT,
                "Created QuerySanitizer with smartquotes:%s, quotes:%s, parentheses:%s, qualifiers:%s, " +
                "exclamations:%s, regexps:%s, , questionmarks:%s",
                                fixSmartQuotes, fixQuotes, fixParentheses, fixQualifiers,
                                fixExclamations, fixRegexps, fixTrailingQuestionmarks));
    }

    private Pattern generateSmartQuotePattern(String quoteChars) {
        return Pattern.compile("[" + quoteChars + "]");
    }

    /**
     * The reply from sanitizing a query.
     */
    public static class SanitizedQuery {
        /**
         * none:        No re-writing.<br/>
         * summasyntax: Change from Lucene QueryParser syntax (e.g. 'New paradigm: Turtles' is legal)<br/>
         * error:       Syntax error (e.g. unbalanced parentheses or quotes)
         */
        public enum CHANGE {none, summasyntax, error}
        private List<String> queries = new ArrayList<>(5);
        private List<String> feedback = new ArrayList<>(5);
        private List<CHANGE> changes = new ArrayList<>(5);

        public SanitizedQuery(String originalQuery) {
            queries.add(originalQuery);
            feedback.add("");
            changes.add(CHANGE.none);
        }

        public void addChange(String newQuery, CHANGE change, String feedback) {
            queries.add(newQuery);
            changes.add(change);
            this.feedback.add(feedback == null ? "" : feedback);
        }

        public String getOriginalQuery() {
            return queries.get(0);
        }
        public String getLastQuery() {
            return queries.get(queries.size()-1);
        }
        public List<String> getQueries() {
            return getChangeCount() == 0 ? new ArrayList<String>(0) : queries.subList(1, queries.size());
        }
        public CHANGE getLastChange() {
            return changes.get(changes.size()-1);
        }
        public List<CHANGE> getChanges() {
            return getChangeCount() == 0 ? new ArrayList<CHANGE>(0) : changes.subList(1, changes.size());
        }

        /**
         * @return human-readable message if change is summasyntax or error, else "".
         */
        public String getLastFeedback() {
            return feedback.get(feedback.size() -1);
        }
        public List<String> getFeedbacks() {
            return getChangeCount() == 0 ? new ArrayList<String>(0) : feedback.subList(1, feedback.size());
        }

        /**
         * @return the number of changes to the query. An unmodified query has change count 0.
         */
        public int getChangeCount() {
            return changes.size()-1;
        }
    }

    /**
     * The sanitizer handles the following common problems:<br/>
     * - Unbalanced quotes (error)<br/>
     * - Unbalanced parentheses (error)<br/>
     * - Colon without following term (summasyntax)<br/>
     * - Faulty range (brackets without TO = summasyntax)<br/>
     * - Trailing exclamation mark (summasyntax)
     * @param query the query to sanitize.
     * @return the sanitized query + .
     */
    public SanitizedQuery sanitize(String query) {
        if ("".equals(query)) {
            return new SanitizedQuery(query);
        }
        return sanitize(new SanitizedQuery(query));
    }

    private SanitizedQuery sanitize(SanitizedQuery query) {
        int lastChangeCount = -1;
        while (query.getChangeCount() != lastChangeCount) {
            if (fixSmartQuotes != ACTION.ignore) {
                fixSmartQuotes(query); // Must be before fixQuotes
            }
            if (fixQuotes != ACTION.ignore) {
                fixQuotes(query); // Must be before parentheses etc.
            }
            if (fixParentheses != ACTION.ignore) {
                fixParentheses(query);
            }
            if (fixQualifiers != ACTION.ignore) { // TODO: Consider 'foo:(bar baz)'
                fixTrailing(query, "Qualifier (term with colon) without content", ':', fixQualifiers);
            }
            if (fixExclamations != ACTION.ignore) { // TODO: Consider 'foo!!!'
                fixTrailing(query, "Exclamation mark without expression", '!', fixExclamations);
            }
            if (fixRegexps!= ACTION.ignore) {
                fixRegexps(query, fixRegexps);
            }
            if (fixTrailingQuestionmarks) {
                fixTrailingQuestionMarks(query);
            }


            lastChangeCount = query.getChangeCount();
        }
        return query;
    }

    private void fixRegexps(SanitizedQuery query, ACTION action) {
        String adjusted = fixChar(query.getLastQuery(), '/', action);
        if (adjusted != null && !adjusted.equals(query.getLastQuery())) {
            query.addChange(adjusted, SanitizedQuery.CHANGE.summasyntax, "Regexp signal char '/'");
        }
    }

    private void fixSmartQuotes(SanitizedQuery query) {
        String adjusted = SMART_QUOTES.matcher(query.getLastQuery()).replaceAll(
                fixSmartQuotes == ACTION.escape ? "\"" : "");

        if (adjusted != null && !adjusted.equals(query.getLastQuery())) {
            query.addChange(adjusted, SanitizedQuery.CHANGE.error,
                            "Quote-like characters replaced with " +
                            (fixSmartQuotes == ACTION.escape ? "simple quote sign (\")" : "empty string"));
        }
    }

    private void fixQuotes(SanitizedQuery query) {
        int quoteCount = countQuotes(query.getLastQuery());
        if (quoteCount % 2 != 0) {
            String adjusted = fixChar(query.getLastQuery(), '\"', fixQuotes);
            query.addChange(adjusted, SanitizedQuery.CHANGE.error, "Uneven (" + quoteCount + ") number of quotation marks");
        }
    }

    private void fixTrailingQuestionMarks(SanitizedQuery query) {
        String lastQuery = query.getLastQuery();
        //Replace all trailing ? with *. Example: Hvor er Toke??? -> Hvor er Toke* .  Hvor er Toke -> Hvor er Toke (no change)
        boolean anyReplaced=false;
        while (lastQuery.endsWith("?")){
            lastQuery=lastQuery.substring(0, lastQuery.length()-1);
            anyReplaced=true;
        }
        if (anyReplaced){
            lastQuery=lastQuery+"*";
        }
        query.addChange(lastQuery, SanitizedQuery.CHANGE.summasyntax, "replaced trailing questionmarks");
    }

    // If a single parentheses-fail is found, all non-quoted parentheses are removed
    private void fixParentheses(SanitizedQuery query) {
        String q = query.getLastQuery();
        boolean inQuote = false;
        boolean escape = false;
        int parenthesisLevel = 0;

        for (int i = 0 ; i < q.length() ; i++) {
            char p = i == 0 ? 0 : q.charAt(i-1); // previous
            char c = q.charAt(i);                // current
            if (escape) {
                escape = false;
            } else if (p == '\\') {
                escape = true;
            } else if (c == '\"') {
                inQuote = !inQuote;
            } else if (!inQuote && c == '(') {
                parenthesisLevel++;
            } else if (!inQuote && c == ')') {
                parenthesisLevel--;
                if (parenthesisLevel < 0) {
                    query.addChange(fixChar(fixChar(q, '(', fixParentheses), ')', fixParentheses),
                                    SanitizedQuery.CHANGE.error, "Missing start parenthesis");
                    return;
                }
            }
        }
        if (parenthesisLevel > 0) {
            query.addChange(fixChar(fixChar(q, '(', fixParentheses), ')', fixParentheses),
                            SanitizedQuery.CHANGE.error, "Missing end parenthesis");
        }
    }

    private void fixTrailing(SanitizedQuery query, String message, char trailer, ACTION action) {
        String q = query.getLastQuery();
        boolean inQuote = false;
        boolean escape = false;
        for (int i = 0 ; i < q.length() ; i++) {
            char p = i == 0 ? 0 : q.charAt(i-1); // previous
            char c = q.charAt(i);                // current
            char n = i == q.length()-1 ? 0 : q.charAt(i+1); // next
            if (escape) {
                escape = false;
            } else if (p == '\\') {
                escape = true;
            } else if (c == '\"') {
                inQuote = !inQuote;
            } else if (!inQuote && c == trailer && (n == 0 || n == ' ')) {
                String newQuery = q.substring(0, i);
                if (action == ACTION.escape) {
                    newQuery += "\\" + c;
                }
                if (n != 0) {
                    newQuery += q.substring(i + 1);
                }

                query.addChange(newQuery, SanitizedQuery.CHANGE.summasyntax, message);
                return;
            }
        }
    }

    private String fixChar(String query, char problem, ACTION action) {
        StringWriter reduced = new StringWriter(query.length());
        boolean inQuote = false;
        boolean escape = false;
        for (int i = 0 ; i < query.length(); i++) {
            char p = i == 0 ? 0 : query.charAt(i-1); // previous
            char c = query.charAt(i);                // current
            if (escape) {
                escape = false;
                reduced.append(c);
            } else if (p == '\\') {
                escape = true;
                reduced.append(c);
            } else if (!inQuote && c == problem) {
                if (action == ACTION.escape) {
                    reduced.append("\\").append(c);
                }
                // Else Skip char
            } else if (c == '\"') {
                inQuote = !inQuote;
                reduced.append(c);
            } else {
                reduced.append(c);
            }
        }
        return reduced.toString();
    }

    private int countQuotes(String query) {
        int count = 0;
        for (int i = 0 ; i < query.length() ; i++) {
            if (query.charAt(i) == '\"') {
                if (i == 0 || query.charAt(i-1) != '\\') { // compensate for escaping
                    count++;
                }
            }
        }
        return count;
    }
}
