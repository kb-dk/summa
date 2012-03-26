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

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pre-processor for entered queries that handles common problems.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class QuerySanitizer {
    private static Log log = LogFactory.getLog(QuerySanitizer.class);

    @SuppressWarnings({"UnusedParameters"})
    public QuerySanitizer(Configuration conf) {
        log.debug("Created QuerySanitizer");
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
        private List<String> queries = new ArrayList<String>(5);
        private List<String> feedback = new ArrayList<String>(5);
        private List<CHANGE> changes = new ArrayList<CHANGE>(5);

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
        return sanitize(new SanitizedQuery(query));
    }

    private SanitizedQuery sanitize(SanitizedQuery query) {
        int lastChangeCount = -1;
        while (query.getChangeCount() != lastChangeCount) {
            fixQuotes(query); // Must be first
            fixParentheses(query);
            removeTrailing(query, "Qualifier (term with colon) without content", ':'); // TODO: Consider 'foo:(bar baz)'
            removeTrailing(query, "Exclamation mark without expression", '!');         // TODO: Consider 'foo!!!'
            lastChangeCount = query.getChangeCount();
        }
        return query;
    }

    private void fixQuotes(SanitizedQuery query) {
        int quoteCount = countQuotes(query.getLastQuery());
        if (quoteCount % 2 != 0) {
            query.addChange(removeChar(query.getLastQuery(), '\"'), SanitizedQuery.CHANGE.error,
                            "Uneven (" + quoteCount + ") number of quotation marks");
        }
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
                    query.addChange(
                        removeChar(removeChar(q, '('), ')'), SanitizedQuery.CHANGE.error, "Missing start parenthesis");
                    return;
                }
            }
        }
        if (parenthesisLevel > 0) {
            query.addChange(
                removeChar(removeChar(q, '('), ')'), SanitizedQuery.CHANGE.error, "Missing end parenthesis");
        }
    }

    private void fixColonWithoutTerm(SanitizedQuery query) {
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
            } else if (!inQuote && c == ':' && (n == 0 || n == ' ')) { // TODO: Consider 'foo:(bar baz)'
                query.addChange(n == 0 ? q.substring(0, q.length()-1) :
                                q.substring(0, i) + q.substring(i + 1), SanitizedQuery.CHANGE.summasyntax,
                                "Qualifier (term with colon) without content");
                return;
            }
        }
    }

    private void removeTrailing(SanitizedQuery query, String message, char trailer) {
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
                query.addChange(n == 0 ? q.substring(0, q.length()-1) :
                                q.substring(0, i) + q.substring(i + 1), SanitizedQuery.CHANGE.summasyntax,
                                message);
                return;
            }
        }
    }

    private String removeChar(String query, char removable) {
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
            } else if (!inQuote && c == removable) {
                // Skip
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
