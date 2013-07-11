package dk.statsbiblioteket.summa.support.harmonise.hub;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles looking up terms in a list of strings describing black listed terms
 */
public class BlacklistMatcher {
    private final List<String> blacklistPatterns = new ArrayList<String>();

    public BlacklistMatcher(List<String> blacklistPatterns) {
        this.blacklistPatterns.addAll(blacklistPatterns);
    }

    public boolean blacklistContains(Query query) {
        if (query instanceof TermQuery) {
            return blacklistContains((TermQuery) query);
        } else if (query instanceof PhraseQuery) {
            return blacklistContains((PhraseQuery) query);
        } else if (query instanceof TermRangeQuery) {
            return blacklistContains((TermRangeQuery) query);
        } else if (query instanceof PrefixQuery) {
            return blacklistContains((PrefixQuery) query);
        } else if (query instanceof FuzzyQuery) {
            return blacklistContains((FuzzyQuery) query);
        }
        return false;
    }

    public boolean blacklistContains(Term term) {
        return fieldMatch(term.field()) || textMatch(term.text()) || textAndFieldMatch(term.field(), term.text());
    }

    private boolean blacklistContains(TermQuery termQuery) {
        return blacklistContains(termQuery.getTerm());
    }

    private boolean blacklistContains(PhraseQuery phraseQuery) {
        Term[] terms = phraseQuery.getTerms();
        return terms.length > 1 ? fieldMatch(terms[0].field()) : blacklistContains(terms[0]);
    }

    private boolean blacklistContains(TermRangeQuery termRangeQuery) {
        return fieldMatch(termRangeQuery.getField());
    }

    private boolean blacklistContains(PrefixQuery prefixQuery) {
        return fieldMatch(prefixQuery.getField());
    }

    private boolean blacklistContains(FuzzyQuery fuzzyQuery) {
        return fieldMatch(fuzzyQuery.getField());
    }

    private boolean textMatch(String text) {
        return blacklistPatterns.contains(":" + (text == null ? "" : text));
    }

    private boolean fieldMatch(String field) {
        return blacklistPatterns.contains((field == null ? "" : field) + ":");
    }

    private boolean textAndFieldMatch(String field, String text) {
        return field != null && text != null && blacklistPatterns.contains(field + ":" + text);
    }

    public List<String> getBlacklistPatterns() {
        return Collections.unmodifiableList(blacklistPatterns);
    }
}
