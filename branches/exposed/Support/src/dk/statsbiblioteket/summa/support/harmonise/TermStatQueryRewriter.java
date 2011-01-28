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
package dk.statsbiblioteket.summa.support.harmonise;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Rewrites queries textually based on term statistics for given targets.
 * Terms in the queries will be assigning weights, simulating a single
 * controlled index with regards to the scores for the returned documents.
 * </p><p>
 * Textual query rewriting is error-prone and incomplete so this should be
 * seen as a "best effort" or "better than the alternative", rather than a
 * authoritative harmonization of separate indexes.
 * </p><p>
 * The runtime arguments to the rewriter can be prefixed with the id for
 * a specific target, using the same principle as
 * {@link dk.statsbiblioteket.summa.support.harmonise.InteractionAdjuster}.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class TermStatQueryRewriter {
    private static Log log = LogFactory.getLog(TermStatQueryRewriter.class);

    /**
     * Holds a list of configurations conforming to {@link Target}.
     */
    public static final String CONF_TARGETS = "queryrewriter.targets";
    public static final String SEARCH_WEIGHT = "queryrewriter.weight";

    public class Target implements Configurable {

        /**
         * The id for the target. Used for specifying search-time adjustments
         * and for loading term statistics.
         */
        public static final String CONF_ID = "target.id";

        /**
         * The relative weight of the stats from this target.
         * </p><p>
         * Sane values go from 0.0 to 1.0, where 0.0 is a complete disregard of
         * the targets effect on the merged term stats and 1.0 is full effect.
         * </p><p>
         * If the value is specified at runtime, it must be prefixed with the
         * target it + ".". Example: {@code sb.target.weight=1.0}.
         * </p><p>
         * Optional. Default is 1.0.
         */
        public static final String SEARCH_WEIGHT = "target.weight";
        public static final String CONF_WEIGHT = "target.weight";
        public static final double DEFAULT_WEIGHT = 1.0d;
    }

}
