/* $Id: TokenMasker.java,v 1.4 2007/10/04 13:28:17 te Exp $
 * $Revision: 1.4 $
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

import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;
import java.util.StringTokenizer;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * The TokenMasker is used to mask tokens that otherwise would not searchable.
 * Typically this is tokens that contains characters that would  be removed,
 * transliterated or make the queryParser cough examples are: c++, c#,
 * c*-algebra.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal",
        comment = "Methods needs Javadoc")
public class TokenMasker extends Reader {

    private final boolean ignoreCase;

    private Reader _reader;
    private Reader _org;

    private static final String defaultMask =
            "'c++' > 'cplusplus';"
            + "'c#' > 'csharp';"
            + " 'c* algebra' > 'cstaralgebra';"
            + " 'c*-algebra > 'cstaralgebra'";

    private Map<String, String> maskToken;
    private static final Map<String, String> defaultMaskToken =
            RuleParser.parse(defaultMask);

    //TODO: shoud this throw IOException??
    /**
     * A TokenMasker is a Reader that mask the text read from it.
     *
     * @see dk.statsbiblioteket.summa.common.lucene.analysis.RuleParser
     * @param reader - containing the text to mask.
     * @param rules  - the masking rules, in format parsable to the RuleParser.
     * @param keepDefault - keep the default rules (add the new rules to the
     * exsisting set).
     * @param ignoreCase - treat all text as lowercase.
     * @throws IOException - if something worng when reading the rules.
     */
    public TokenMasker(Reader reader, String rules, boolean keepDefault,
                       boolean ignoreCase) throws IOException {
        if (keepDefault) {
            if (rules == null || "".equals(rules)) {
                maskToken = defaultMaskToken;
            } else {
                maskToken = RuleParser.parse(rules + defaultMask);
            }
        } else {
            maskToken = RuleParser.parse(rules);
        }
        this.ignoreCase = ignoreCase;
        _reader = makeReader(reader);
        _org = reader;
    }

    /**
     * Returns a Reader where text is masked.
     *
     * @param reader
     * @return
     * @throws IOException
     */
    private Reader makeReader(Reader reader) throws IOException {
        StringBuffer masked = new StringBuffer();
        BufferedReader b = new BufferedReader(reader);
        String line;
        while ((line = b.readLine()) != null) {
            StringTokenizer t = new StringTokenizer(line, " ", false);
            while (t.hasMoreTokens()) {
                String tok = t.nextToken();
                String m;
                if (ignoreCase) {
                    m = maskToken.get(tok.toLowerCase());
                } else {
                    m = maskToken.get(tok);
                }
                if (m != null) {
                    masked.append(m).append(' ');
                } else {
                    masked.append(tok).append(' ');
                }
            }
        }
        return new StringReader(masked.toString().trim());
    }


    /**
     * Generates a default TokenMasker upon a TokenStream.
     * @param tokenStream - the tokenStream to mask.
     * @param ignoreCase - treat everything as lowercase.
     */
    public TokenMasker(TokenStream tokenStream, boolean ignoreCase) {
        super(tokenStream);
        maskToken = RuleParser.parse(defaultMask);
        this.ignoreCase = ignoreCase;
    }


    /**
     * Generates a TokenMasker with the provided mask on the TokenStream.
     *
     * @param input - the tokenStream to mask.
     * @param maskRules - the rules to apply.
     * @param ignoreCase - treat text as lowercase.
     */
    public TokenMasker(TokenStream input, String[] maskRules,
                       boolean ignoreCase) {
        super(input);
        this.ignoreCase = ignoreCase;
        this.maskToken = makeMaskToken(maskRules, ignoreCase);
    }

    private Map<String, String> makeMaskToken(String[] maskRules,
                                              boolean ignoreCase) {
        String rules = "";
        for (String s : maskRules) {
            if (ignoreCase) {
                s = s.toLowerCase();
            }
            rules += s + ";";
        }
        return RuleParser.parse(rules, 100);
    }

    /**
     * Generate a TokenMasker on an tokenStream applying the provided maskMap as
     * rules.
     *
     * @param input - the tokenStream to mask
     * @param maskToken - the map defining maskRules
     * @param ignoreCase - treat text as lowercase.
     */
    public TokenMasker(TokenStream input, Map<String, String> maskToken,
                       boolean ignoreCase) {
        super(input);
        this.ignoreCase = ignoreCase;
        this.maskToken = maskToken;
    }

    /**
     * Constructs a TokenMasker which marks words from the input
     * TokenStream that are named in the map.
     * It is crucial that an efficient map implementation is used
     * for maximum performance.
     * @param in - the tokenStrream to mask.
     * @param maskToken - the map containing the rules.
     */
    public TokenMasker(TokenStream in, Map<String, String> maskToken) {
        this(in, maskToken, false);
    }

    /**
     * Read characters into a portion of an array.  This method will block
     * until some input is available, an I/O error occurs, or the end of the
     * stream is reached.
     *
     * @param cbuf Destination buffer
     * @param off  Offset at which to start storing characters
     * @param len  Maximum number of characters to read
     * @return The number of characters read, or -1 if the end of the
     *         stream has been reached
     * @throws java.io.IOException If an I/O error occurs
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        return _reader.read(cbuf, off, len);
    }

    /**
     * Close the stream.  Once a stream has been closed, further read(),
     * ready(), mark(), or reset() invocations will throw an IOException.
     * Closing a previously-closed stream, however, has no effect.
     *
     * @throws java.io.IOException If an I/O error occurs
     */
    public void close() throws IOException {
        _reader.close();
        _org.close();
    }
}

