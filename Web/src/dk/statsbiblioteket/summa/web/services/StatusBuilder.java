package dk.statsbiblioteket.summa.web.services;

import dk.statsbiblioteket.util.qa.QAInfo;
import dk.statsbiblioteket.util.xml.DOM;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * FIXME: Missing class docs for dk.statsbiblioteket.summa.web.services.StatusPresentation
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
    private String searcherStatus;
    private String storageStatus;
    private String suggestStatus;
    private String lastUpdate;
    private String now;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public StatusBuilder (Node statusDom) {
        this.statusDom = statusDom;

        searcher = new Properties(
                DOM.selectNode(statusDom, "/status/group[@name='searcher']"));
        storage = new Properties(
                DOM.selectNode(statusDom, "/status/group[@name='storage']"));
        suggest = new Properties(
                DOM.selectNode(statusDom, "/status/group[@name='suggest']"));

        searcherStatus = searcher.getText("status");
        storageStatus = storage.getText("status");
        suggestStatus = suggest.getText("status");

        // FIXME: In the future we should probably use the moment
        // of index update instead
        lastUpdate = storage.getText("lastUpdate");
        now = dateFormat.format(new Date());
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
        buf.append("Last update: ").append(lastUpdate).append("<br/>");
        buf.append("Report generated: ").append(now).append("<br/>");
        buf.append("<p/>\n");

        buf.append("<b>Searcher: ").append(searcherStatus).append("</b><br/>");
        buf.append("Number of documents: ")
            .append(searcher.getText("numDocs"))
            .append("<br/>");
        buf.append("Response time: ")
            .append(searcher.getText("responseTime"))
            .append("ms<br/>");
        buf.append("Raw search time: ")
            .append(searcher.getText("searchTime"))
            .append("ms<br/>");
        buf.append("<p/>\n");

        buf.append("<b>Storage: ").append(storageStatus).append("</b><br/>");
        buf.append("Response time: ")
            .append(storage.getText("responseTime"))
            .append("ms<br/>");
        buf.append("<p/>\n");

        buf.append("<b>Suggest: ").append(suggestStatus).append("</b><br/>");
        buf.append("Response time: ")
            .append(suggest.getText("responseTime"))
            .append("ms<br/>");
        buf.append("<p/>\n");

        return buf;
    }

}
