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
package dk.statsbiblioteket.summa.web;

import dk.statsbiblioteket.util.qa.QAInfo;
import junit.framework.TestCase;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.web.RSSChannelTest
 *
 * @author mke
 * @since Nov 25, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class RSSChannelTest extends TestCase {

    public void testPrint() {
        RSSChannel rss = new RSSChannel(
                "My Feed", "http://example.com/news.html",
                "Short description", "http://example.com");

        rss.addItem(
                "Item 1", "http://example.com/1", "Description 1");
        rss.addItem(
                "Item 2", "http://example.com/2", "Description 2",
                "Body text, or so called 'content'");        

        System.out.println(rss);
    }
}

