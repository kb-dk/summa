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
package dk.statsbiblioteket.summa.common.lucene.analysis;

import java.util.HashMap;
import java.util.Map;
import java.io.StringReader;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This is a simple RuleParser, used for reading mapping rules found in
 * {@link Rules}.
 * <p/>
 * The rules should follow this notation:
 * <pre>
 * rule = statement > statement;
 * statement = 'char';
 * char = ((character - specialChar) | (escape specialChar))*;
 * specialChar = ' or ; or > or \;
 * escape = \;
 * </pre>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class RuleParser {

    // Cache used to store parsed ruleMaps.
    private static Map<String,Map<String,String>> ruleCache =
                                       new HashMap<String,Map<String,String>>();

    /**
     * Parses a String containing rules, a rule is a mapping pair of Strings.
     * The maximun lenght for any String found in the rules String must not
     * exceed 64 charaters.
     *
     * @param rules - the rules to parse.
     * @return a Map representation of the rules where keys are to mapped onto
     *         the value.
     */
    public static Map<String,String> parse(String rules){
        return parse(rules, 64);
    }

    /**
     * Parses a String containing rules, a rule is a mapping pair of Strings.
     * The maximun lenght for any String found in the rules String must not
     * exceed the maxLen argument.
     *
     * @param rules - the rules to parse.
     * @param maxLen  - the maxLen of any key or value found in the rules.
     * @return a Map representation of the rules where keys are to mapped onto
     *         the value.
     * @throws IllegalArgumentException if the rules cannot be parsed.
     */
    public static Map<String,String> parse(String rules, int maxLen){

        Map<String, String> ruleMap;

        // Check if we have the ruleMap cached, and return it if so
        ruleMap = getCachedRuleMap(rules, maxLen);
        if (ruleMap != null) {
            return ruleMap;
        }

        // Ok, we didn't have the ruleMap parsed yet. Go for it
        ruleMap = new HashMap<String, String>();
        char[] keyByf = new char[maxLen];
        char[] valBuf = new char[maxLen];

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
                if (keyBuf && inM ||
                    keyBuf && val != ign && val != l && val != m && !inM) {
                    if (!inM || inM && val != m) {
                        keyByf[keyPoint++] = val;
                    }
                } else if (inM && val != m ||
                           !inM && val != ign && val != r &&
                           val != l && val != m) {
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
             throw new IllegalArgumentException("The rules could not be parsed",
                                                e);
        }

        cacheRuleMap(rules, maxLen, ruleMap);

        return ruleMap;
    }

    private static void cacheRuleMap(String rules,
                                     int maxLen,
                                     Map<String,String> ruleMap) {
        ruleCache.put(getRuleMapKey(rules, maxLen), ruleMap);
    }

    private static Map<String,String> getCachedRuleMap(String rules,
                                                       int maxLen) {
        return ruleCache.get(getRuleMapKey(rules, maxLen));
    }

    private static String getRuleMapKey(String rules, int maxLen) {
        return maxLen + "_" + rules;
    }

    public static String sanitize(String rules,
                                  boolean keepDefaults, String defaults) {
        if (keepDefaults) {
            if (rules == null || "".equals(rules)) {
                return defaults;
            } else {
                return rules + defaults;
            }
        } else if (rules == null) {
            return "";
        } else {
            return rules;
        }
    }
}




