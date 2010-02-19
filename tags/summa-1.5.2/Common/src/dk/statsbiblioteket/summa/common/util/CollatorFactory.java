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
package dk.statsbiblioteket.summa.common.util;

import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
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

    /**
     * Wraps the given collator as a String-comparator.
     * @param collator the collator to wrap.
     * @return the collator as a comparator.
     */
    public static Comparator<String> wrapCollator(final Collator collator) {
        return new Comparator<String>() {
            public final int compare(final String o1, final String o2) {
                return collator.compare(o1, o2);
            }
        };
    }
}

