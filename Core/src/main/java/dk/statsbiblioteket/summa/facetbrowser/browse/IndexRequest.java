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
package dk.statsbiblioteket.summa.facetbrowser.browse;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.facetbrowser.api.IndexKeys;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Locale;

/**
 * Representation of a request for an index lookup.
 * </p><p>
 * The public constructor for the IndexRequest only accepts a subset of the
 * request attributes and is meant to create a default object. Concrete requests
 * should be generated from the default object with the method
 * {@link #createRequest}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class IndexRequest implements Serializable {
    private static final long serialVersionUID = 798434168190L;

    private static Log log = LogFactory.getLog(IndexRequest.class);

    /**
     * Specifies whether the search is case-sensitive or not.
     * This can be overwritten in the query.
     * </p><p>
     * Optional. If no value is specified, false used.
     */
    public static final String CONF_INDEX_CASE_SENSITIVE = "search.index.default.casesensitive";
    public static final boolean DEFAULT_INDEX_CASE_SENSITIVE = false;

    /**
     * The delta, relative to the origo derived from the given term, to the
     * start-position for the index. This is normally 0 or negative.
     * This can be overwritten in the query.
     * </p><p>
     * Optional. If no value is specified, -5 is used.
     */
    public static final String CONF_INDEX_DELTA = "search.index.default.delta";
    public static final int DEFAULT_INDEX_DELTA = -5;

    /**
     * The maximum length of the index to return. This can be overwritten in
     * the query.
     * </p><p>
     * Optional. If no value is specified, 20 is used.
     */
    public static final String CONF_INDEX_LENGTH = "search.index.default.length";
    public static final int DEFAULT_INDEX_LENGTH = 20;

    /**
     * The absolute maximum for the index to return. The length given in the
     * query will be reduced to be equal to or below the length limit.
     * Optional. Default is 10000.
     */
    public static final String CONF_INDEX_LENGTHLIMIT = "search.index.lengthlimit";
    public static final int DEFAULT_INDEX_LENGTHLIMIT = 10000;

    /**
     * The minimum number of documents that must contain the term for the term
     * to be returned.
     * </p><p>
     * Optional. Default is 0.
     */
    public static final String CONF_INDEX_MINCOUNT = "search.index.mincount";
    public static final int DEFAULT_INDEX_MINCOUNT = 0;


    // Taken directly from {@link IndexKeys}.
    private String query = null;
    private String field = null;
    private String term = null;
    private boolean caseSensitive = DEFAULT_INDEX_CASE_SENSITIVE;
    private int delta = DEFAULT_INDEX_DELTA;
    private int length = DEFAULT_INDEX_LENGTH;
    private Locale locale = null;
    private int minCount = DEFAULT_INDEX_MINCOUNT;

    private int lengthLimit = DEFAULT_INDEX_LENGTHLIMIT;
    private boolean valid = false;

    /**
     * Create a default request object that is to be used for creating concrete
     * requests.
     *
     * @param conf the configuration for the default request.
     */
    public IndexRequest(Configuration conf) {
        log.trace("Creating default index request object");
        caseSensitive = conf.getBoolean(CONF_INDEX_CASE_SENSITIVE, caseSensitive);
        delta = conf.getInt(CONF_INDEX_DELTA, delta);
        length = conf.getInt(CONF_INDEX_LENGTH, length);
        lengthLimit = conf.getInt(CONF_INDEX_LENGTHLIMIT, lengthLimit);
        minCount = conf.getInt(CONF_INDEX_MINCOUNT, minCount);
        log.debug(String.format(Locale.ROOT, "Created default index request with caseSensitive=%b, delta=%d,"
                                + " length=%d, lengthLimit=%d, minCount=%d", 
                                caseSensitive, delta, length, lengthLimit, minCount));
    }

    IndexRequest(String query, String field, String term, boolean caseSensitive, int delta, int length, int minCount) {
        this.query = query;
        this.field = field;
        this.term = term;
        this.caseSensitive = caseSensitive;
        this.delta = delta;
        this.length = Math.min(length, lengthLimit);
        this.minCount = minCount;
        valid = true;
        log.debug(String.format(Locale.ROOT,
                "Created index request with field='%s', term='%s', caseSensitive=%b, delta=%d, length=%d", 
                field, term, caseSensitive, delta, length));
    }

    /**
     * Extract IndexRequest-specific parameters from the generic request and
     * creates a new IndexRequest based on the defaults paired with the concrete
     * parameters.
     * </p><p>
     * Note that this method is also usable for determining if an index lookup
     * should be performed at all, as it returns null if the generic request
     * does not contain the keys necessary to build an index request.
     *
     * @param request a generic Search request.
     * @return an IndexRequest based on defaults and request or null, if no
     *         valid IndexRequest could be created.
     */
    public IndexRequest createRequest(Request request) {
        log.trace("createRequest called");
        String query = request.getString(IndexKeys.SEARCH_INDEX_QUERY, null);
        String field;
        String term;
        if ((field = request.getString(IndexKeys.SEARCH_INDEX_FIELD, null)) == null) {
            log.trace("No field specified in createRequest");
            return null;
        }
        if ((term = request.getString(IndexKeys.SEARCH_INDEX_TERM, null)) == null) {
            log.trace("No term specified in createRequest, assigning \"\"");
            term = "";
        }
        IndexRequest ir = new IndexRequest(
                query, field, term, request.getBoolean(IndexKeys.SEARCH_INDEX_CASE_SENSITIVE, caseSensitive), 
                request.getInt(IndexKeys.SEARCH_INDEX_DELTA, delta),
                request.getInt(IndexKeys.SEARCH_INDEX_LENGTH, length), 
                request.getInt(IndexKeys.SEARCH_INDEX_MINCOUNT, minCount));
        if (request.containsKey(IndexKeys.SEARCH_INDEX_LOCALE)) {
            ir.setLocale(new Locale(request.getString(IndexKeys.SEARCH_INDEX_LOCALE)));
        }
        return ir;
    }

    private void checkValid() {
        if (!valid) {
            throw new IllegalStateException("This IndexRequest is not valid. The method createRequest "
                                            + "must be used for the creation of IndexRequests meant for lookups");
        }
    }

    @Override
    public String toString() {
        return "IndexRequest(query='" + query + "', field=" + field + ", term='" + term + "', locale=" + locale
               + ", delta=" + delta + ", length=" + length + ", minCount=" + minCount + ")";
    }

    // temporary hack to merge new exposed with old Summa
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /* Getters */

    public String getQuery() {
        return query;
    }

    public String getField() {
        checkValid();
        return field;
    }

    public String getTerm() {
        checkValid();
        return term;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public int getDelta() {
        return delta;
    }

    public int getLength() {
        return length;
    }

    public Locale getLocale() {
        return locale;
    }

    public int getMinCount() {
        return minCount;
    }
}
