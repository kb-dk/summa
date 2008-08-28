/* $Id: StringConverter.java,v 1.3 2007/10/04 13:28:21 te Exp $
 * $Revision: 1.3 $
 * $Date: 2007/10/04 13:28:21 $
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
 * CVS:  $Id: StringConverter.java,v 1.3 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.facetbrowser.util.pool;

import java.io.UnsupportedEncodingException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import dk.statsbiblioteket.util.LineReader;

/**
 * Simple conversion of Strings to and from bytes.
 */
public class StringConverter implements ValueConverter<String> {
    private static Log log = LogFactory.getLog(StringConverter.class);

    public byte[] valueToBytes(String value) {
        if (log.isTraceEnabled()) {
            log.trace("Converting \"" + value + "\" to bytes");
        }
        try {
            return (value + "\n").getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("utf-8 conversion failed for value '"
                                       + value + "'", e);
        }
    }

    public String bytesToValue(byte[] buffer, int length) {
        try {
            String result = new String(buffer, 0, length, "utf-8");
            if (result.length() == 0) {
                return result;
            }
            return result.substring(0, result.length()-1); // Remove \n
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("utf-8 conversion failed", e);
        }
    }
}
