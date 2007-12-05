/* $Id: Normalize.java,v 1.5 2007/10/05 10:20:23 te Exp $
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
 * This plugin is used to remove 'noize' and normalize a String.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class Normalize {


    public static final Log log = LogFactory.getLog(Normalize.class);

    /**
     * The String is trimed and:    - (  ) , . [ ]     is removed<br>
     * @param in the string to normalize
     * @return  the normalized string
     */
    public static String normalize(String in){
        try{
            in = in.trim();
            in = in.replaceAll("-", "");
            in = in.replaceAll("\\(", "");
            in = in.replaceAll("\\)", "");
            in = in.replaceAll(",", "");
            in = in.replaceAll("\\.", "");
            in = in.replaceAll("\\[", "");
            in = in.replaceAll("]", "");
        } catch (Exception e){
           log.warn(e.getMessage());
        }
        return in;
    }
}
