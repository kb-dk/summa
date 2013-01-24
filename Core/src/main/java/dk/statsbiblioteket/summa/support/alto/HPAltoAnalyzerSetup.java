/* $Id:$
 *
 * WordWar.
 * Copyright (C) 2012 Toke Eskildsen, te@ekot.dk
 *
 * This is confidential source code. Unless an explicit written permit has been obtained,
 * distribution, compiling and all other use of this code is prohibited.    
  */
package dk.statsbiblioteket.summa.support.alto;

import dk.statsbiblioteket.summa.common.configuration.Configurable;
import dk.statsbiblioteket.summa.common.configuration.Configuration;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 *
 */
public class HPAltoAnalyzerSetup implements Configurable {
    private static Log log = LogFactory.getLog(HPAltoAnalyzerSetup.class);

    /**
     * If specified, this setup will only be used for Altos with a date that is on or after the stated date.
     * </p><p>
     * ISO date (YYYYMMDD). Optional. Default is 00000101.
     * </p>
     */
    public static final String CONF_FROM_DATE = "hpaltoanalyzer.date.from";
    public static final String DEFAULT_FROM_DATE = "00000101";

    /**
     * If specified, this setup will only be used for Altos with a date that is before the stated date.
     * </p><p>
     * ISO date (YYYYMMDD). Optional. Default is 99991231.
     * </p>
     */
    public static final String CONF_TO_DATE = "hpaltoanalyzer.date.to";
    public static final String DEFAULT_TO_DATE = "99991231";

    /**
     * If true, Segments that has no time are merged into the previous segment, if it has time.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_MERGE_SUBSEQUENT_NOTIME = "hpaltoanalyzer.merge.subsequent";
    public static final boolean DEFAULT_MERGE_SUBSEQUENT_NOTIME = true;

    /**
     * If a Segment has no end time, the start time from the next Segment with a start time will be used.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_CONNECT_TIMES = "hpaltoanalyzer.connect.times";
    public static final boolean DEFAULT_CONNECT_TIMES = true;

    /**
     * If true, any leftover floating TextBlocks after standard Segment construction will be attach to the Segments
     * they are closest to, measured from middle of the right side of the segment.
     * </p><p>
     * Optional. Default is true.
     */
    public static final String CONF_ATTACH_FLOATERS = "hpaltoanalyzer.floaters.attach";
    public static final boolean DEFAULT_ATTACH_FLOATERS = true;

    /**
     * When calculating the distance between two points, the horizontal distance will be multiplied with this factor.
     * Stating a value below 1 means that vertical distance is more significant..
     * </p><p>
     * Optional. Default is 0.5.
     */
    public static final String CONF_HDIST_FACTOR = "hpaltoanalyzer.hdist.factor";
    public static final double DEFAULT_HDIST_FACTOR = 0.5d;

    private final boolean mergeSubsequent;
    private final boolean connectTimes;
    private final boolean attachFloaters;
    private final double hdistFactor;
    private final String fromDate;
    private final String toDate;

    public HPAltoAnalyzerSetup(Configuration conf) {
        mergeSubsequent = conf.getBoolean(CONF_MERGE_SUBSEQUENT_NOTIME, DEFAULT_MERGE_SUBSEQUENT_NOTIME);
        connectTimes = conf.getBoolean(CONF_CONNECT_TIMES, DEFAULT_CONNECT_TIMES);
        attachFloaters = conf.getBoolean(CONF_ATTACH_FLOATERS, DEFAULT_ATTACH_FLOATERS);
        hdistFactor = conf.getDouble(CONF_HDIST_FACTOR, DEFAULT_HDIST_FACTOR);
        fromDate = conf.getString(CONF_FROM_DATE, DEFAULT_FROM_DATE);
        toDate = conf.getString(CONF_TO_DATE, DEFAULT_TO_DATE);
    }

    public boolean doMergeSubsequent() {
        return mergeSubsequent;
    }

    public boolean doConnectTimes() {
        return connectTimes;
    }

    public boolean doAttachFloaters() {
        return attachFloaters;
    }

    public double getHdistFactor() {
        return hdistFactor;
    }

    public boolean fitsDate(Alto alto) {
        String altoDate = HPAltoAnalyzer.getDateFromFilename(alto.getFilename());
        if (altoDate == null) {
            log.warn("Unable to extract date from " + alto + ". Alto is qualified as not fitting date");
            return false;
        }
        return ((fromDate == null || fromDate.compareTo(altoDate) <= 0)
                        && (toDate == null || toDate.compareTo(altoDate) > 0));
    }

    @Override
    public String toString() {
        return "HPAltoAnalyzerSetup(fromDate='" + fromDate + '\'' + ", toDate='" + toDate + '\''
                + ", mergeSubsequent=" + mergeSubsequent + ", connectTimes=" + connectTimes
                + ", hdistFactor=" + hdistFactor + ')';
    }
}
