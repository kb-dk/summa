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
package dk.statsbiblioteket.summa.support.lucene.search.sort;

import dk.statsbiblioteket.util.CachedCollator;
import dk.statsbiblioteket.util.Streams;
import dk.statsbiblioteket.util.qa.QAInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.Collator;
import java.util.Locale;

/**
 * An abstract localizable comparator for doing sorted searches in Lucene.
 * </p><p>
 * The comparator looks for the files {@code charstats.<languagecode>.dat} and
 * {@code charstats.dat} in that order. If no char statistics are found,
 * it defaults to {@link #summaChars}. The languagecode is a two-letter language
 * code from ISO 639-2. The charset is UTF-8.
 * </p><p>
 * The charstats contains the characters that are safe for comparing
 * char-to-char. If the two Strings to compare only consists of those chars,
 * they can be compared with array-lookups instead of the more costly Collator
 * from Java.
 * </p><p>
 * Note: Contrary to the default Java Collator, spaces are considered
 * significant and sorted before any other characters.
 * </p><p>
 * In order to use the caching, an instance of the comparator should be kept
 * alive between searches.
 * </p><p>
 * Usage:
 * <pre>
 * ReusableSortComparator myCompare = new ReusableSortComparator("da");
 * SortField titleSort           = new SortField("title", myCompare);
 * SortField titleSortReverse    = new SortField("title", myCompare, true);
 * ...
 * Hits hits = searcher.search(query, Sort(titleSort));
 * </pre>
 * </p><p>
 * Implementers should take care to call {@link #checkCacheConsistency} whenever
 * {@link #newComparator} is called.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "te")
public abstract class ReusableSortComparator extends SortComparator {
    private static Log log = LogFactory.getLog(ReusableSortComparator.class);

    protected String language = "NotDefined";
    protected Collator collator;// Field name, document position

    // The last index-version used for building the structures.
    // Constructed by indexVersion + "_" + Integer.toString(maxDoc)
    private String indexVersion = null;

    protected static final String summaChars =
            "eaoi 0ntr1s24cl93857hd6pgum.bfv:xwykj_z/-qASPCXIUø"
            + "NEGæ$>é#Väåö&ü^áāLó~–íãT*@ıç%čâèBM|š—FYêDúàūžñRð"
            + "·Oć−ôë,łβα°±HşīîJõKZQēśδ†ṣōïěğăńýřûė→ì";
    // Unsafe:            "þ×µμγ§ßο∼"
//            + "£ò▿ưκđσơλùειżτę­νπąρœ¤őηǩĸºφ≥ςĭωί³⋅≤иũňţθό∞ή™υź"
//            + "еаέ…²ªW€≈ψ¢нт•↑ľ¾ύχ₂ώр‰űάÿ¹о½ẽ‐ųζů;л'‡ξĩ√⁰¼ﬁĝȩ←";

    /**
     * Create a comparator based on the sorting rules for the given language.
     * @param language A two-letter ISO-639 language code. A list is located at
     *                http://www.loc.gov/standards/iso639-2/php/English_list.php
     */
    public ReusableSortComparator(String language) {
        log.debug("Creating ReusableSortComparator(" + language + ")");
        this.language = language;
        Locale locale = new Locale(language);
        collator = createCollator(locale);
    }

    /**
     * Create a comparator using the given Collator.
     * @param collator The collator to use for sorting.
     */
    public ReusableSortComparator(Collator collator) {
        log.debug("Creating ReusableSortComparator with custom Collator "
                  + collator);
        this.collator = collator;
    }

    /**
     * Attempts to load char statistics and create a
     * {@link dk.statsbiblioteket.util.CachedCollator} from
     * that. If no statistics can be loaded, the CachedCollator uses the default
     * statistics.
     * @param locale The Locale to use for the Collator.
     * @return > collator, preferably based on char statistics.
     */
    protected Collator createCollator(Locale locale) {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            String first = "charstats." + locale.getLanguage() + ".dat";
            String second = "charstats.dat";

            URL url = loader.getResource(first);
            if (url == null) {
                //noinspection DuplicateStringLiteralInspection
                log.debug("Could not locate " + first + ", trying " + second);
                url = loader.getResource(second);
                if (url == null) {
                    //noinspection DuplicateStringLiteralInspection
                    log.debug("Could not locate " + second + ". Defaulting to "
                              + "hardcoded Summa char statistics");
                    return new CachedCollator(locale, summaChars, true);
                }
            }
            InputStream is = url.openStream();
            assert is != null : "The InputStream is should be defined, as we "
                                + "know the URL '" + url + "'";
            ByteArrayOutputStream out = new ByteArrayOutputStream(1000);
            Streams.pipe(is, out);
            Collator collator =
                    new CachedCollator(locale, out.toString("utf-8"), true);
            log.debug("Created CachedCollator based on stored char statistics "
                      + "from '" + url + "'");
            return collator;
        } catch(UnsupportedEncodingException e) {
            log.error("Exception converting to UTF-8, defaulting to hardcoded "
                      + "defaults", e);
            return new CachedCollator(locale, summaChars, true);
        } catch(IOException e) {
            log.error("IOException getting statistics for collator, defaulting "
                      + "to hardcoded defaults", e);
            return new CachedCollator(locale, summaChars, true);
        } catch(Exception e) {
            log.error("Exception getting statistics for collator, defaulting to"
                      + " hardcoded defaults", e);
            return new CachedCollator(locale, summaChars, true);
        }
    }

    // inherit javadocs
    @Override
    public abstract ScoreDocComparator newComparator(
            IndexReader reader, String fieldname)
            throws IOException;

    /**
     * Checks is the reader has changed and thus if cached structures should be
     * discarded. This should be called whenever a ScoreDocComparator is
     * requested.
     * @param reader Te reader that the sorter should work on.
     */
    protected synchronized void checkCacheConsistency(IndexReader reader) {
        String newIndexVersion = indexVersion + "_" + reader.maxDoc();
        if (newIndexVersion.equals(indexVersion)) {
            return;
        }
        indexChanged();
        this.indexVersion = newIndexVersion;
    }

    /**
     * Called when the index has changed and cached structures should be
     * discarded.
     */
    protected abstract void indexChanged();

    @Override
    protected Comparable getComparable(String termtext) {
        throw new UnsupportedOperationException(
                "Not implemented as it should not be called");
    }

    public String getLanguage() {
        return language;
    }
}

