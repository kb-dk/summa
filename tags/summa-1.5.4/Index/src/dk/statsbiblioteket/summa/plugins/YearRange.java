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
package dk.statsbiblioteket.summa.plugins;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.StringWriter;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This plugin is used to generate ranges of years.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class YearRange {

    private static final Log log = LogFactory.getLog(YearRange.class);

    /**
     * A thread local StringBuffer that is reset on each get() request
     */
    private static final ThreadLocal<StringBuffer> threadLocalBuffer =
                                               new ThreadLocal<StringBuffer>() {
        @Override
        protected StringBuffer initialValue() {
            return new StringBuffer();
        }

        @Override
        public StringBuffer get() {
            StringBuffer buf = super.get();
            buf.setLength(0); // clear/reset the buffer
            return buf;
        }

    };


    /**
     * Convert a string to a range of years if possible.<br>
     * The input needs to have <code>in.lenght() == 4</code> and may contain one or two tailing <code>?</code><br>
     * if the input does not meet theese conditions the input is returned unchanged.
     *
     * @param in the string to expand
     * @return  a expanded string representing a year range ex: 199? -> 1990 1991 1992 1993 1994 1995 1996 1997 1998 1999
     */
    public static String makeRange(String in){
        try{
        return doit(in,in);
        } catch (Exception e){
            log.warn(e.getMessage());
            return in;
        }
    }

    /**
     * Make a range expansion between the two inputs. The <code>?<code>
     * character can be used as a tailing wildcard (up to two) for both inputs.
     * <p/>
     * The expanded range will expand between the lowest and highest number
     * regardless of the input order.
     *
     * @param in1 a bounderey for the range.
     * @param in2 a bounderey for the range.
     * @return an expanded range.
     */
    public static String makeRange(String in1, String in2){
        try{
        return doit(in1, in2);
        } catch (Exception e){
            log.debug("When making YearRange for ('" + in1 + "', '"
                      + in2 + "'): " + e.getMessage());
            return in1 + "-" + in2;
        }
    }

    //expand input to range of ints concatinated in String
    private static String doit(String in1, String in2)  {

        if (in1.length() > 4 || in2.length() >4 ){
           throw new IllegalArgumentException("Input length greater than 4");
        }

        StringBuffer buf = threadLocalBuffer.get();

        if (in1.equals(in2) && (in1.contains("???") || in1.contains("????")) ){
            return in1;
        }

        String years[] = new String[4];
        years[0] = in1; years[1] = in1;
        years[2] = in2; years[3] = in2;
        if (in1.contains("?")){
            years[0] = in1.replace("?", "0");
            years[1] = in1.replace("?", "9");
        }

        if (in2.contains("?")){
            years[2] = in2.replace("?", "0");
            years[3] = in2.replace("?", "9");
        }

        String startYear = years[0];
        String endYear = years[0];
        for (int i = 1; i<4;i++ ){
            if (years[i].compareTo(startYear)< 0) startYear= years[i];
            if (years[i].compareTo(endYear)> 0) endYear= years[i];
        }

        int start = Integer.parseInt(startYear);
        int end = Integer.parseInt(endYear);

        if (start == end){
            return Integer.toString(start);
        }

        for (int j=start; j<=end ;j++){
           buf.append(" ").append(j);
        }

        return buf.toString().substring(1);
    }

    public static void main (String[] args) {
        System.out.println("'" + makeRange("199?") + "'");
        System.out.println("'" + makeRange("199?","2001") + "'");
        System.out.println("'" + makeRange("1969","1969") + "'");
    }
}




