<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="dk.statsbiblioteket.summa.web.services.StatusBuilder" %>
<%@ page import="dk.statsbiblioteket.util.xml.DOM" %>
<%@ page import="org.w3c.dom.Node" %>
<%@ page import="dk.statsbiblioteket.summa.common.SummaConstants" %>
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
<?xml version="1.0" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <title>Summa Status Page - <%= SummaConstants.getVersion() %></title>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
    <link rel="alternate" type="application/rss+xml" title="Summa Status Feed" href="<%= rssUrl %>" />
</head>
<body style="padding: 10px;">
<div>
<img src="images/summa-logo_h40.png" alt="Summa logo"/>
<h1>Summa Status Page</h1>
<p>
    <%= SummaConstants.getVersion() %>
</p>
<p>
    Live updates: <a class="rsslink" href="<%= rssUrl %>">RSS Status Feed</a>
</p>
<%= stats.toString() %>
</div>
</body>
</html>
