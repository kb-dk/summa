/* $Id: CDATASectionKeepEntityEncoding.java,v 1.6 2007/10/05 10:20:23 te Exp $
 * $Revision: 1.6 $
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
 * This plugin is used for encode Strings as values in embeded XML in CDATAsections.
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "hal")
public class CDATASectionKeepEntityEncoding {

    public static final Log log = LogFactory.getLog(CDATASectionKeepEntityEncoding.class);

    /**
     * This will encode a String " to <code>&amp;apos;<code>.
     * This masked encoding makes the org " encoding 'keepable' in a embeded XML text value.
     *
     * This has been changed to the logical &quot; instead. It remains to be
     * checked what this changes at the receiving end.
     * @param in  encode this String
     * @return   the encoded String
     */
    // TODO: Review &apos; vs. &quot;
    public static String encode(String in){
        in = in.replaceAll("&", "&amp;");
        in = in.replaceAll("\"", "&quot;");
        in = in.replaceAll("<", "&lt;");
        in = in.replaceAll(">", "&gt;");
        log.debug("encoded string: " + in);
        // FIXME: Consider double-encoding & if it is not part of &amp;
        return in;
    }
}



