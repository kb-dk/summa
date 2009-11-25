<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="dk.statsbiblioteket.summa.web.RSSChannel" %>
<%@ page import="dk.statsbiblioteket.util.xml.DOM" %>
<%@ page import="dk.statsbiblioteket.util.xml.XSLT" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="javax.xml.transform.TransformerException" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="org.w3c.dom.Node" %>
<%@ page import="java.text.ParseException" %>
<%@ page import="org.w3c.dom.NodeList" %>
<%@ page import="org.w3c.dom.NamedNodeMap" %>
<%@ page import="dk.statsbiblioteket.summa.web.services.StatusBuilder" %>
<%@ page pageEncoding="UTF-8" %>
<%
    response.setContentType("application/rss+xml; charset=UTF-8");
    request.setCharacterEncoding("UTF-8");

    String basePath = request.getSession().getServletContext().getRealPath("/");
    String rootUrl =  HttpUtils.getRequestURL(request).toString();
    rootUrl = rootUrl.substring(0, rootUrl.lastIndexOf("/"));
    String indexUrl = rootUrl + "/index.jsp";

    WebServices services = WebServices.getInstance();

    RSSChannel rss = new RSSChannel("Summa Status Feed",
                                    indexUrl,
                                    "The latest status report from Summa",
                                    basePath);
    rss.setImage(rootUrl + "/images/summa-logo_h40.png");

    String statusXml = (String)services.execute("summafullstatus");

    if (statusXml == null) {
        rss.addItem("Error Getting Status",
                    indexUrl,
                    "Error contacting the status webservice");
    } else {
        Node dom = DOM.stringToDOM(statusXml);
        StatusBuilder stats = new StatusBuilder(dom);

        String title = "Summa Status Update " + stats.getNow();
        String guid = "summastats:" + stats.getLastUpdate();
        String description;
        if (stats.allGood()) {
            description = "All services running";
        } else {
            // We have problems in paradise,
            // make sure the RSS timestamp is updated
            title = "WARNING: " + title;
            description = "Warning: One or more services are not responding";
            guid = "summastats:" + stats.getNow();
            rss.setLastBuildDate(stats.getNowStamp());
        }

        rss.setLastBuildDate(stats.getLastUpdateStamp());
        RSSChannel.Item item =
                rss.addItem(title, indexUrl, description, stats.toString());
        item.setGuid(guid);
    }


    out.clearBuffer();
%><%= rss.toString() %>