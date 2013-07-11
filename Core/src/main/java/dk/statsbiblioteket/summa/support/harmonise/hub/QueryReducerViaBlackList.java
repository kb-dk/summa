package dk.statsbiblioteket.summa.support.harmonise.hub;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * Removes blacklisted terms from queries and reduces the resulting query. A blacklisted term is known to give no
 * hits when searching. That means a query can be reduced according to the following rules
 *
 * 1. A blacklisted term is irrelevant and can be removed
 * 2. A blacklisted term is marked required in a boolean clause which causes the whole boolean query to return no hits
 */
public class QueryReducerViaBlackList {

    private BlacklistMatcher blacklistMatcher;

    public QueryReducerViaBlackList(BlacklistMatcher blacklistMatcher) {
        this.blacklistMatcher = blacklistMatcher;
    }

    /**
     * Reduces the given query according to the blacklisted terms
     * @param query the given query
     * @return the reduced query or null if the given query can be proven to result in no hits
     */
    public Query reduce(Query query) {
        return query instanceof BooleanQuery ? reduceNode((BooleanQuery) query) : reduceLeaf(query);
    }

    private Query reduceNode(BooleanQuery booleanQuery) {
        BooleanQuery result = booleanQuery.clone();
        result.clauses().clear();

        for (BooleanClause clause : booleanQuery.clauses()) {
            Query clauseQuery = reduce(clause.getQuery());

            if (clauseQuery != null) {
                result.add(new BooleanClause(clauseQuery, clause.getOccur()));
            } else if (clause.isRequired()) {
                return null;
            }
        }

        return result.clauses().isEmpty() ? null : result;
    }

    private Query reduceLeaf(Query query) {
        if (!(query instanceof TermQuery)) {
            return query;
        }
        Term term = ((TermQuery) query).getTerm();
        return blacklistMatcher.blacklistContains(term) ? null : query;
    }
}
