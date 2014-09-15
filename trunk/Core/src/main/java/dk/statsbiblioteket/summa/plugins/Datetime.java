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
package dk.statsbiblioteket.summa.plugins;

import dk.statsbiblioteket.util.qa.QAInfo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles transforming of ISO date and time to misc. forms of dates.
 * The canonical input is 2010-11-30T15:58:45+0200 but time offset is ignored
 * and variations such as 20101130 15:58:45 and 2010/11/30-15:58 are accepted.
 */
@SuppressWarnings("MagicConstant") // The month-magic is assumed to be constant
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Datetime {

    // Nice online evaluator at http://regexplanet.com/simple/
    public static final Pattern datePattern =
        Pattern.compile("^([0-9]{4}).?([0-9]{2}).?([0-9]{2}).*");
    public static final Pattern timePattern = // Accepts missing seconds
        Pattern.compile("^[0-9]{4}.?[0-9]{2}.?[0-9]{2}.?([0-9]{2}).?([0-9]{2}).?([0-9]{2})?.*");


    // TODO: Allow more fuzziness
    public static final Pattern solrDayPattern =
            Pattern.compile("[^0-9]*([0-9]{4})[^0-9]?([0-9]{2})[^0-9]?([0-9]{2}).*");
    public static final Pattern solrMonthPattern = Pattern.compile("[^0-9]*([0-9]{4})[^0-9]?([0-9]{2}).*");
    public static final Pattern solrYearPattern = Pattern.compile("[^0-9]*([0-9]{4}).*");
    /**
     * Extremely lenient date/time-parser that produces SOLR-date-time, as expected by TrieDateField.
     * {@see https://lucene.apache.org/solr/4_3_1/solr-core/org/apache/solr/schema/TrieDateField.html}.
     * </p><p>
     * If no date info could be derived, the empty String is returned. If only the year is provided, the full date
     * will be {@code YYYY-01-01T00:00:00Z}.
     * </p><p>
     * Sample inputs: "Tillæg fra 2008 til Andelsposten", "2008-03-05:middag".
     * @param datetime dirty text, hopefully with a prefix for a Solr date.
     * @return Solr date format compliant {@code YYYY-MM-DDTHH:mm:SSZ} or the empty String is no date was extracted.
     */
    public static synchronized String solrDateTime(String datetime) {
        Matcher dayMatcher = solrDayPattern.matcher(datetime);
        if (dayMatcher.matches()) {
            return String.format("%s-%s-%sT00:00:00Z", dayMatcher.group(1), dayMatcher.group(2), dayMatcher.group(3));
        }
        Matcher monthMatcher = solrMonthPattern.matcher(datetime);
        if (monthMatcher.matches()) {
            return String.format("%s-%s-01T00:00:00Z", monthMatcher.group(1), monthMatcher.group(2));
        }
        Matcher yearMatcher = solrYearPattern.matcher(datetime);
        if (yearMatcher.matches()) {
            return String.format("%s-01-01T00:00:00Z", yearMatcher.group(1));
        }
        return "";
    }

    /**
     * Produces tokens with common variations of entering the given date.
     * If the input does not match the pattern {@link #datePattern}, the
     * input string is returned.
     * @param iso    ISO-timestamp such as 2010-11-30T15:58:45+0200.
     * @param locale for locale-specific processing such as month names.
     *               If the locale is empty, "en" is used.
     * @return a space-delimited list of tokens with common ways of writing the
     *         given date. This includes YYYY-MM-DD.
     */
    public static synchronized String dateExpand(String iso, String locale) {
        Matcher matcher = datePattern.matcher(iso);
        if (!matcher.matches()) {
            return iso;
        }
        String year = matcher.group(1);
        String month = matcher.group(2);
        String day = matcher.group(3);

        StringBuilder buffer = new StringBuilder(100);
        buffer.append(year).append("-").append(month).append("-").append(day);
        buffer.append(" ");
        buffer.append(day).append("/").append(month).append("-").append(year);
        buffer.append(" ");
        buffer.append(month).append("/").append(day).append("-").append(year);
        buffer.append(" ");
        buffer.append(day).append("/").append(month).append("/").append(year);
        buffer.append(" ");
        buffer.append(year).append(month).append(day).append(" ");

        buffer.append(day).append("/").append(month).append(" ");
        buffer.append(month).append("/").append(day).append(" ");
        buffer.append(month).append("-").append(day).append(" ");

        if (shorten(day).length() == 1 || shorten(month).length() == 1) {
            buffer.append(shorten(day)).append("/");
            buffer.append(shorten(month)).append(" ");
            buffer.append(shorten(month)).append("/");
            buffer.append(shorten(day)).append(" ");
            buffer.append(shorten(month)).append("-");
            buffer.append(shorten(day)).append(" ");
        }

        Locale loc = locale == null || "".equals(locale) ?
                     new Locale("en") : new Locale(locale);
        Formatter formatter = new Formatter(buffer, loc);
        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(year), Integer.parseInt(month)-1, Integer.parseInt(day));
        formatter.format("%1$tB %1$tb", cal);
        formatter.flush();
        return buffer.toString();
    }

    /**
     * Produces tokens with common variations of entering the given time.
     * If the input does not match the pattern {@link #timePattern}, the
     * input string is returned.
     * @param iso    ISO-timestamp such as 2010-11-30T15:58:45+0200.
     * @param locale currently ignored but might be used in the future.
     * @return a space-delimited list of tokens with common ways of writing the
     *         given time. This includes HH:MM:SS.
     */
    @SuppressWarnings("UnusedParameters")
    public static synchronized String timeExpand(String iso, String locale) {
        Matcher matcher = timePattern.matcher(iso);
        if (!matcher.matches()) {
            return iso;
        }
        String hour = matcher.group(1);
        String minute = matcher.group(2);
        String second = matcher.groupCount() == 3 ? matcher.group(1) : null;

        StringBuilder buffer = new StringBuilder(100);
        buffer.append(hour).append(":").append(minute).append(" ");
        buffer.append(hour).append(".").append(minute).append(" ");
        buffer.append(hour).append(minute).append(" ");
        buffer.append(hour).append("h").append(minute).append("m");
        if (second != null) {
            buffer.append(" ");
            buffer.append(hour).append(":").append(minute).append(":");
            buffer.append(second).append(" ");
            buffer.append(hour).append("h").append(minute).append("m");
            buffer.append(second).append("s");
        }
        return buffer.toString();
    }

    /**
     * Produce a /-divided representation of the given timestamp, usable as
     * input for hierarchical faceting.
     * @param iso   ISO-timestamp such as 2010-11-30T15:58:45+0200.
     * @param start where to start the output in the tokenized list of datetime
                    elements:
                    1=year, 2=month, 3=day, 4=hour, 5=minute, 6=second.
     * @param end   where to end the output in the tokenized list of datetime
                    elements. Indexes correspond to the start parameter and are
                    exclusive.
     * @return YYYY/MM/DD/HH/MM/SS or YYYY/MM/DD if dateOnly is true. If the
     *         iso can not be parsed, the empty string is returned.
     */
    public static synchronized String divide(String iso, int start, int end) {
        Matcher matcher = datePattern.matcher(iso);
        if (!matcher.matches()) {
            return "";
        }
        List<String> tokens = new ArrayList<>(6);
        for (int i = 1 ; i < matcher.groupCount()+1 ; i ++) {
            tokens.add(matcher.group(i));
        }

        Matcher timeMatcher = timePattern.matcher(iso);
        if (timeMatcher.matches()) {
            for (int i = 1 ; i < timeMatcher.groupCount()+1 ; i ++) {
                tokens.add(timeMatcher.group(i));

            }
        }
        StringBuilder buffer = new StringBuilder(100);
        for (int i = start-1 ; i < end && i < tokens.size() ; i++) {
            if (i != start-1) {
                buffer.append("/");
            }
            buffer.append(tokens.get(i));
        }
        return buffer.toString();
    }

    private static String shorten(String s) {
        if (s.length() <= 1 || s.charAt(0) != '0') {
            return s;
        }
        return s.substring(1, s.length());
    }

/*    private static String align2(final int v) {
        return v < 10 ? "0" + Integer.toString(v) : Integer.toString(v);
    }

    private static String align4(final int v) {
        String result = Integer.toString(v);
        while (result.length() < 4) {
            result = "0" + result;
        }
        return result;
    }
     */
    /**
     * Produces tokens with common variations of entering the given date and
     * time. This is a shorthand for calling both {@link #dateExpand} and
     * {@link #timeExpand}.
     * @param iso    ISO-timestamp such as 2010-11-30T15:58:45+0200.
     * @param locale currently ignored but might be used in the future.
     * @return a space-delimited list of tokens with common ways of writing the
     *         given date and time.
     */
    public static String dateAndTimeExpand(String iso, String locale) {
        return dateExpand(iso, locale) + " " + timeExpand(iso, locale);
    }
}
