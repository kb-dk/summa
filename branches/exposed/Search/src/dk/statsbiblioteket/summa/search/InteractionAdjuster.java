/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2010  The State and University Library
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
package dk.statsbiblioteket.summa.search;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.search.api.Request;
import dk.statsbiblioteket.summa.search.api.Response;
import dk.statsbiblioteket.summa.search.api.ResponseCollection;
import dk.statsbiblioteket.summa.search.api.document.DocumentResponse;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.Serializable;
import java.util.Map;

/**
 * Acts as a transformer of requests and responses. Queries can be rewritten
 * with weight-adjustment of terms, scores for returned documents can be
 * tweaked.
 * </p><p>
 * IMPORTANT: Search-arguments for this adjuster are special as they should be
 * prepended by an identifier that matches the adjuster. If no identifier is
 * given, the argument will be applied to all adjusters.
 * </p><p>
 * Note that there are CONF-equivalents to some SEARCH-arguments. Effects are
 * cumulative.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class InteractionAdjuster implements Configurable {
    private static Log log = LogFactory.getLog(InteractionAdjuster.class);

    /**
     * The id for this search adjuster. All search-time arguments must be
     * prepended with this id and a dot.
     * Example: The id is 'remote_a' and the returned scores should be
     * multiplied by 1.5. The search-argument must be
     * {@code remote_a.adjust.score.multiply=1.5}.
     */
    public static final String CONF_IDENTIFIER = "adjuster.id";

    // TODO: disable facets? empty/null?
    // facet-names
    // field-names (many-to-many) recordID, recordBase
    // tag-names
    // Weight-rewrite from lookup-table (share tables if possible)

    

    /**
     * Add a constant to returned scores for documents.
     * Additions are performed after multiplications.
     */
    public static final String SEARCH_ADJUST_SCORE_ADD = "adjust.score.add";
    public static final String CONF_ADJUST_SCORE_ADD = SEARCH_ADJUST_SCORE_ADD;

    /**
     * Multiply the returned scores for documents with a constant.
     * Multiplications are performed before additions.
     */
    public static final String SEARCH_ADJUST_SCORE_MULTIPLY =
        "adjust.score.multiply";
    public static final String CONF_ADJUST_SCORE_MULTIPLY =
        SEARCH_ADJUST_SCORE_MULTIPLY;

    private final String id;
    private final String prefix;
    private double baseFactor = 1.0;
    private double baseAddition = 0.0;

    public InteractionAdjuster(Configuration conf) {
        id = conf.getString(CONF_IDENTIFIER);
        prefix = id + ".";
        baseFactor = conf.getDouble(CONF_ADJUST_SCORE_MULTIPLY, baseFactor);
        baseAddition = conf.getDouble(CONF_ADJUST_SCORE_ADD, baseAddition);
        log.debug("Constructed search adjuster for '" + id + "'");
    }

    /**
     * Creates a copy of the provided request and rewrites arguments according
     * to settings and request-time arguments, then returns the adjusted
     * request.
     * @param request the unadjusted request.
     * @return an adjusted request.
     */
    public Request rewrite(Request request) {
        Request adjusted = new Request();
        for (Map.Entry<String, Serializable> entry: request.entrySet()) {
            adjusted.put(entry.getKey(), entry.getValue());
        }
        return adjusted;
    }

    public void adjust(Request request, ResponseCollection responses) {
        double factor = baseFactor;
        double addition = baseAddition;
        if (request.containsKey(SEARCH_ADJUST_SCORE_MULTIPLY)) {
            factor *= request.getDouble(SEARCH_ADJUST_SCORE_MULTIPLY);
        }
        if (request.containsKey(prefix + SEARCH_ADJUST_SCORE_MULTIPLY)) {
            factor *= request.getDouble(prefix + SEARCH_ADJUST_SCORE_MULTIPLY);
        }
        if (request.containsKey(SEARCH_ADJUST_SCORE_ADD)) {
            addition += request.getDouble(SEARCH_ADJUST_SCORE_ADD);
        }
        if (request.containsKey(prefix + SEARCH_ADJUST_SCORE_ADD)) {
            addition += request.getDouble(prefix + SEARCH_ADJUST_SCORE_ADD);
        }
        // It is okay to compare as worst case is an unnecessary adjustment
        //noinspection FloatingPointEquality
        if (baseAddition != 0 || baseFactor != 1.0) {
            adjustDocuments(responses, factor, addition);
        }
    }

    /**
     * If the responses contain a {@link DocumentResponse}, the scores for the
     * documents are adjusted with the given factor and addition.
     * @param responses a collection of responses of which DocumentResponse will
     *                  be adjusted.
     * @param factor    multiplied to the document scores.
     * @param addition  added to the document scores after multiplication.
     */
    private void adjustDocuments(
        ResponseCollection responses, double factor, double addition) {
        log.trace("adjustDocuments called with factor " + factor + ", addition "
                  + addition);
        for (Response response: responses) {
            if (!DocumentResponse.NAME.equals(response.getName())) {
                continue;
            }
            if (!(response instanceof DocumentResponse)) {
                log.error("adjustDocuments found response wil name "
                          + DocumentResponse.NAME + " and expected Class "
                          + DocumentResponse.class + " but got "
                          + response.getClass());
                continue;
            }
            DocumentResponse documentResponse = (DocumentResponse) response;
            for (DocumentResponse.Record record: documentResponse.getRecords()){
                record.setScore((float)(record.getScore() * factor + addition));
            }
        }
    }

    public String getId() {
        return id;
    }
}
