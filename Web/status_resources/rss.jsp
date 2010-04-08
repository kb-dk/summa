<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="dk.statsbiblioteket.summa.web.RSSChannel" %>
<%@ page import="dk.statsbiblioteket.util.xml.DOM" %>
<%@ page import="org.w3c.dom.Node" %>
<%@ page import="dk.statsbiblioteket.summa.web.services.StatusBuilder" %>
<%@ page import="java.util.*" %>
<%@ page pageEncoding="UTF-8" %>
<%
    response.setContentType("application/rss+xml; charset=UTF-8");
    request.setCharacterEncoding("UTF-8");

    String basePath = request.getSession().getServletContext().getRealPath("/");
    String rootUrl =  HttpUtils.getRequestURL(request).toString();
    rootUrl = rootUrl.substring(0, rootUrl.lastIndexOf("/"));
    String indexUrl = rootUrl + "/index.jsp";
    boolean allGood = true;

    String granularity = request.getParameter("granularity");
    int ignoreNewerThan;
    try {
        ignoreNewerThan = Integer.parseInt(
                                       request.getParameter("ignorenewerthan"));
    } catch (NumberFormatException e) {
        ignoreNewerThan = -1; 
    }

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
        String guid = "summastats:" + stats.getLastUpdate(granularity,
                                                    stats.getLastUpdateStamp());
        String description;
        if (stats.allGood()) {
            description = "All services running";
        } else {
            allGood = false;
            // We have problems in paradise,
            // make sure the RSS timestamp is updated
            title = "WARNING: " + title;
            description = "Warning: One or more services are not responding";
            guid = "summastats:" + stats.getLastUpdate(granularity,
                                                       stats.getNowStamp());
            rss.setLastBuildDate(stats.getNowStamp());
        }

        rss.setLastBuildDate(stats.getLastUpdateStamp());
        Date ignoreTimeStamp = Calendar.getInstance().getTime();
        ignoreTimeStamp.setTime(ignoreTimeStamp.getTime()-ignoreNewerThan);
        if(!allGood && stats.getLastUpdateStamp().before(ignoreTimeStamp)) {
            RSSChannel.Item item =
                    rss.addItem(title, indexUrl, description, stats.toString());
            item.setGuid(guid);
        }
    }

    out.clearBuffer();
%><%= rss.toString() %>