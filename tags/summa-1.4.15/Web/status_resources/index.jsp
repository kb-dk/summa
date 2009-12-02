<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="dk.statsbiblioteket.util.xml.DOM" %>
<%@ page import="dk.statsbiblioteket.util.xml.XSLT" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="javax.xml.transform.TransformerException" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.util.Properties" %>
<%@ page import="dk.statsbiblioteket.summa.web.services.StatusBuilder" %>
<%@ page import="org.w3c.dom.Node" %>
<%@ page pageEncoding="UTF-8" %>
<%
    response.setContentType("text/html; charset=UTF-8");
    request.setCharacterEncoding("UTF-8");

    String rootUrl =  HttpUtils.getRequestURL(request).toString();
    rootUrl = rootUrl.substring(0, rootUrl.lastIndexOf("/"));
    String rssUrl = rootUrl + "/rss.jsp";

    WebServices services = WebServices.getInstance();

    String statusXml = (String)services.execute("summafullstatus");
    Node dom = DOM.stringToDOM(statusXml);
    StatusBuilder stats = new StatusBuilder(dom);

    out.clearBuffer();
%>
<html>
<head>
    <title>Summa Status Page</title>
    <link rel="alternate" type="application/rss+xml" title="Summa Status Feed" href="<%= rssUrl %>%>" />
</head>
<body style="padding: 10px;">

<img src="images/summa-logo_h40.png" alt="Summa logo"/>
<h1>Summa Status Page</h1>
Live updates: <a class="rsslink" href="<%= rssUrl %>">RSS Status Feed</a>
<p/>
<%= stats.toString() %>
</body>
</html>
