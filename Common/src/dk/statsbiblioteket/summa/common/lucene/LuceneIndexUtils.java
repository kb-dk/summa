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
package dk.statsbiblioteket.summa.common.lucene;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.index.IndexDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;

/**
 * Lucene-related helper methods.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class LuceneIndexUtils {
    private static Log log = LogFactory.getLog(LuceneIndexUtils.class);

    /**
     * The subfolder in the index root containing the lucene index.
     * This will be appended to {@link #indexRoot}.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String LUCENE_FOLDER = "lucene";

    /**
     * Used to communicate changes to Lucene-indexes caused by this Record.
     * Updates will have both {@link #META_DELETE_DOCID} and META_ADD_DOCID.
     */
    public static final String META_ADD_DOCID = "Index_DocID_Add";
     /**
      * Used to communicate changes to Lucene-indexes caused by this Record.
      * Updates will have both META_DELETE_DOCID and {@link #META_ADD_DOCID}.
      */
    public static final String META_DELETE_DOCID = "Index_DocID_Delete";

    /**
     * Get the sub-properties {@link IndexDescriptor#CONF_DESCRIPTOR} and create
     * a descriptor based on that. If the sub-properties does not exist, the
     * default descriptor is created.
     * @param conf a configuration with the setup for descriptor stored as a
     *             sub-configuration with the key
     *             {@link IndexDescriptor#CONF_DESCRIPTOR}.
     * @return a descriptor usable for indexing (and querying).
     * @throws Configurable.ConfigurationException if there was an error with
     *                                             the configuration.
     */
    public static LuceneIndexDescriptor getDescriptor(Configuration conf) throws
                                           Configurable.ConfigurationException {
        Configuration descConf = null;
        try {
            descConf =
                    conf.getSubConfiguration(IndexDescriptor.CONF_DESCRIPTOR);
        } catch (IOException e) {
            //noinspection DuplicateStringLiteralInspection
            log.error("Exception requesting '" + IndexDescriptor.CONF_DESCRIPTOR
                      + "' from properties", e);
        } catch (UnsupportedOperationException e) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("The configuration does not support sub-storages. "
                      + "Attempting to use the storage directly");
            descConf = conf;
        }
        LuceneIndexDescriptor descriptor;
        if (descConf == null) {
            log.warn("No '" + IndexDescriptor.CONF_DESCRIPTOR
                     + "' specified in properties. "
                     + "Using default LuceneIndexDescriptor");
            descriptor = new LuceneIndexDescriptor();
        } else {
            log.trace("Creating LuceneIndexDescriptor based on properties");
            try {
                descriptor = new LuceneIndexDescriptor(descConf);
            } catch (IOException e) {
                throw new Configurable.ConfigurationException(
                        "Exception creating LuceneIndexDescriptor based "
                        + "on properties", e);
            }
        }
        return descriptor;
    }

    /**
     * Parses a Query-tree and returns it as a human-readable String. This
     * dumper writes custom boosts. Not suitable to feed back into a parser!
     * @param query the query to dump as a String.
     * @return the query as a human-redable String.
     */
    // TODO: Make this dumper more solid - let it handle all known Queries
    public static String queryToString(Query query) {
        StringWriter sw = new StringWriter(100);
        if (query instanceof BooleanQuery) {
            sw.append("(");
            boolean first = true;
            for (BooleanClause clause: ((BooleanQuery)query).getClauses()) {
                if (!first) {
                    sw.append(" ");
                }
                sw.append(clause.getOccur().toString());
                sw.append(queryToString(clause.getQuery()));
                first = false;
            }
            sw.append(")");
        } else if (query instanceof TermQuery) {
            TermQuery termQuery = (TermQuery)query;
            sw.append(termQuery.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof RangeQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof WildcardQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof FuzzyQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof PrefixQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof PhraseQuery) {
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        } else if (query instanceof DisjunctionMaxQuery) {
            Iterator iterator = ((DisjunctionMaxQuery)query).iterator();
            sw.append("<");
            boolean first = true;
            while (iterator.hasNext()) {
                if (!first) {
                    sw.append(" ");
                }
                sw.append(queryToString((Query)iterator.next()));
                first = false;
            }
            sw.append(">");
        } else {
            sw.append(query.getClass().toString());
            sw.append(query.toString()).append("[");
            sw.append(Float.toString(query.getBoost())).append("]");
        }
        return sw.toString();
    }
}




