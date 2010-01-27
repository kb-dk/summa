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

import java.net.URI;

import dk.statsbiblioteket.util.qa.QAInfo;

/**
 * Escapes the metadata part of the open url (the query) according to the
 * OpenUrl specification.
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
        asci = asci.replace("/", "%2F");
        asci = asci.replace(":", "%3A");
        return asci;
    }

}




