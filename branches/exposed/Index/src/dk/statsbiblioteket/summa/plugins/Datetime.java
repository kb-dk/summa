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
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles transforming of ISO date and time to misc. forms of dates.
 * The canonical input is 2010-11-30T15:58:45+0200 but time offset is ignored
 * and variations such as 20101130 15:58:45 and 2010/11/30-15:58 are accepted.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class Datetime {
    private static Log log = LogFactory.getLog(Datetime.class);

    // Nice online evaluator at http://regexplanet.com/simple/
    public static final Pattern datePattern =
        Pattern.compile("^([0-9]{4}).?([0-9]{2}).?([0-9]{2}).*");
    public static final Pattern timePattern = // Accepts missing seconds
        Pattern.compile("^[0-9]{4}.?[0-9]{2}.?[0-9]{2}.?([0-9]{2}).?([0-9]{2}).?([0-9]{2})?");

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
    public static String dateExpand(String iso, String locale) {
        Matcher matcher = datePattern.matcher(iso);
        if (!matcher.matches()) {
            return iso;
        }

        // TODO: Implement this
        return null;
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
    public static String timeExpand(String iso, String locale) {
        Matcher matcher = timePattern.matcher(iso);
        if (!matcher.matches()) {
            return iso;
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));

        int second = matcher.groupCount() == 3 ?
                     Integer.parseInt(matcher.group(1)) :
                     -1;
        // TODO: Implement this
        return null;
    }

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
