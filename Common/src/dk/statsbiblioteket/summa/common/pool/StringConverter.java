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
/*
 * The State and University Library of Denmark
 * CVS:  $Id: StringConverter.java,v 1.3 2007/10/04 13:28:21 te Exp $
 */
package dk.statsbiblioteket.summa.common.pool;

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




