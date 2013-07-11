package dk.statsbiblioteket.summa.support.harmonise.hub;

import org.apache.lucene.index.Term;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles looking up terms in a set of strings describing black listed terms
 */
public class BlacklistMatcher {
    private final Set<String> blacklistPatterns = new HashSet<String>();

    public BlacklistMatcher(Set<String> blacklistPatterns) {
        this.blacklistPatterns.addAll(blacklistPatterns);
    }

    public boolean blacklistContains(Term term) {
        String field = term.field() == null ? "" : term.field();
        String text = term.text() == null ? "" : term.text();
        return fieldMatch(field) || textMatch(text) || textAndFieldMatch(field, text);
    }

    private boolean textMatch(String text) {
        return blacklistPatterns.contains(":" + text);
    }

    private boolean fieldMatch(String field) {
        return blacklistPatterns.contains(field + ":");
    }
    
    private boolean textAndFieldMatch(String field, String text) {
        return blacklistPatterns.contains(field + ":" + text);
    }
}
