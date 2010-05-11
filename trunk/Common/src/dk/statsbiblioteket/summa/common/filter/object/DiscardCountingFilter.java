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
package dk.statsbiblioteket.summa.common.filter.object;

import dk.statsbiblioteket.summa.common.configuration.Configuration;
import dk.statsbiblioteket.summa.common.filter.Payload;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Discards Payloads based on counting, such as all payloads after the first
 * 100 or every second Payload. The primary purpose is to reduce the amount
 * of Payloads for testing and development purposes.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class DiscardCountingFilter extends AbstractDiscardFilter {
    private static Log log = LogFactory.getLog(DiscardCountingFilter.class);

    /**
     * The first x Payloads encountered are discarded.
     * </p><p>
     * Optional. Default is -1 (disabled).
     */
    public static final String CONF_MIN = "discard.count.min";
    public static final int DEFAULT_MIN = -1;

    /**
     * When the given number of Payloads has passed, the rest is discarded.
     * </p><p>
     * Optional. Default is -1 (disabled).
     */
    public static final String CONF_MAX = "discard.count.max";
    public static final int DEFAULT_MAX = -1;

    /**
     * Only the given fraction of Payloads are allowed to pass.
     * </p><p>
     * Optional. Default is 1.0 (disabled).
     */
    public static final String CONF_FRACTION = "discard.count.fraction";
    public static final double DEFAULT_FRACTION = 1.0d;

    private int min = DEFAULT_MIN;
    private int max = DEFAULT_MAX;
    private double fraction = DEFAULT_FRACTION;

    private int encountered = 0;
    private int passed = 0;

    public DiscardCountingFilter(Configuration conf) {
        super(conf);
        min = conf.getInt(CONF_MIN, min);
        max = conf.getInt(CONF_MAX, max);
        if (conf.valueExists(CONF_FRACTION)) {
            fraction = Double.parseDouble(conf.getString(CONF_FRACTION));
        }
        this.feedback = false;
        log.info(String.format(
            "Created discarder witn min=%d, max=%d, fraction=%f",
            min, max, fraction));
    }

    @Override
    protected boolean checkDiscard(Payload payload) {
        encountered++;
        if (min != -1 && encountered < min) {
            return true;
        }
        if (max != -1 && passed >= max) {
            return true;
        }
        if (fraction != DEFAULT_FRACTION) { // == is okay due to constant usage
            int potentials = min == -1 ? encountered : encountered - min;
            double shouldHavePassed = potentials * fraction;
            if (passed > shouldHavePassed) {
                return true;
            }
        }
        passed++;
        return false;
    }
}
