/**
 * Created: te 30-05-2008 16:01:36
 * CVS:     $Id$
 */
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;

import java.io.IOException;

/**
 * Lucene-specic searcher. Relevant properties from {@link SummaSearcher},
 * {@link IndexWatcher}, {@link SearchNodeWrapper} , {@link LuceneSearchNode}
 * and {@link LuceneIndexUtils} needs to be specified.
 */
public class LuceneSearcher extends SummaSearcherImpl implements
                                                      LuceneSearcherMBean {
    /**
     * The maximum number of boolean clauses that a query can be expanded to.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String CONF_MAX_BOOLEAN_CLAUSES = "search.clauses.max";
    public static final int DEFAULT_MAX_BOOLEAN_CLAUSES = 10000;
    // TODO: Use maxBooleanClauses
    private int maxBooleanClauses = DEFAULT_MAX_BOOLEAN_CLAUSES;
    private LuceneIndexDescriptor descriptor = null;

    public LuceneSearcher(Configuration conf) {
        super(conf);
        maxBooleanClauses =
                conf.getInt(CONF_MAX_BOOLEAN_CLAUSES, maxBooleanClauses);
    }

    public SearchNodeWrapper constructSearchNode(Configuration conf) throws
                                                                   IOException {
        if (descriptor == null) { // Bit of a hack
            descriptor = LuceneIndexUtils.getDescriptor(conf);
            maxBooleanClauses =
                    conf.getInt(CONF_MAX_BOOLEAN_CLAUSES, maxBooleanClauses);
        }
        return new SearchNodeWrapper(this, conf,
                new LuceneSearchNode(this, conf, descriptor));
    }
}
