/**
 * Created: te 30-05-2008 16:01:36
 * CVS:     $Id$
 */
package dk.statsbiblioteket.support.lucene.search;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexDescriptor;
import dk.statsbiblioteket.summa.common.lucene.LuceneIndexUtils;
import dk.statsbiblioteket.summa.search.SearchNodeLoadBalancer;
import dk.statsbiblioteket.summa.search.SummaSearcher;
import dk.statsbiblioteket.summa.search.IndexWatcher;
import dk.statsbiblioteket.summa.support.lucene.search.LuceneSearchNode;

import java.io.IOException;

/**
 * Lucene-specic searcher. Relevant properties from {@link SummaSearcher},
 * {@link IndexWatcher}, {@link SearchNodeLoadBalancer} , {@link LuceneSearchNode}
 * and {@link LuceneIndexUtils} needs to be specified.
 * @deprecated in favor of the Search framework.
 */
public class LuceneSearcher {/*extends SummaSearcherImpl implements
                                                      LuceneSearcherMBean {*/
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
        //super(conf);
        maxBooleanClauses =
                conf.getInt(CONF_MAX_BOOLEAN_CLAUSES, maxBooleanClauses);
    }

    public SearchNodeLoadBalancer constructSearchNode(Configuration conf) throws
                                                                   IOException {
        if (descriptor == null) { // Bit of a hack
            descriptor = LuceneIndexUtils.getDescriptor(conf);
            maxBooleanClauses =
                    conf.getInt(CONF_MAX_BOOLEAN_CLAUSES, maxBooleanClauses);
        }
        throw new UnsupportedOperationException("Not implemented yet");
//        return new SearchNodeLoadBalancer(this, conf,
//                new LuceneSearchNode(this, conf, descriptor));
    }
}


