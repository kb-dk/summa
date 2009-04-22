<%@ page import="java.io.File" %>
<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="dk.statsbiblioteket.util.xml.*" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.net.URL" %>
<%@ page import="javax.xml.transform.TransformerException" %>
<%@ page pageEncoding="UTF-8" %>
<%
    response.setContentType("text/html; charset=UTF-8");
    request.setCharacterEncoding("UTF-8");

    String basepath = request.getSession().getServletContext().getRealPath("/");
    WebServices services = WebServices.getInstance();

    String search_html = "";
    String facet_html = "";
    String form_query = "";

    String query = request.getParameter("query");
    if (query != null && !query.equals("")) {
        form_query = query.replaceAll("\"", "&quot;");

        int per_page = 10;
        int startIndex = 0;
        int current_page;
        try {
            current_page = Integer.parseInt(request.getParameter("page"));
            if (current_page < 0) {
                current_page = 0;
            }
        }
        catch (NumberFormatException e) {
            current_page = 0;
        }

        String xml_search_result = (String) services.execute("summasimplesearch", query, per_page, current_page * per_page);

        if (xml_search_result == null) {
            search_html = "Error executing query";
        } else {
            URL search_xslt = new File(basepath + "xslt/short_records.xsl").toURI().toURL();

            Properties search_prop = new Properties();
            search_prop.put("query", query);
            search_prop.put("per_page", per_page);
            search_prop.put("current_page", current_page);

            try {
                search_html = XSLT.transform(search_xslt, xml_search_result, search_prop, true);
            } catch (TransformerException e) {
                search_html = "Transformer exception: " + e.getMessage();
            }
        }

        String xml_facet_result = (String) services.execute("summasimplefacet", query);

        if (xml_facet_result == null) {
            facet_html = "Error faceting query";
        } else {
            URL facet_xslt = new File(basepath + "xslt/facet_overview.xsl")
                                                               .toURI().toURL();

            Properties facet_prop = new Properties();
            facet_prop.put("query", query);

            try {
                facet_html = XSLT.transform(facet_xslt, xml_facet_result, facet_prop);
            } catch (TransformerException e) {
                facet_html = "Transformer exception: " + e.getMessage();
            }
        }

    }
%>

<html>
<head>
    <title>summa example website</title>
    <link rel="stylesheet" type="text/css" href="css/project.css"/>
</head>
<body style="padding: 10px;">

<img src="images/summa-logo_h40.png" alt="Summa logo" />
<br />

<div class="searchBoxContainer" id="searchBoxContainer">
    <div class="searchBox" id="searchBox">
        <form action="index.jsp" class="searchBoxTweak" id="fpSearch">
            <input type="text" name="query" value="<%= form_query %>" />
            <input type="submit" value="Search" />
        </form>
    </div>
</div>

<div class="clusterLeft">
    <%= search_html %>
</div>
<div class="clusterRight">
    <%= facet_html %>
</div>
</body>
</html>
