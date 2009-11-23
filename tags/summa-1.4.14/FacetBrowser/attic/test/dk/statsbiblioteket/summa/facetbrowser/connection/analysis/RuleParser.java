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
import java.util.HashMap;
import java.io.StringReader;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This is a simple RuleParser, used for reading mapping rulles for the
 * TransliteratorTokenizer.
 *
 * The rules should follow this notation:
 *
 * rule = statement ">" statement ";";
 * statement = "'" char* "'";
 * char = ((character - specialChar) | (escape specialChar))*;
 * specialChar = "'" | ";" | ">" | "\";
 * escape = "\";
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class RuleParser {

    /**
     * Parses a String containing rules, a rule is a mapping pair of Strings.
     * The maximun lenght for any String found in the rules String must not exeed 64 charaters.
     *
     * @param rules - the rules to parse.
     * @return  A Map representation of the rules where keys are to mapped onto the value.
     */
    public static Map<String,String> parse(String rules){
        return RuleParser.parse(rules, 64);
    }

    /**
     * Parses a String containing rules, a rule is a mapping pair of Strings.
     * The maximun lenght for any String found in the rules String must not exeed the maxLen argument
     *
     * @param rules - the rules to parse.
     * @param maxLen  - the maxLen of any key or value found in the rules.
     * @return - A Map representation of the rules where keys are to mapped onto the value.
     * @throws IllegalArgumentException if the rules cannot be parsed.
     */
    public static Map<String,String> parse(String rules, int maxLen){

        char[] keyByf = new char[maxLen];
        char[] valBuf = new char[maxLen];

        HashMap<String,String> ruleMap = new HashMap<String, String>();
        char m = '\'';
        char r = '>';
        char l = ';';
        char ign = ' ';
        char ec = '\\';
        boolean inM = false;
        boolean keyBuf = true;
        boolean es = false;
        boolean esNext = false;


        int i;
        int keyPoint = 0;
        int valPoint = 0;


        StringReader read = new StringReader(rules);
        try{
        while ((i = read.read()) != -1) {
            char val = (char) i;
            if (val == m && !esNext) { // the char is a ping '
                inM = !inM; // switch between in prase -> out frase
            }
            if (inM && val == ec && !esNext) {
                es = !es;
            }

            // the rule is finished here
            if (!inM && val == l) {
                keyBuf = true;
                char[] trVal = new char[valPoint];
                char[] trKey = new char[keyPoint];
                System.arraycopy(valBuf, 0, trVal, 0, valPoint);
                System.arraycopy(keyByf, 0, trKey, 0, keyPoint);
                ruleMap.put(new String(trKey), new String(trVal));
                keyPoint = 0; valPoint = 0;
                keyByf = new char[maxLen]; valBuf = new char[maxLen];
            }
            if (!inM && val == r) {
                keyBuf = false;
            }
            if (es) {
                esNext = true;
                es = false;
            } else if (!esNext) {
                if ((keyBuf && inM) || (keyBuf && val != ign && val != l && val != m && !inM)) {
                    if (!inM || inM && val != m) {
                        keyByf[keyPoint++] = val;
                    }
                } else if ((inM && val != m) || (!inM && val != ign && val != r && val != l && val != m)) {
                    valBuf[valPoint++] = val;
                }
            } else if (esNext) {
                if (keyBuf) {
                    keyByf[keyPoint++] = val;

                } else {
                    valBuf[valPoint++] = val;
                }
                esNext = false;
            }
        }
        } catch (Exception e){
             throw new IllegalArgumentException("The rules could not be parsed: " + e);
        }
        return ruleMap;
    }


}


