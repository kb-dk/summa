<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="java.io.File" %>
<%@ page import="dk.statsbiblioteket.util.xml.XMLUtil" %>
<%@ page import="dk.statsbiblioteket.util.xml.DOM" %>
<%@ page import="dk.statsbiblioteket.util.xml.XSLT" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="java.net.URL" %>
<%@ page import="javax.xml.transform.TransformerException" %>
<%@ page pageEncoding="UTF-8" %>
<%
    String basepath = request.getSession().getServletContext().getRealPath("/");
    request.setCharacterEncoding("UTF-8");


    String record_id = request.getParameter("record_id");
    WebServices services = WebServices.getInstance();
    String xml_record = (String) services.execute("summagetrecord", record_id);
    String record_html = "";


    if (xml_record == null || "".equals(xml_record)) {
        record_html = "Unable to resolve '" + record_id + "'";
    } else if (record_id == null
        || record_id.equals("")
        || record_id.indexOf(":") == -1) {

        if (xml_record.indexOf("<content>") == -1) {
            record_html = XMLUtil.encode(xml_record).replace("\n", "<br />\n");
        } else {
            // Stupid double-work, but hey... This is just a quick web site
            record_html = xml_record.substring(
                               xml_record.indexOf("<content>")
                               + 9, xml_record.lastIndexOf("</record>"))
                                                     .replace("\n", "<br />\n");
        }
    } else {
        // extract the first part of the id - ie. the type
        String record_type = record_id.substring(0, record_id.indexOf(":"));
	    URL show_xslt = new File(basepath
                                 + "xslt/full_record/full_record_show.xsl")
                                                               .toURI().toURL();	

        try {
            record_html = XSLT.transform(show_xslt, xml_record, true);
        } catch (TransformerException e) {
            record_html = "Transformer exception: " + e.getMessage();
        }
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
