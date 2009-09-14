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
            String usersearch = request.getParameter("usersearch");
            if (usersearch != null && "true".equals(usersearch)) {
                // this search comes from the form, ie. it is submitted by a user
                // so we extract the hitCount and submit it to the Suggestions index
                Document search_dom = DOM.stringToDOM(xml_search_result);
                String hitCountStr = DOM.selectString(search_dom,
                        "/responsecollection/response/documentresult/@hitCount", "0");
                long hitCount = 0;
                try {
                    hitCount = Long.parseLong(hitCountStr);
                } catch (NumberFormatException e) {
                    hitCount = 0;
                }
                services.execute("summacommitquery", query, hitCount);
            }


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
    <script type="text/javascript" src="js/jquery-1.3.2.min.js"></script>
    <script type="text/javascript" src="js/jquery.autocomplete.min.js"></script>

    <script type="text/javascript">
        function init() {
            $('#q').autocomplete({ serviceUrl:'service/autocomplete.jsp' });
            <%-- $('#i').autocomplete({ serviceUrl:'service/indexlookup.jsp' }); --%>
        }
    </script>
</head>
<body style="padding: 10px;" onload="init();">

<img src="images/summa-logo_h40.png" alt="Summa logo" />
<br />

<div class="searchBoxContainer" id="searchBoxContainer">
    <div class="searchBox" id="searchBox">
        <form action="index.jsp" class="searchBoxTweak" id="fpSearch">
            Standard search
            <input type="text" name="query" size="65" id="q" value="<%= form_query %>" />
            <input type="submit" value="Search" />
            <input type="hidden" name="usersearch" value="true" />
        </form>
        <%--
        <form action="index.jsp" class="searchBoxTweak" id="fpSearchI">
            Experimental index lookup (enter <code>facetname:term</code>)
            <input type="text" name="query" size="65" id="i" value="" />
            <input type="submit" value="Search" />
            <input type="hidden" name="usersearchI" value="true" />
        </form>
        --%>
    </div>
</div>

<div class="clusterLeft" style="clear: both;">
    <%= search_html %>
</div>
<div class="clusterRight">
    <%= facet_html %>
</div>
</body>
</html>
