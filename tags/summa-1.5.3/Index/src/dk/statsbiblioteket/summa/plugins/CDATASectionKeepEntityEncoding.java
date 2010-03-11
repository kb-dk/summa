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




