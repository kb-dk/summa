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
package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.summa.common.util.Pair;
import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * FIXME: Missing class docs for
 * dk.statsbiblioteket.summa.web.services.StatusPresentation
 *
 * @author mke
 * @since Nov 25, 2009
 */
@QAInfo(level = QAInfo.Level.NORMAL,
        state = QAInfo.State.IN_DEVELOPMENT,
        author = "mke")
public class StatusBuilder {

    static class Properties {
        Node group;

        Properties(Node group) {
            if (group == null) throw new NullPointerException(
                                             "Properties based on null group");
            this.group = group;
        }

        Node get(String name) {
            NodeList children = group.getChildNodes();

            if (children == null) {
                return null;
            }

            int numChildren = children.getLength();
            for (int i = 0; i < numChildren; i++) {
                Node child = children.item(i);
                if (!child.hasAttributes()) {
                    continue;
                }

                Node prop = child.getAttributes().getNamedItem("name");

                if (prop == null) continue;

                if (prop.getTextContent().equals(name)) {
                    return child;
                }
            }
            return null;
        }

        String getText(String name) {
            Node prop = get(name);
            if (prop == null) return "Undefined";

            Node child = prop.getFirstChild();
            if (child == null) {
                return "Undefined value for property '" + name + "'";
            }

            return child.getTextContent().trim();
        }
    }

    private Node statusDom;
    private Properties searcher;
    private Properties storage;
    private Properties suggest;
    private List<Pair<String,String>> queryCount;
    private String searcherStatus;
    private String storageStatus;
    private String suggestStatus;
    private String lastUpdate;
    private String now;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    NumberFormat numberFormat = NumberFormat.getIntegerInstance();

    public StatusBuilder (Node statusDom) {
        this.statusDom = statusDom;

        searcher = new Properties(
                DOM.selectNode(statusDom, "/status/group[@name='searcher']"));
        storage = new Properties(
                DOM.selectNode(statusDom, "/status/group[@name='storage']"));
        suggest = new Properties(
                DOM.selectNode(statusDom, "/status/group[@name='suggest']"));
        queryCount = sortQueries(DOM.selectNodeList(
                      statusDom, "/status/group[@name='queryCount']/property"));

        searcherStatus = searcher.getText("status");
        storageStatus = storage.getText("status");
        suggestStatus = suggest.getText("status");

        // FIXME: In the future we should probably use the moment
        // of index update instead
        lastUpdate = storage.getText("lastUpdate");
        now = dateFormat.format(new Date());

        // Minimum 6 digits and don't group numbers with . or ,
        numberFormat.setGroupingUsed(false);
        numberFormat.setMinimumIntegerDigits(6);
    }

    /* Parse the queryCount group into a sorted list of Pairs (count, query) */
    private List<Pair<String,String>> sortQueries(NodeList nodes) {
        List<Pair<String,String>> l = new LinkedList<Pair<String,String>>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            l.add(
                new Pair<String,String>(
                    node.getAttributes().getNamedItem("name").getTextContent(),
                    node.getFirstChild().getTextContent().trim()
                )
            );
        }

        Collections.sort(l);
        Collections.reverse(l);

        return l;
    }

    public boolean allGood() {
        return "OK".equals(searcherStatus) &&
               "OK".equals(storageStatus) &&
               "OK".equals(suggestStatus);
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public Date getLastUpdateStamp() {
        try {
            return dateFormat.parse(lastUpdate);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public Date getNowStamp() {
        try {
            return dateFormat.parse(now);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public String getNow() {
        return now;
    }

    public String toString() {
        try {
            return toString(new StringBuilder()).toString();
        } catch (IOException e) {
            throw new RuntimeException(
              "IOException while writing to memory. This should never happen!");
        }
    }

    public Appendable toString(Appendable buf) throws IOException {
        buf.append("<p>\n");
        buf.append("Last update: ").append(lastUpdate).append("<br/>");
        buf.append("Report generated: ").append(now).append("<br/>");
        buf.append("</p>\n");

        buf.append("<p>\n");
        buf.append("<b>Searcher:</b> <i>").append(searcherStatus)
                .append("</i><br/>");
        buf.append("Number of documents: ")
            .append(searcher.getText("numDocs"))
            .append("<br/>");
        buf.append("Response time: ")
            .append(searcher.getText("responseTime"))
            .append("ms<br/>");
        buf.append("Raw search time: ")
            .append(searcher.getText("searchTime"))
            .append("ms<br/>");
        buf.append("</p>\n");

        buf.append("<p>\n");
        buf.append("<b>Storage:</b> <i>").append(storageStatus)
                .append("</i><br/>");
        buf.append("Response time: ")
            .append(storage.getText("responseTime"))
            .append("ms<br/>");
        buf.append("</p>\n");

        buf.append("<p>\n");
        buf.append("<b>Suggest:</b> <i>").append(suggestStatus)
                .append("</i><br/>");
        buf.append("Response time: ")
            .append(suggest.getText("responseTime"))
            .append("ms<br/>");
        buf.append("</p>\n");

        buf.append("<p>\n");
        buf.append("<b>Popular queries the last 24 hours</b>");
        buf.append("<i>(number of queries - query string)</i>:<br/>");
        if(queryCount.size() > 0) {
            buf.append("<ul>");
            for (Pair<String,String> prop : queryCount) {
                buf.append("<li>")
                   .append(Integer.toString(Integer.parseInt(prop.getKey())))
                   .append(" - ")
                   .append("<tt>")
                   .append(prop.getValue())
                   .append("</tt>")
                   .append("</li>\n");
            }
            buf.append("</ul>\n");
        }
        buf.append("</p>\n");

        Node holdings = DOM.selectNode(statusDom,
          "/status/group[@name='storage']/property[@name='holdings']/holdings");

        buf.append("<b>Storage holdings:</b>");
        if (holdings != null) {
            NodeList bases = holdings.getChildNodes();
            if(bases.getLength() > 0) {
                buf.append("<ul>\n");
                for (int i = 0; i < bases.getLength(); i++) {
                    if (bases.item(i).getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    NamedNodeMap base = bases.item(i).getAttributes();
                    buf.append("<li><tt>")
                       .append(base.getNamedItem("name").getTextContent())
                       .append("</tt><br/>")
                       .append("Live: ")
                       .append(base.getNamedItem("live").getTextContent())
                       .append(", total: ")
                       .append(base.getNamedItem("total").getTextContent())
                       .append(". Updated: ")
                       .append(base.getNamedItem("mtime").getTextContent())
                       .append("</li>\n");
                }
                buf.append("</ul>\n");
            }
        } else {
            buf.append(" Not available");
        }
        return buf;
    }
}

