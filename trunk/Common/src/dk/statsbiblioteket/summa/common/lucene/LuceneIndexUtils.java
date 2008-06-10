/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.statsbiblioteket.summa.common.lucene;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.configuration.Configurable;
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
     * The property-key for the substorage containing the setup for the
     * LuceneIndexDescriptor.
     */
    public static final String CONF_DESCRIPTOR = "summa.index.descriptor-setup";
    /**
     * The subfolder in the index root containing the lucene index.
     * This will be appended to {@link #indexRoot}.
     */
    @SuppressWarnings({"DuplicateStringLiteralInspection"})
    public static final String LUCENE_FOLDER = "lucene";

    /**
     * Get the sub-properties {@link #CONF_DESCRIPTOR} and create a descriptor
     * based on that. If the sub-properties does not exist, the default
     * descriptor is created.
     * @param conf a configuration with the setup for descriptor stored as a
     *             sub-configuration with the key {@link #CONF_DESCRIPTOR}.
     * @return a descriptor usable for indexing (and querying).
     * @throws Configurable.ConfigurationException if there was an error with
     *                                             the configuration.
     */
    public static LuceneIndexDescriptor getDescriptor(Configuration conf) throws
                                           Configurable.ConfigurationException {
        Configuration descConf = null;
        try {
            descConf = conf.getSubConfiguration(CONF_DESCRIPTOR);
        } catch (IOException e) {
            //noinspection DuplicateStringLiteralInspection
            log.error("Exception requesting '" + CONF_DESCRIPTOR
                      + "' from properties");
        } catch (UnsupportedOperationException e) {
            //noinspection DuplicateStringLiteralInspection
            log.debug("The configuration does not support sub-storages. "
                      + "Attempting to use the storage directly");
            descConf = conf;
        }
        LuceneIndexDescriptor descriptor;
        if (descConf == null) {
            log.warn("No '" + CONF_DESCRIPTOR + "' specified in properties. "
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
