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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Simple conversion of Strings to and from bytes.
 */
public class StringConverter implements ValueConverter<String> {
    private static Log log = LogFactory.getLog(StringConverter.class);

    @Override
    public byte[] valueToBytes(String value) {
        if (log.isTraceEnabled()) {
            log.trace("Converting \"" + value + "\" to bytes");
        }
        return (value + "\n").getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String bytesToValue(byte[] buffer, int length) {
        String result = new String(buffer, 0, length, StandardCharsets.UTF_8);
        if (result.isEmpty()) {
            return result;
        }
        return result.substring(0, result.length()-1); // Remove \n
    }
}




