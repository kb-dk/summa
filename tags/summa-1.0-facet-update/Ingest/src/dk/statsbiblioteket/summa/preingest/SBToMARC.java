/* $Id: SBToMARC.java,v 1.4 2007/10/05 10:20:24 te Exp $
 * $Revision: 1.4 $
 * $Date: 2007/10/05 10:20:24 $
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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: SBToMARC.java,v 1.4 2007/10/05 10:20:24 te Exp $
 */
package dk.statsbiblioteket.summa.preingest;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.StreamTokenizer;
import java.io.Reader;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Changes SB's almost-but-not-completely-MARC format to
 * even-closer-to-MARC-but-still-not-there format. The difference being that
 * multi volumes is specified by the SB exporter i fields 014z and 015z,
 * while MARC specifies them to be at 014a and 015a.
 *
 * The relevant structures to change are
 * {@code
 * <datafield tag="014" ind1="0" ind2="0">
        <subfield code="a">
 * }
 * to
 * {@code
 * <datafield tag="014" ind1="0" ind2="0">
 *       <subfield code="z">
 * }
 * and
 * {@code
 * <datafield tag="015" ind1="0" ind2="0">
 *       <subfield code="a">
 * }
 * to
 * {@code
 * }
 * <datafield tag="015" ind1="0" ind2="0"
         <subfield code="z">
 * }
 *
 * This filter is optimized towards speed, at the cost of solidness.
 * It is possible to trick it to deliver the wrong output (see
 * {@link#applyFilter(Reader, Writer) for details). However, assuming a valid
 * MARC-XML, the filter should be solid.
 * Reapplying the filter will not introduce further changes. 
 */
@QAInfo(level = QAInfo.Level.NORMAL,
       state = QAInfo.State.IN_DEVELOPMENT,
       author = "te")
public class SBToMARC implements IngestFilter {
    private static Log log = LogFactory.getLog(IngestFilter.class);

    /**
     * Wrapper for {@link #applyFilter(Reader, Writer).
     * @param input    the file with the input data.
     * @param ext      the output file will use the input + delimiter + ext as
     *                 the file name.
     * @param encoding the character encoding for the input.
     */
    public void applyFilter(File input, Extension ext, String encoding) {
        try {
            Reader in = new InputStreamReader(
                    new FileInputStream(input), encoding);
            File output = new File(input.getAbsolutePath() + "." + ext);
            Writer out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(output), encoding));
            applyFilter(in, out);
            out.close();
            in.close();
        } catch (FileNotFoundException e) {
            log.error("Could not find file \"" + input + "\"", e);
        } catch (UnsupportedEncodingException e) {
            log.error("Unsupported encoding: \"" + encoding + "\"");
        } catch (IOException e) {
            log.error("Could not read or write stream: " + e.getMessage(), e);
        }
    }

    // TODO: Make this handle "<foo<". This could be done by making a custom reader
    // This reader assumes MARC and does not handle non-well-formed inputs well
    /**
     * Filters the in stream according to the rules stated in the class
     * description and writes the result to out. The filter is meant to be
     * non-destructive, so no special assumptions are made about the input.
     * Note: The implementation is not solid, so it can be tricked to producing
     * a wrong output. Known errors are:
     * - The input needs to end in '>'.
     * - Multiple '<' in succession will be collapsed to a single '<'.
     * - Nesting inside 014 and 015 datafields with a structure such as
     *  <datafield tag="014">
     *  <subthingie><subfield code="z"></subthingie>
     *  </datafield>
     *   will incorrectly convert the nested subfield code to 'a'.
     * @param in           the stream to filter.
     * @param out          the output stream.
     * @throws IOException if either the in or out could not be accessed.
     */
    public void applyFilter(Reader in, Writer out) throws IOException {
        BufferedReader bin = new BufferedReader(in);
        bin.mark(10);
        boolean firstToken = bin.read() != '<';
        bin.reset();


        StreamTokenizer tokenizer = new StreamTokenizer(bin);
        tokenizer.resetSyntax();
        tokenizer.wordChars(0, Integer.MAX_VALUE);
        tokenizer.whitespaceChars('<', '<');
        int tokenType = tokenizer.nextToken();
        boolean linkDatafield = false;
        while (tokenType != StreamTokenizer.TT_EOF) {
            if (firstToken) {
                firstToken = false;
            } else {
                out.write('<');
            }
            String token = tokenizer.sval;
            if (token.startsWith("datafield")) {
                linkDatafield = token.contains("tag=\"014\"") ||
                                token.contains("tag=\"015\"");
            } else if (linkDatafield && token.startsWith("subfield")) {
                if (token.contains("code=\"z\"")) {
                    token = token.replace("code=\"z\"", "code=\"a\"");
                }
            } else if (token.startsWith("/datafield")) {
                linkDatafield = false;
            }
            out.write(token);
            tokenType = tokenizer.nextToken();

        }
    }
}



