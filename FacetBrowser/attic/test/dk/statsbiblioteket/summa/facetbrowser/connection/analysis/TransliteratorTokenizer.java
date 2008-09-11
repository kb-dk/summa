/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id$
 */
package dk.statsbiblioteket.summa.facetbrowser.connection.analysis;

import java.util.Map;
import java.io.Reader;
import java.io.IOException;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Token;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Taken from Commons. Use only for testing!
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class TransliteratorTokenizer extends Tokenizer {


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


    private Map<String, String> ruleMap;

    private StringBuffer nextToken;

    private Reader reader;
    private int position;

    /**
     * Constructs a tailored TransliteratorTokenizer on the supplied reader. Rules needs to be
     * supplied in accordance to the RuleParser syntax.
     * Default rules can be replaces by or appended to the given rules,
     * by specifying the boolean keepDefault
     *
     * @param reader The reader to be tokenized
     * @param rules  A String containing a add of transliteration rules.
     * @param keepDefault if true, rules are appended to the default rules.
     */
    public TransliteratorTokenizer(Reader reader, String rules, boolean keepDefault){
        super(reader);
        if (keepDefault) { rules = TransliteratorTokenizer
                .DEFAULT_TRANSLITERATIONS + TransliteratorTokenizer.isNull +
                                                                           TransliteratorTokenizer
                                                                                   .isBlank; }
        ruleMap = RuleParser.parse(rules);
        this.reader = reader;
        nextToken = new StringBuffer();
        position = 0;

    }


    /**
     * Constructs a TransliteratorTokenizer with the default add off rules on the reader.
     *
     * @param reader  The reader to be tokenized
     */
   public TransliteratorTokenizer(Reader reader){
       new TransliteratorTokenizer(reader, "", true);
   }

    /**
     *
     * @param in
     * @return
     */
    private char[] getTransliteration(char in) {
        in = Character.toLowerCase(in);
        String ret = ruleMap.get(""+in);
        return ret == null ? new char[]{in} : ret.toCharArray();
    }

    /**
     *
     * @param in
     * @return
     */
    boolean isTokenChar(char in){
        return Character.isWhitespace(in);
    }

    /**
     * Gets the next Token from the Reader.
     * @return
     * @throws IOException
     */
    public Token next() throws IOException {

        int start = position;
        Token y = null;
        int c;
        while ((c = reader.read()) != -1){
           char[] res = getTransliteration((char)c);
           for (char t : res){
               if (nextToken.length() == 0) {
                   start = position;
               }
               if (isTokenChar(t)){
                  nextToken.trimToSize();
                  if (nextToken.length() > 0){
                    y = new Token(nextToken.toString().trim(), start, position);
                    nextToken = new StringBuffer();
                  }
               } else {
                    nextToken.append(t);
               }
               position++;
           }
           if (y != null) {
               return y;
           }
        }
        if (nextToken.length()> 0){
            y = new Token(nextToken.toString().trim(), start, position);
            nextToken = new StringBuffer();
            return y;
        }
        return null;
    }
}


