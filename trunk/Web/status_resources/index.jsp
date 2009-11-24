<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="dk.statsbiblioteket.util.xml.DOM" %>
<%@ page import="dk.statsbiblioteket.util.xml.XSLT" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="javax.xml.transform.TransformerException" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.util.Properties" %>
<%@ page pageEncoding="UTF-8" %>
<%
    response.setContentType("text/html; charset=UTF-8");
    request.setCharacterEncoding("UTF-8");

    String basepath = request.getSession().getServletContext().getRealPath("/");
    String rsspath = basepath + "rss.jsp";

    WebServices services = WebServices.getInstance();

    String status_html = "FIXME";
    String status_xml = (String)services.execute("fullStatus");


%>

<html>
<head>
    <title>Summa Status Page</title>
    <link rel="alternate" type="application/rss+xml" title="Summa Status Feed" href="<%= rsspath %>%>" />
</head>
<body style="padding: 10px;">

<img src="images/summa-logo_h40.png" alt="Summa logo"/>
<h1>Summa Status Page</h1>
<%= status_html %>
</body>
</html>
