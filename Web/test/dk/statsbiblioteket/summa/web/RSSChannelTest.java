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
                "Item 2", "http://example.com/2", "Description 2");        

        System.out.println(rss);
    }
}
