<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="java.io.File" %>
<%@ page import="dk.statsbiblioteket.commons.XmlOperations" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page pageEncoding="UTF-8" %>
<%
    String basepath = request.getSession().getServletContext().getRealPath("/");
    request.setCharacterEncoding("UTF-8");

    String record_id;
    String record_type = "";

    record_id = request.getParameter("record_id");
    if (record_id != null && !record_id.equals("")) {
        record_type = record_id.substring(0, record_id.indexOf(":")); // extract the first part of the id - ie. the type
    }
    String record_html = "";

    File show_xslt = new File(basepath + "xslt/full_record/full_record_show.xsl");

    WebServices services = WebServices.getInstance();
    String xml_record = (String) services.execute("summagetrecord", record_id);
    Document dom_record = XmlOperations.stringToDOM(xml_record);

    record_html = XmlOperations.xsltTransform(dom_record, show_xslt);
%>
<html>
<head>
    <title>summa test</title>
</head>
<body style="padding: 10px;">
    <%= record_html %>
</body>
</html>
