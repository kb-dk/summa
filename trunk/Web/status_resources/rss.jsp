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
    response.setContentType("application/xml; charset=UTF-8");
    request.setCharacterEncoding("UTF-8");

    WebServices services = WebServices.getInstance();

    String status_xml = (String)services.execute("fullStatus");


%>

FIXME