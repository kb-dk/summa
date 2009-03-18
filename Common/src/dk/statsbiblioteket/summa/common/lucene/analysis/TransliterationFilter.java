package dk.statsbiblioteket.summa.common.lucene.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Token;

import java.io.IOException;
import java.io.Reader;
import java.io.CharArrayReader;
import java.util.Map;
import java.util.HashMap;

import dk.statsbiblioteket.util.reader.ReplaceReader;
import dk.statsbiblioteket.util.reader.ReplaceFactory;

/**
 *
 */
public class TransliterationFilter extends TokenFilter {

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

        public static final String BLANK_TRANSLITERATIONS =
                "'-' > ' ';'.' > ' ';',' > ' ';'/' > ' ';'\\\'' > ' ';';' > ' ';" +
                "'+' > ' ';'=' > ' ';'\\\\' > ' ';' ' > ' ';' ' > ' ';'–' > ' ';'*' > ' ';" +
                "'†' > ' ';'' > ' ';'³' > ' 3';'—' > ' ';'­' > ' ';'×' > ' ';'·' > ' ';" +
                "'→' > ' ';'−' > ' ';'±' > ' ';'²' > ' 2';'。' > ' ';'¼' > '1 4';'¹' > ' 1';'•' > ' ';" +
                "'…' > ' ';'⋅' > ' ';'⁹' > ' 9';'⁴' > ' 4';'⁻' > ' ';'¬' > ' ';'₋' > ' ';'⁶' > ' 6';'½' > '1 2';" +
                "'‡' > ' ';'　' > ' ';'₊' > ' ';'·' > ' ';'↔' > ' ';'⁸' > ' 8';'≈' > ' ';'⁵' > ' 5';'″' > ' ';" +
                "'‐' > ' ';'∫' > ' ';'∗' > ' ';'↓' > ' ';'－' > ' ';'≥' > ' ';'―' > ' ';'━' > ' ';'≤' > ' ';" +
                "' ' > ' ';'∷' > ' ';'⁷' > ' 7';'⊂' > ' ';'∙' > ' ';'‖' > ' ';'‧' > ' ';'‴' > ' ';'ℂ' > ' ';";

        private static final String ALL_TRANSLITERATIONS =
                DEFAULT_TRANSLITERATIONS + VOID_TRANSLITERATIONS + BLANK_TRANSLITERATIONS;


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

    public Token next(Token tok) throws IOException {
        tok = input.next(tok);

        if (tok == null) {
            return null;
        }

        ReplaceReader replacer = getLocalReplaceReader(rules);
        replacer.setSource(
                new CharArrayReader(tok.termBuffer(),0,tok.termLength()));

        char[] termBuf = tok.termBuffer();
        int count = 0;
        int codePoint;
        while ((codePoint = replacer.read()) != -1) {
            if (count >= termBuf.length) {
                tok.resizeTermBuffer(2*termBuf.length);
                termBuf = tok.termBuffer();
            }

            termBuf[count] = (char)codePoint;
            count++;
        }

        tok.setTermLength(count);

        return tok;
    }

    /**
     * Construct a token stream filtering the given input.
     * @param input
     * @param rules
     * @param keepDefaults
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
            this.rules = ALL_TRANSLITERATIONS;
        } else {
            this.rules = rules;
        }
    }
}
