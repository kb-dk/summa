package dk.statsbiblioteket.summa.common.lucene.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;

import java.io.IOException;
import java.io.CharArrayReader;
import java.util.Map;
import java.util.HashMap;

import dk.statsbiblioteket.util.reader.ReplaceReader;
import dk.statsbiblioteket.util.reader.ReplaceFactory;

/**
 * Generic {@link TokenFilter} implementation which can replace substrings
 * inside the tokens based on a set of rules.
 * <p/>
 * Internally it will select the optimal replacement strategy based on the
 * rules it has been given.
 */
public class TransliterationFilter extends TokenFilter {

    /**
     * Transliteration rules for converting common non-ascii characters found
     * in bibliographic records to ascii.
     */
    public static final String CHARACTER_TRANSLITERATIONS = "i > i;£ > £;¦ > |;ª > a;µ > m;º > o;ß > ss;à > a;á > a;" +
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

    /**
     * Rules that translates any non-letter characters that
     * "you can safely ignore" (at your own peril) to empty strings
     */
    public static final String VOID_TRANSLITERATIONS =
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

    /**
     * Rules for converting characters that can be interpreted as white space
     * into the ascii white space character
     */
    public static final String BLANK_TRANSLITERATIONS =
            "'-' > ' ';'.' > ' ';',' > ' ';'/' > ' ';'\\\'' > ' ';';' > ' ';" +
            "'+' > ' ';'=' > ' ';' ' > ' ';' ' > ' ';'–' > ' ';'*' > ' ';" +
            "'†' > ' ';'³' > ' 3';'—' > ' ';'­' > ' ';'×' > ' ';'·' > ' ';" +
            "'→' > ' ';'−' > ' ';'±' > ' ';'²' > ' 2';'。' > ' ';'¼' > '1 4';'¹' > ' 1';'•' > ' ';" +
            "'…' > ' ';'⋅' > ' ';'⁹' > ' 9';'⁴' > ' 4';'⁻' > ' ';'¬' > ' ';'₋' > ' ';'⁶' > ' 6';'½' > '1 2';" +
            "'‡' > ' ';'　' > ' ';'₊' > ' ';'·' > ' ';'↔' > ' ';'⁸' > ' 8';'≈' > ' ';'⁵' > ' 5';'″' > ' ';" +
            "'‐' > ' ';'∫' > ' ';'∗' > ' ';'↓' > ' ';'－' > ' ';'≥' > ' ';'―' > ' ';'━' > ' ';'≤' > ' ';" +
            "' ' > ' ';'∷' > ' ';'⁷' > ' 7';'⊂' > ' ';'∙' > ' ';'‖' > ' ';'‧' > ' ';'‴' > ' ';'ℂ' > ' ';";

    /**
     * Collation of the rules defined in
     * {@link #CHARACTER_TRANSLITERATIONS}, {@link #VOID_TRANSLITERATIONS}, and
     * {@link #BLANK_TRANSLITERATIONS}
     */
    public static final String ALL_TRANSLITERATIONS =
            CHARACTER_TRANSLITERATIONS + VOID_TRANSLITERATIONS + BLANK_TRANSLITERATIONS;


    /**
     * Thread local cache shared by all transliterators.
     * The replace readers might have a non-trivial
     * initialization time, so we keep them around. We use the transliteration
     * rule string as key for each replace reader.
     */
    private static final ThreadLocal<Map<String,ReplaceReader>>
            localReplacerCache = new ThreadLocal<Map<String,ReplaceReader>>() {

        @Override
        protected Map<String,ReplaceReader> initialValue() {
            return new HashMap<String,ReplaceReader>();
        }
    };

    private String rules;

    /**
     * Look up a thread local ReplaceReader
     *
     * @param rules used as a lookup key into the replace reader cache
     * @return a cached, thred local, replace reader
     */
    private static ReplaceReader getLocalReplaceReader(String rules) {
        Map<String, ReplaceReader> replacerCache = localReplacerCache.get();

        ReplaceReader replacer = replacerCache.get(rules);
        if (replacer != null) {
            return replacer;
        }

        replacer = ReplaceFactory.getReplacer(RuleParser.parse(rules));
        replacerCache.put(rules, replacer);
        return replacer;
    }

    @Override
    public Token next(Token tok) throws IOException {
        tok = input.next(tok);

        if (tok == null) {
            return null;
        }

        ReplaceReader replacer = getLocalReplaceReader(rules);
        replacer.setSource(
                new CharArrayReader(tok.termBuffer(),0,tok.termLength()));

        char[] termBuf = tok.termBuffer();
        int totalRead = 0;
        int numRead = 0;
        while ((numRead =
                replacer.read(termBuf, totalRead, termBuf.length - totalRead))
               != -1) {
            totalRead += numRead;
            if (totalRead >= termBuf.length) {
                tok.resizeTermBuffer(2*termBuf.length);
                termBuf = tok.termBuffer();
            }

        }

        tok.setTermLength(totalRead);

        return tok;
    }

    /**
     * Construct a token stream filtering the given input.
     *
     * @param input the token stream to perform transliteration on
     * @param rules the transliteration rules to apply
     * @param keepDefaults whether or not to apply the rules defined
     *                     in {@link #ALL_TRANSLITERATIONS} in addition to
     *                     the rules defined in {@code rules}
     */
    public TransliterationFilter(TokenStream input, String rules,
                                 boolean keepDefaults) {
        super(input);
        if (keepDefaults) {
            if (rules == null || "".equals(rules)) {
                this.rules = ALL_TRANSLITERATIONS;
            } else {
                this.rules = rules + ALL_TRANSLITERATIONS;
            }
        } else if (rules == null) {
            this.rules = "";
        } else {
            this.rules = rules;
        }
    }
}
