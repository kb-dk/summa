<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="dk.statsbiblioteket.util.xml.DOM" %>
<%@ page import="dk.statsbiblioteket.util.xml.XSLT" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="javax.xml.transform.TransformerException" %>
<%@ page import="java.io.File" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.util.Properties" %>
<%@ page import="dk.statsbiblioteket.summa.common.*" %>
<%@ page pageEncoding="UTF-8" %>
<%
    response.setContentType("text/html; charset=UTF-8");
    request.setCharacterEncoding("UTF-8");

    String basepath = request.getSession().getServletContext().getRealPath("/");
    WebServices services = WebServices.getInstance();

    String search_html = "";
    String facet_html = "";
    String didyoumean_html = "";
    String form_filter = "";
    String form_query = "";
    String form_sort = "";
    boolean doDidYouMean = false;

    String query = request.getParameter("query");
    if ("".equals(query)) {
        query = null;
    }
    if (query != null) {
        form_query = query.replaceAll("\"", "&quot;");
    }

    String filter = request.getParameter("filter");
    if ("".equals(filter)) {
        filter = null;
    }
    if (filter != null) {
        form_filter = filter.replaceAll("\"", "&quot;");
    }

    String sort = request.getParameter("sort");
    if ("".equals(sort)) {
        sort = null;
    }
    if (sort != null) {
        form_sort = sort.replaceAll("\"", "&quot;");
    }

    if(request.getParameter("dodidyoumean") != null) {
        doDidYouMean = true;
    }


    if (query != null || filter != null) {
        int per_page = 10;
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

        URL didyoumean_xslt = new File(basepath + "xslt/didyoumean.xsl").toURI().toURL();
        Properties didyoumean_prop = new Properties();
        // didyoumean_prop.put("query", query == null ? "" : query);
        String xml_didyoumean_result = null;
        if(doDidYouMean) {
            xml_didyoumean_result = (String)services.execute(
                    "summadidyoumean", query, 10);
        }
        if (xml_didyoumean_result == null) {
            didyoumean_html = "No DidYouMean search/services";
        } else {
            //XSLT.clearTransformerCache();
            didyoumean_html = XSLT.transform(didyoumean_xslt, xml_didyoumean_result, didyoumean_prop, false);
        }


        String xml_search_result = (String)services.execute(
                "summafiltersearchsorted", filter, query,
                per_page, current_page * per_page, sort, false);

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
                long hitCount;
                try {
                    hitCount = Long.parseLong(hitCountStr);
                } catch (NumberFormatException e) {
                    hitCount = 0;
                }
                services.execute("summacommitquery", query, hitCount);
            }


            URL search_xslt = new File(
                    basepath + "xslt/short_records.xsl").toURI().toURL();

            Properties search_prop = new Properties();
            if (query != null) {
                search_prop.put("query", query);
            }
            if (filter != null) {
                search_prop.put("filter", filter);
            }
            if (sort != null) {
                search_prop.put("sort", sort);
            }
            search_prop.put("per_page", per_page);
            search_prop.put("current_page", current_page);

            try {
                search_html = XSLT.transform(
                        search_xslt, xml_search_result, search_prop, true);
            } catch (TransformerException e) {
                search_html = "Transformer exception: " + e.getMessage();
            }
        }

        // TODO: maybe we should use explicit filter/query for faceting too?
        String merged = "";
        if (filter != null) {
            merged += "(" + filter + ")";
        }
        if (query != null && !"*".equals(query) ) {
            if (!"".equals(merged)) {
                merged += " AND ";
            }
            merged += "(" + query + ")";
        }
        if ("".equals(merged)) {
            merged = "*";
        }
        String xml_facet_result = (String)services.execute(
                "summasimplefacet", merged);

        if (xml_facet_result == null) {
            facet_html = "Error faceting query";
        } else {
            URL facet_xslt = new File(
                    basepath + "xslt/facet_overview.xsl").toURI().toURL();

            Properties facet_prop = new Properties();
            facet_prop.put("filter", filter == null ? "" : "(" + filter + ") ");
            facet_prop.put("query", query == null ? "" : query);

            try {
                facet_html = XSLT.transform(facet_xslt, xml_facet_result, facet_prop);
            } catch (TransformerException e) {
                facet_html = "Transformer exception: " + e.getMessage();
            }
        }
    }
    out.clearBuffer();
%>
<?xml version="1.0" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf8" />
    <%-- TODO name should be depended on target test/services/dev. --%>
    <title>Summa Example Website - <%= SummaConstants.getVersion() %></title>
    <link rel="stylesheet" type="text/css" href="css/project.css" />
    <script type="text/javascript" src="js/jquery-1.3.2.min.js" ></script>
    <script type="text/javascript" src="js/jquery.autocomplete.min.js" ></script>

    <script type="text/javascript">
        function init() {
            $('#q').autocomplete({ serviceUrl:'service/autocomplete.jsp' });
            $('#f2').autocomplete({ serviceUrl:'service/autocomplete.jsp' });
            $('#q2').autocomplete({ serviceUrl:'service/autocomplete.jsp' });
            $('#q3').autocomplete({ serviceUrl:'service/autocomplete.jsp' });
            $('#f3').autocomplete({ serviceUrl:'service/autocomplete.jsp' });
            <%-- $('#i').autocomplete({ serviceUrl:'service/indexlookup.jsp' }); --%>
        }
    </script>
</head>
<body style="padding: 10px;" onload="init();">

<div>
    <img src="images/summa-logo_h40.png" alt="Summa logo" />
    <br />    
</div>

<div class="searchBoxContainer" id="searchBoxContainer">
    <div class="searchBox" id="searchBox">
        <form action="index.jsp" class="searchBoxTweak" id="fpSearch">
            <div>
                <label for="q">Standard search</label>
                <input type="text" name="query" size="65" id="q" value="<%= form_query %>" />
                <input type="submit" value="Search" />
                <input type="hidden" name="usersearch" value="true" />
            </div>
        </form>
        <%--
        <form action="index.jsp" class="searchBoxTweak" id="fpSearchI">
            Experimental index lookup (enter <code>facetname:term</code>)
            <input type="text" name="query" size="65" id="i" value="" />
            <input type="submit" value="Search" />
            <input type="hidden" name="usersearchI" value="true" />
        </form>
	<hr />
        <form action="index.jsp" class="searchBoxTweak" id="fpFilterSearch">
            Filter search<br />
            Filter: <input type="text" name="filter" size="55" id="f2" value="<%= form_filter %>" /><br />
            Query: <input type="text" name="query" size="55" id="q2" value="<%= form_query %>" />
            <input type="submit" value="Search" />
            <input type="hidden" name="userfiltersearch" value="true" />
        </form>
        --%>
	<hr />

        <form action="index.jsp" class="searchBoxTweak" id="fpFilterSortSearch">
            <div>
                Filter sorted search<br />
                <label for="f3">Filter:</label> <input type="text" name="filter" size="55" id="f3" value="<%= form_filter %>" /><br />
                <label for="q3">Query:</label> <input type="text" name="query" size="55" id="q3" value="<%= form_query %>" /><br />
                <label for="s3">Sort field:</label> <input type="text" name="sort" size="20" id="s3" value="<%= form_sort %>" /><br />
                <label for="dodidyoumean">Do Didyoumean Search:</label> <input type="checkbox" name="dodidyoumean" id="dodidyoumean" /><br />
                <input type="submit" value="Search" />
                <input type="hidden" name="userfiltersortsearch" value="true" />
            </div>
        </form>
<%= didyoumean_html %>
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
