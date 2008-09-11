/* $Id: OpenUrlEscape.java,v 1.4 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.4 $
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

import java.net.URI;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Escapes the metadata part of the open url (the query) according to the OpenUrl specification.<br>
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class OpenUrlEscape {

    /**
     * Escapes the metadata query input 
     *
     * @param in
     * @return
     */
    public static String escape(String in){
        String base= "http://a.dk/?";
        URI i =URI.create(base + in);
        String asci = i.toASCIIString();
        asci = asci.substring(base.length());
        asci = asci.replaceAll("/", "%2F");
        asci = asci.replaceAll(":", "%3A");
        return asci;
    }

}



