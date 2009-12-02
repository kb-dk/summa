/* $Id:$
 *
 * The Summa project.
 * Copyright (C) 2005-2009  The State and University Library
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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Locale;

/**
 * The standard Collator from Java 1.5 and 1.6 prioritizes spaces below
 * everything else. The order of {"a b" "aa"} is thus {"aa" "a b"}.
 * In the danish library world and other places, space takes precedence so
 * that the order it {"a b" "aa"}. This factory aims to ensure that Collators
 * adhere to the space-first priority.
 * </p><p>
 * Note: Depending on scenario, it might be advisable to use
 * {@link dk.statsbiblioteket.util.CachedCollator} from sbutil, as it markedly
 * fater at comparing Strings. This comes at the cost of increased construction
 * time.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public class CollatorFactory {
    private static Log log = LogFactory.getLog(CollatorFactory.class);

    /**
     * If the given Collator is a RuleBased Collator, a new Collator is build
     * with rules that prioritizes space before other characters.
     * </p><p>
     * The operation can be repeated without any ill effect.
     * @param collator the base Collator.
     * @return a new Collator with modified behaviour.
     */
    public static Collator fixCollator(Collator collator) {
        return fixCollator(collator, true);
    }

    private static Collator fixCollator(Collator collator, boolean check) {
        if (!(collator instanceof RuleBasedCollator)) {
            log.warn(String.format(
                    "fixCollator expected a RuleBasedCollator but got %s. "
                    + "Unable to update Collator", collator.getClass()));
            return collator;
        }
        String rules = ((RuleBasedCollator)collator).getRules();
        if (check && rules.indexOf("<' '<'\u005f'") == -1) {
            log.debug("fixCollator: The received Collator already sorts spaces"
                      + " first");
            return collator;
        }
        try {
            RuleBasedCollator newCollator = new RuleBasedCollator(
                    rules.replace("<'\u005f'", "<' '<'\u005f'"));
            log.trace("Successfully updated Collator to prioritize spaces "
                      + "before other characters");
            return newCollator;
        } catch (ParseException e) {
            throw new RuntimeException(
                    "ParseException while parsing\n" + rules, e);
        }
    }

    /**
     * Create a Collator from the given Locale, prioritizing space before other
     * characters.
     * @param locale the wanted Locale for the Collator.
     * @return a new Collator.
     */
    // TODO: Make this produce fixed CachedCollators
    public static Collator createCollator(Locale locale) {
        Collator collator = Collator.getInstance(locale);
        return fixCollator(collator, false);
    }
}
