<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="java.io.File" %>
<%@ page import="dk.statsbiblioteket.commons.XmlOperations" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page pageEncoding="UTF-8" %>
<%
    String basepath = request.getSession().getServletContext().getRealPath("/");
    request.setCharacterEncoding("UTF-8");


    String record_id = request.getParameter("record_id");
    WebServices services = WebServices.getInstance();
    String xml_record = (String) services.execute("summagetrecord", record_id);
    String record_html = "";


    if (record_id == null || record_id.equals("") || record_id.indexOf(":") == -1) {
	if (xml_record.indexOf("<content>") == -1) {
	    record_html = XmlOperations.entityEncode(xml_record).replace("\n", "<br />\n");
	} else {
	    // Stupid double-work, but hey... This is just a quick web site
	    record_html = xml_record.substring(xml_record.indexOf("<content>") + 9, xml_record.lastIndexOf("</record>")).replace("\n", "<br />\n");
	}
    } else {
        String record_type = record_id.substring(0, record_id.indexOf(":")); // extract the first part of the id - ie. the type
	File show_xslt = new File(basepath + "xslt/full_record/full_record_show.xsl");
	
	Document dom_record = XmlOperations.stringToDOM(xml_record);
	
	record_html = XmlOperations.xsltTransform(dom_record, show_xslt);
    }
%>
<html>
<head>
    <title>summa test</title>
</head>
<body style="padding: 10px;">
    <%= record_html %>
</body>
</html>
