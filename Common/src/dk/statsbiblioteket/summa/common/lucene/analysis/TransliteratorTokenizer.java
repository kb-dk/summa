/* $Id: TransliteratorTokenizer.java,v 1.5 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/04 13:28:17 $
 * $Author: te $
 *
 * The Summa project.
 * Copyright (C) 2005-2007  The State and University Library
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
package dk.statsbiblioteket.summa.common.lucene.analysis;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.Tokenizer;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.Reader;
import java.io.CharArrayWriter;
import java.io.FilterReader;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.reader.ReplaceFactory;
import dk.statsbiblioteket.util.reader.ReplaceReader;

/**
 * The TransliteratorTokenizer tokenizes a Reader on white spaces AFTER the
 * content is converted to lowercase and parsed through the transliterator rules
 * - be aware on how your transliterator rules is defined - transliteration
 * might introduce white spaces.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal",
        comment = "Methods needs Javadoc, indentation needs work, but beware "
                  + "bad IDEA text-handling for transliteration strings."

                  + "As this is basically a map from char => char[], the "
                  + "obvious optimization is to make the array char[][], where "
                  + "the first dimension is the char and the second index is"
                  + "the array. Lookups for 'b' would this be the code"
                  + "return index[(int)'b'). To guard against out of bounds, "
                  + "this should have an explicit check or be wrapped with "
                  + "try-catch.")
public class TransliteratorTokenizer extends Tokenizer {

    /**
     * A small hack to allow zero-allocation access to the internal buffer
     * of a StringBuilder like thingie
     */
    private static class PeekableCharArrayWriter extends CharArrayWriter {
        public char[] peekInternalArray() {
            return buf;
        }

        public int peekCharCount() {
            return count;
        }
    }

    /**
     * A Reader impl. that converts characters to lower case on the fly
     */
    private static class LowerCasingReader extends FilterReader

    {
        protected LowerCasingReader(Reader in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            return Character.toLowerCase(in.read());
        }
    }

    private static final ThreadLocal<Map<String,ReplaceReader>>
            localReplacerCache = new ThreadLocal<Map<String,ReplaceReader>>() {

        @Override
        protected Map<String,ReplaceReader> initialValue() {
            return new HashMap<String,ReplaceReader>();
        }
    };

    /**
     *  The DEFAULT_TRANSLITERATIONS is a String 
     */
    public static final String DEFAULT_TRANSLITERATIONS = "i > i;£ > £;¦ > |;ª > a;µ > m;º > o;ß > ss;à > a;á > a;" +
            "â > a;ã > a;ä > æ;å > aa;æ > æ;ç > c;è > e;é > e;ê > e;ë > e;ì > i;í > i;î > i;" +
            "ï > i;ð > d;ñ > n;ò > o;ó > o;ô > o;õ > o;ö > ø;ø > ø;ù > u;ú > u;û > u;ü > y;ý > y;þ > th;" +
            "ÿ > y;ā > a;ă > a;ą > a;ć > c;ĉ > c;ċ > c;č > c;ď > d;đ > d;ē > e;ĕ > e;ė > e;ę > e;ě > e;" +
            "ĝ > g;ğ > g;ġ > g;ģ > g;ĥ > h;ĩ > i;ī > i;ĭ > i;į > i;ı > i;ĵ > j;ķ > k;ĸ > k;ļ > l;ľ > l;" +
            "ł > l;ń > n;ņ > n;ň > n;ō > o;ŏ > o;ő > o;œ > oe;ŕ > r;ŗ > r;ř > r;ś > s;ŝ > s;ş > s;š > s;" +
            "ţ > t;ť > t;ũ > u;ū > u;ŭ > u;ů > u;ű > u;ų > u;ŷ > y;ź > z;ż > z;ž > z;ƒ > f;ơ > o;ư > u;" +
            "ǣ > æ;ǧ > g;ǩ > k;ȩ > e;ɛ > t; '˜' > '~';ά > a;α > a;β > b;γ > g;δ > d;ε > e;ζ > z;η > e;θ > th;" +
            "ι > i;κ > k;λ > l;μ > m;ν > n;ξ > x;π > p;ρ > r;ς > s;σ > s;τ > t;υ > u;φ > ph;" +
            "χ > ch;ψ > ps;ω > o;ϕ > ph;ϖ > p;г > g;к > k;о > o;с > s;є > ie;і > i;ḡ > g;ṣ > s;ẽ > e;" +
            "⁰ > 0;₀ > 0;₁ > 1;₂ > 2;₃ > 3;₄ > 4;₅ > 5;₆ > 6;₇ > 7;₈ > 8;₉ > 9;™ > tm;ⅰ > 1;∂ > d;♭ > b;" +
            "ｃ > c;ｅ > e;ｉ > i;ｌ > l;ｍ > m;ｓ > s;";

    public static final String isNull =
            "'_' > '';':' > '';'(' > '';')' > '';\" > '';'[' > '';']' > '';'?' > '';'ʹ' > '';'!' > '';'<' > '';" +
                    "'̣' > '';'̈' > '';'ʻ' > '';'̲' > '';'}' > '';'{' > '';'́' > '';'̄' > '';" +
                    "'̌' > '';'̇' > '';'̨' > '';'ˇ' > '';'ʾ' > '';'̀' > '';'̊' > '';'̆' > '';" +
                    "'’' > '';'ʺ' > '';'»' > '';'«' > '';'′' > '';'̃' > '';\u0096 > '';'”' > '';'©' > '';'¿' > '';'̂' > '';" +
                    "'`' > '';'̦' > '';'̧' > '';'⃞' > '';'“' > '';'︠' > '';'︡' > '';'˘' > '';'´' > '';'¡' > '';\u0093 > '';" +
                    "'®' > '';'̕' > '';'̜' > '';'̋' > '';\u009C > '';'̤' > '';'¸' > '';'̳' > '';'¨' > '';\u0091 > '';'˙' > '';" +
                    "'̮' > '';'̉' > '';\u0084 > '';\u0080 > '';'˚' > '';\u0092 > '';'¯' > '';\u007F > '';'（' > '';" +
                    "'）' > '';'《' > '';'》' > '';'˛' > '';'¶' > '';'ь' > '';\u0097 > '';'‘' > '';'ʿ' > '';'̥' > '';\u009A > '';" +
                    "\u0085 > '';\u0082 > '';'ъ' > '';'、' > '';'，' > '';'̅' > '';\u009D > '';\u0086 > '';'̐' > '';\u0099 > '';" +
                    "'' > '';\u008A > '';'' > '';\u009E > '';'ˆ' > '';\u008C > '';'̓' > '';'？' > '';\u008E > '';\u0087 > '';" +
                    "'ǹ' > '';'；' > '';'＂' > '';\u0088 > '';'' > '';';' > '';\u009F > '';'：' > '';\u0089 > '';'˝' > '';'「' > '';" +
                    "\u0083 > '';\u0081 > '';'' > '';'„' > '';'' > '';\u0098 > '';'�' > '';'〉' > '';'」' > '';\u008D > '';";

    public static final String isBlank = "'-' > ' ';'.' > ' ';',' > ' ';'/' > ' ';" +
            "'\\\'' > ' ';" +
            " ';' > ' ';" +
            "'+' > ' ';'=' > ' ';'\\\\' > ' ';' ' > ' ';' ' > ' ';'–' > ' ';'*' > ' ';" +
            "'†' > ' ';'' > ' ';'³' > ' 3';'—' > ' ';'­' > ' ';'×' > ' ';'·' > ' ';" +
            "'→' > ' ';'−' > ' ';'±' > ' ';'²' > ' 2';'。' > ' ';'¼' > '1 4';'¹' > ' 1';'•' > ' ';" +
            "'…' > ' ';'⋅' > ' ';'⁹' > ' 9';'⁴' > ' 4';'⁻' > ' ';'¬' > ' ';'₋' > ' ';'⁶' > ' 6';'½' > '1 2';" +
            "'‡' > ' ';'　' > ' ';'₊' > ' ';'·' > ' ';'↔' > ' ';'⁸' > ' 8';'≈' > ' ';'⁵' > ' 5';'″' > ' ';" +
            "'‐' > ' ';'∫' > ' ';'∗' > ' ';'↓' > ' ';'－' > ' ';'≥' > ' ';'―' > ' ';'━' > ' ';'≤' > ' ';" +
            "' ' > ' ';'∷' > ' ';'⁷' > ' 7';'⊂' > ' ';'∙' > ' ';'‖' > ' ';'‧' > ' ';'‴' > ' ';'ℂ' > ' ';";

    private static final String defaultRules =
                                    DEFAULT_TRANSLITERATIONS + isNull + isBlank;

    private PeekableCharArrayWriter nextToken;

    private int position;
    private int tokenStart;

    /**
     * Constructs a tailored TransliteratorTokenizer on the supplied reader.
     * Rules needs to be supplied in accordance to the RuleParser syntax.
     * Default rules can be replaces by or appended to the given rules,
     * by specifying the boolean keepDefault
     *
     * @see dk.statsbiblioteket.summa.common.lucene.analysis.RuleParser
     * @param reader The reader to be tokenized
     * @param rules  A String containing a set of transliteration rules.
     * @param keepDefault if true, rules are appended to the default rules.
     *                    In case {@code rules} is {@code null} the default
     *                    rules will be applied no matter the value of
     *                    {@code keepDefault}
     */
    public TransliteratorTokenizer(Reader reader,
                                   String rules, boolean keepDefault){
        // This guarantees that our input stream is always in lower case
        super(reader);
        if (keepDefault) {
            if (rules == null || "".equals(rules)) {
                rules = defaultRules;
            } else {
                rules += defaultRules;
            }
        } else if (rules == null) {
            rules = defaultRules; 
        }


        nextToken = new PeekableCharArrayWriter();
        tokenStart = 0;
        position = 0;

        // Fold the internal reader to do transliteration
        input = getLocalReplaceReader(new LowerCasingReader(input), rules);

    }

    /**
     * Look up a thread local ReplaceReader and set its source to
     * {@code source}.
     *
     * @param source the data source for the replace reader
     * @param rules used as a lookup key into the replace reader cache
     * @return a cached, thred local, replace reader
     */
    private static Reader getLocalReplaceReader(Reader source,
                                                String rules) {
        Map<String, ReplaceReader> replacerCache = localReplacerCache.get();

        ReplaceReader replacer = replacerCache.get(rules);
        if (replacer != null) {
            return replacer.setSource(source);
        }

        replacer = ReplaceFactory.getReplacer(new LowerCasingReader(source),
                                              RuleParser.parse(rules));
        replacerCache.put(rules, replacer);
        return replacer;
    }

    /**
     * Return true if {@code codePoint} is a token delimiter
     * @param codePoint the character to check
     * @return true of {@code codePoint} is a token delimiter
     */
    private static boolean isDelimiter(int codePoint){
        // We use code points here instead of chars as a minor optimization
        // since Character.isWhitespace() uses code points natively
        return Character.isWhitespace(codePoint);
    }

    /**
     * Gets the next Token
     * @return always reuses {@code inToken} or return {@code null} if the end
     *         of the stream has been reached
     * @throws IOException
     */
    public Token next(Token inToken) throws IOException {

        tokenStart = position;
        int codePoint;

        inToken.clear();

        while ((codePoint = input.read()) != -1){
            if (nextToken.peekCharCount() == 0) {
                tokenStart = position;
            }

            if (isDelimiter(codePoint)){
                if (nextToken.peekCharCount() > 0){
                    inToken = emitToken(inToken);
                    position++; // Position must be incresead _after_ emitToken
                    return inToken;
                }
            } else {
                nextToken.append((char)codePoint);
                position++;
            }
        }

        if (nextToken.peekCharCount() > 0){
            return emitToken(inToken);
        }

        return null;
    }

    /**
     * Prepare a token representing the current state. The internal nextToken
     * character buffer will be cleared.
     *
     * @param inToken the token to prepare
     * @return always returns {@code inToken}
     */
    private Token emitToken(Token inToken) {
        inToken.setTermBuffer(nextToken.peekInternalArray(),
                              0, nextToken.peekCharCount());
        inToken.setStartOffset(tokenStart);
        inToken.setEndOffset(position);
        nextToken.reset(); // Clear the nextToken buffer
        return inToken;
    }
}


