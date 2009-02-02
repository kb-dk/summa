/* $Id: ISBN.java,v 1.5 2007/10/05 10:20:23 te Exp $
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
import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * This plugin contains methods for converting ISBN numbers.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class ISBN {

    private static final Log log = LogFactory.getLog(ISBN.class);

    /**
     * This will try to convert a string into an ISBN-13 number.<br>
     * Valid input is a String representation of an ISBN-10 number
     *
     * WARNING: the method makes no effort to validate the input.
     *
     * if something goes wrong when parsing the input - the input will be returned.
     *
     * @param in  an ISBN-10 String
     * @return if input was ISBN-10 the ISBN-13 number is returned
     */
    public static String isbnNorm(String in) {
        try {
            String out = in.trim();
            out = out.replace("-", "");
            if (out.trim().length() == 10) {
                out = out.substring(0, 9);
                out = "978" + out;
                int sum = 0;
                for (int i = 0; i < out.length(); i++) {
                    if ((i % 2 == 0)) {
                        sum += Integer.parseInt(out.substring(i, i + 1));
                    } else {
                        sum += 3 * Integer.parseInt(out.substring(i, i + 1));
                    }
                }
                int chcckSum = sum % 10;
                if (chcckSum != 0 ) {chcckSum = 10-chcckSum;}
                return  out + chcckSum;
            }

        } catch (Exception e) {
            log.warn("Error parsing isbn '"+in+"': " + e.getMessage());
            return in;
        }
        return in;
    }
}



