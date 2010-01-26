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
import dk.statsbiblioteket.util.xml.DOM;

import java.util.Date;
import java.util.List;
import java.util.LinkedList;
import java.util.logging.Formatter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.IOException;


/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.web.RSSChannel
 *
 * @author mke
 * @since Nov 25, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class RSSChannel {

    private List<Item> items = new LinkedList<Item>();
    private DateFormat dateFormat =
                  new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z"); // RFC 822

    private String title;
    private String link;
    private String description;
    private String baseUrl;
    private String language = "en-us";
    private Date pubDate = new Date();
    private Date lastBuildDate = pubDate;
    private String docs = "http://cyber.law.harvard.edu/rss";
    private String generator = "Summa";
    private String image = null;

    public RSSChannel(String title, String link,
                      String description, String baseUrl) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.baseUrl = baseUrl;
    }

    public Item addItem(String title, String link, String description) {
        return addItem(title, link, description, null);
    }

    public Item addItem(String title, String link,
                        String description, String content) {
        Item item = new Item(dateFormat, title, link, description, content);
        items.add(item);
        return item;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public Date getLastBuildDate() {
        return lastBuildDate;
    }

    public void setLastBuildDate(Date lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
    }

    public String getDocs() {
        return docs;
    }

    public void setDocs(String docs) {
        this.docs = docs;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append(DOM.XML_HEADER).append('\n');
        buf.append("<rss version=\"2.0\" ")
           .append("xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"")
           .append(">\n");

        buf.append("  <channel>").append('\n');

        buf.append("    <title>").append(title).append("</title>").append('\n');
        buf.append("    <link>").append(link).append("</link>").append('\n');
        buf.append("    <description>").append(description).append("</description>").append('\n');
        buf.append("    <language>").append(language).append("</language>").append('\n');
        buf.append("    <pubDate>").append(dateFormat.format(pubDate)).append("</pubDate>").append('\n');
        buf.append("    <lastBuildDate>").append(dateFormat.format(lastBuildDate)).append("</lastBuildDate>").append('\n');
        buf.append("    <docs>").append(docs).append("</docs>").append('\n');
        buf.append("    <generator>").append(generator).append("</generator>").append('\n');

        if (image != null) {
            buf.append("    <image>").append(image).append("</image>").append('\n');
        }

        for (Item item : items) {
            try {
                item.toString(buf);
            } catch (IOException e) {
                throw new RuntimeException(
                        "IOException will writing to memory buffer. "
                        + "This should never happen");
            }
        }

        buf.append("  </channel>").append('\n');
        buf.append("</rss>");

        return buf.toString();
    }

    public static class Item {
        private DateFormat dateFormat;

        private String title;
        private String link;
        private String description;
        private Date pubDate;
        private String guid;
        private String content;

        private Item(DateFormat dateFormat, String title,
                     String link, String description, String content) {
            this.dateFormat = dateFormat;
            this.title = title;
            this.link = link;
            this.description = description;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Date getPubDate() {
            return pubDate;
        }

        public void setPubDate(Date pubDate) {
            this.pubDate = pubDate;
        }

        public String getGuid() {
            return guid;
        }

        public void setGuid(String guid) {
            this.guid = guid;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public Appendable toString(Appendable buf) throws IOException {
            buf.append("    <item>").append('\n');;

            buf.append("      <title>").append(title).append("</title>").append('\n');
            buf.append("      <link>").append(link).append("</link>").append('\n');
            buf.append("      <description><![CDATA[")
               .append(description)
               .append("]]></description>").append('\n');

            if (pubDate != null) {
                buf.append("      <pubDate>")
                   .append(dateFormat.format(pubDate))
                   .append("</pubDate>")
                   .append('\n');
            }

            if (guid != null) {
                buf.append("      <guid>").append(guid).append("</guid>").append('\n');
            }

            if (content != null) {
                buf.append("      <content:encoded><![CDATA[")
                   .append(content)
                   .append("]]></content:encoded>").append('\n');
            }

            buf.append("    </item>").append('\n');;
            return buf;
        }
    }

}

