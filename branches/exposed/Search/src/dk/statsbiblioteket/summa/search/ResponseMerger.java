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

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Merges responses from different searchers based on specified strategies.
 * Intended for federated or fine tuning of distributed integrated search.
 * </p><p>
 * Properties are generally specified both at startup and runtime, where runtime
 * overrides startup. 
 * </p><p>
 * Currently only {@link DocumentResult}s are explicitly processed.
 * Other results are merged with the default merger.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class ResponseMerger {
    private static Log log = LogFactory.getLog(ResponseMerger.class);

    /**
     * The overall way of merging DocumentResponses.
     * </p><p>
     * Optional. Default is 'score'. 
     * @see {@link MERGE_MODE} for details.
     */
    public static final String CONF_MODE = "responsemerger.mode";
    public static final String DEFAULT_MODE = MERGE_MODE.score.toString();
    public static final String SEARCH_MODE = CONF_MODE;
    
    /**
     * After performing a general merge, the resulting list can be 
     * post-processed.
     * </p><p>
     * Optional. Default is 'none'. 
     * @see {@link MERGE_POST} for details.
     */
    public static final String CONF_POST = "responsemerger.post";
    public static final String DEFAULT_POST = MERGE_POST.none.toString();
    public static final String SEARCH_POST = CONF_POST;

    /**
     * Overall merging mode for documents. Note that some of these modes require
     * extra parameters.
     * score:       Direct sort by score.<br/>
     * concatenate: Directly concatenate the lists of responses.
     *              The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER}
     *              must be specified to use this merger.<br/>
     * interleave:  The first documents from all results are inserted after each
     *              other, then the second and so forth.
     *              The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER}
     *              must be specified to use this merger.<br/>
     */
    // TODO: slope (measuring steep drops in scores)
    public static enum MERGE_MODE {
        /**
         * Direct sort by score. This is equivalent to the default merger for
         * DocumentResponses.
         */
        score,
        /**
         * Directly concatenate the lists of responses.
         * The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER} should be
         * specified to use this merger.
         */
        concatenate,
        /**
         * The first documents from all results are inserted after each other,
         * then the second and so forth.
         * The parameters {@link #CONF_ORDER} or {@link #SEARCH_ORDER} should be
         * specified to use this merger.
         */
        interleave
    }

    /**
     * Used with {@link MERGE_MODE#concatenate} and
     * {@link MERGE_MODE#interleave} to specify the order of the sources.
     * </p><p>
     * Optional but highly recommended. The IDs for all possible sources
     * is stated separated by comma. Sources not specified here will be merged
     * after those stated and in order of appearance (i.e. semi-randomly).
     * Example: 'sb, summa, kb'.
     */
    public static final String CONF_ORDER = "responsemerger.order";
    public static final String SEARCH_ORDER = CONF_ORDER;

    /**
     * Post-merge adjustment of the order of the documents.<br/>
     * none:    No post-processing.<br/>
     * enforce: Force a certain number of documents from stated sources to
     *          appear as top-X.
     *          The parameters {@link #CONF_FORCE_TOPX} and
     *          {@link #CONF_FORCE_RULES} must be specified to use this
     *          post-processor.
     */
    public static enum MERGE_POST {none, enforce}

    /**
     * Documents from the sources specified in {@link #CONF_FORCE_RULES} will
     * be forced to appear among the first X documents returned. To make room.
     * documents from sources not specified in the rules will be pushed down
     * in the list, starting from document #X and up.
     * </p><p>
     * Optional. Default is 20.
     */
    public static final String CONF_FORCE_TOPX = "responsemerger.force.topx";
    public static final String SEARCH_FORCE_TOPX = CONF_FORCE_TOPX;
    public static final int DEFAULT_FORCE_TOPX = 20;

    /**
     * The sources of documents to force into the top X documents, specified
     * as a comma-separated list of source IDs with the wanted number in
     * parenthesis. Example: 'sb(3), kb(2)'.
     * </p><p>
     * Optional. Default is none.
     */
    public static final String CONF_FORCE_RULES = "responsemerger.force.rules";
    public static final String SEARCH_FORCE_RULES = CONF_FORCE_RULES;

    private MERGE_MODE defaultMode = MERGE_MODE.valueOf(DEFAULT_MODE);
    private MERGE_POST defaultPost = MERGE_POST.valueOf(DEFAULT_POST);


}
