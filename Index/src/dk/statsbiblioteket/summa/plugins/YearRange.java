/* $Id: YearRange.java,v 1.5 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.5 $
 * $Date: 2007/10/05 10:20:23 $
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
     * Make a range expansion between the two inputs.<br>
     * <code>?<code> can be used a tailing wildcard (up to two) for both inputs.<br>
     * The expanded range will expand between the lowest and highest number regardless of the inpyut order.<br>
     *
     * @param in1 a bounderey for the range.
     * @param in2 a bounderey for the range.
     * @return an expanded range.
     */
    public static String makeRange(String in1, String in2){
        try{
        return doit(in1, in2);
        } catch (Exception e){
            log.warn(e.getMessage());
            return in1 + "-" + in2;
        }
    }

    //expand input to range of ints concatinated in String
    private static String doit(String in1, String in2)  {

        if (in1.length() > 4 || in2.length() >4 ){
           throw new IllegalArgumentException();
        }

        StringWriter w = new StringWriter();

        if (in1.equals(in2) && (in1.contains("???") || in1.contains("????")) ){
            return in1;
        }

        String years[] = new String[4];
        years[0] = in1; years[1] = in1;
        years[2] = in2; years[3] = in2;
        if (in1.contains("?")){
            years[0] = in1.replaceAll("\\?", "0");
            years[1] = in1.replaceAll("\\?", "9");
        }

        if (in2.contains("?")){
            years[2] = in2.replaceAll("\\?", "0");
            years[3] = in2.replaceAll("\\?", "9");
        }

        String a = years[0];
        String b = years[0];
        for (int i = 1; i<4;i++ ){
            if (years[i].compareTo(a)< 0) a= years[i];
            if (years[i].compareTo(b)> 0) b= years[i];
        }

        int start = Integer.parseInt(a);
        int end = Integer.parseInt(b);

        if (start == end){
            return Integer.toString(start);
        }

        for (int j=start; j<=end ;j++){
           w.append(" ").append(Integer.toString(++j));

        }
        return w.toString().substring(1);
    }
}
