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

import com.ibm.icu.text.RuleBasedCollator;
import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.qa.QAInfo;

import com.ibm.icu.text.Collator;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    /** Local log instance. */
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

    /**
     * Fixes the given collator so that is sorts spaces first.
     * @param collator The collator to fix.
     * @param check True if check should be done.
     * @return Return the fixed collator.
     */
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
        } catch (Exception e) {
            throw new RuntimeException(
                    "Exception while parsing\n" + rules, e);
        }
    }

    /**
     * Takes a RuleBasedCollator and removed special handling of AA, Aa, aA and
     * aa (old danish writing for å). Normally AA is equivalent to Å when
     * sorting with Locale "da". This method removes that rule so that AA is
     * treated as ti A's.
     * @param collator a RuleBasedCollator.
     * @return a Collator that treats double-a's as two a's.
     */
    public static Collator adjustAASorting(Collator collator) {
        String AA = ", AA , Aa , aA , aa";
        if (!(collator instanceof RuleBasedCollator)) {
            log.warn(String.format(
                    "adjustAASorting expected a RuleBasedCollator but got %s. "
                    + "Unable to update Collator", collator.getClass()));
            return collator;
        }
        String rules = ((RuleBasedCollator)collator).getRules();
        if (rules.indexOf(AA) == -1) {
            log.debug("adjustAASorting: The received Collator already treats "
                      + "aa as 2*a");
            return collator;
        }
        try {
            RuleBasedCollator newCollator = new RuleBasedCollator(
                    rules.replace(AA, ""));
            log.trace("adjustAASorting: Successfully updated Collator to "
                      + "treat aa as 2*a");
            return newCollator;
        } catch (ParseException e) {
            throw new RuntimeException(
                "ParseException while parsing\n" + rules, e);
        } catch (Exception e) {
            throw new RuntimeException("Exception while parsing\n" + rules, e);
        }

    }

    /**
     * Create a Collator from the given Locale where punctuation and spaces are
     * ignored when comparing Strings.
     * @param locale the wanted Locale for the Collator..
     * @return a new Collator with tweaked rules.
     * @see #fixCollator(com.ibm.icu.text.Collator)
     */
    // TODO: Make this produce fixed CachedCollators
    public static Collator createCollator(Locale locale) {
        Collator collator = Collator.getInstance(locale);
        if (collator instanceof RuleBasedCollator) {
            // true ignores spaces and punctuation but at SB space is just
            // as significant as letters (and comes before them)
            ((RuleBasedCollator)collator).setAlternateHandlingShifted(false);
        } else {
            log.warn("Expected the ICU Collator to be a "
                     + RuleBasedCollator.class.getSimpleName()
                     + " but got " + collator.getClass());
        }
        return collator;
    }

    public static String getCollatorKey(Locale locale) {
        return "icu_collator_" + locale.toString();
    }

    /**
     * Calls {@link #createCollator(java.util.Locale)} and wraps the Collator as
     * a CachedCollator if cached == true. This increases creation time but
     * makes the Collator markedly faster.
     * </p><p>
     * Note: The CachedCollator will be created with default cache-characters
     * {@link CachedCollator#COMMON_SUMMA_EXTRACTED}.
     * @param locale the wanted Locale for the Collator..
     * @param cache if true, the Collator is cached.
     * @return a new optionally cached cached Collator with tweaked rules.
     * @see #fixCollator(com.ibm.icu.text.Collator)
     * @see #adjustAASorting(com.ibm.icu.text.Collator)
     */
    public static Collator createCollator(Locale locale, boolean cache) {
        return createCollator(
            locale,  CachedCollator.COMMON_SUMMA_EXTRACTED, cache);
    }

    /**
     * Calls {@link #createCollator(java.util.Locale)} and wraps the Collator as
     * a CachedCollator if cached == true. This increases creation time but
     * makes the Collator markedly faster.
     * </p><p>
     * @param locale the wanted Locale for the Collator..
     * @param cacheChars the characters that are considered safe to sort by on
     *                   an individual character basis.
     * @param cache if true, the Collator is cached.
     * @return a new optionally cached cached Collator with tweaked rules.
     * @see #fixCollator(com.ibm.icu.text.Collator)
     * @see #adjustAASorting(com.ibm.icu.text.Collator)
     */
    public static Collator createCollator(
        Locale locale, String cacheChars, boolean cache) {
        return createCollator(locale);
        // TODO: Re-introduce CachedCollator when it has been fixed properly
        //return cache ? new CachedCollator(collator, cacheChars) : collator;
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
