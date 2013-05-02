<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="dk.statsbiblioteket.summa.common.SummaConstants" %>
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
    WebServices services = WebServices.getInstance();

    String search_html = "";
    String facet_html = "";
    String didyoumean_html = "";
    String form_filter = "";
    String form_json = "";
    String form_query = "";
    String form_sort = "";
    boolean doDidYouMean = false;
    String pingResult = "No ping result";
    boolean reverseSort = false;
    String searchTiming = "";
    String facetTiming = "";

    long searchCall = 0;
    long searchXSLT = 0;
    long facetCall = 0;
    long facetXSLT = 0;
    long dymCall = 0;
    long dymXSLT = 0;
    long totalTime = -System.currentTimeMillis();
    String timing = "No timimg result";

    String query = request.getParameter("query");
    if ("".equals(query)) {
        query = null;
    }
    if (query != null) {
        form_query = query.replaceAll("\"", "&quot;");
    }

    String json = request.getParameter("json");
    if ("".equals(json)) {
        json = null;
    }
    if (json != null) {
        form_json = json.replaceAll("\"", "&quot;");
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

    if("on".equals(request.getParameter("reverse"))) {
        reverseSort = true;
    }


    if (query != null || filter != null || json != null) {
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

        // didyoumean_prop.put("query", query == null ? "" : query);
        String xml_didyoumean_result = null;
        if(doDidYouMean) {
            dymCall = -System.currentTimeMillis();
            xml_didyoumean_result = (String)services.execute(
                    "summadidyoumean", query, 10);
            dymCall += System.currentTimeMillis();
        }
        dymXSLT = -System.currentTimeMillis();
        if (xml_didyoumean_result == null) {
            didyoumean_html = "No DidYouMean search/services";
        } else {
            //XSLT.clearTransformerCache();
            URL didyoumean_xslt = new File(basepath + "xslt/didyoumean.xsl").toURI().toURL();
            Properties didyoumean_prop = new Properties();
            didyoumean_html = XSLT.transform(didyoumean_xslt, xml_didyoumean_result, didyoumean_prop, false);
        }
        dymXSLT += System.currentTimeMillis();
            
        searchCall = -System.currentTimeMillis();
        String xml_search_result = null;

        if (json != null) {
            xml_search_result = (String)services.execute("directjson", json);
        } else {
            xml_search_result = (String)services.execute(
                    "summafiltersearchsorted", filter, query,
                    per_page, current_page * per_page, sort, reverseSort);
        }
        searchCall += System.currentTimeMillis();

        if (xml_search_result == null) {
            search_html = "Error executing query";
        } else {
            String usersearch = request.getParameter("usersearch");
            Document search_dom = DOM.stringToDOM(xml_search_result);
            searchTiming = DOM.selectString(search_dom,
                        "/responsecollection/@timing", "N/A");
            if (usersearch != null && "true".equals(usersearch)) {
                // this search comes from the form, ie. it is submitted by a user
                // so we extract the hitCount and submit it to the Suggestions index
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


            searchXSLT = -System.currentTimeMillis();
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
            if (reverseSort) {
                search_prop.put("reverse", "on");
            } else {
                search_prop.put("reverse", "off");
            }
            search_prop.put("per_page", per_page);
            search_prop.put("current_page", current_page);

            try {
                search_html = XSLT.transform(
                        search_xslt, xml_search_result, search_prop, true);
            } catch (TransformerException e) {
                search_html = "Transformer exception: " + e.getMessage();
            }
            searchXSLT += System.currentTimeMillis();
        }

        // TODO: maybe we should use explicit filter/query for faceting too?
        String merged = "";
        if (filter != null) {
            merged += "(" + filter + ")";
        }
        if (query != null && !"*".equals(query) && !"*:*".equals(query) ) {
            if (!"".equals(merged)) {
                merged += " AND ";
            }
            merged += "(" + query + ")";
        }
        if ("".equals(merged)) {
            merged = "*";
        }
        facetCall = -System.currentTimeMillis();

        String xml_facet_result;
        if (json != null) {
            xml_facet_result = (String)services.execute("directjson", json);
        } else {
            xml_facet_result = (String)services.execute(
                "summasimplefacet", merged);
        }


        facetCall += System.currentTimeMillis();

        if (xml_facet_result == null) {
            facet_html = "Error faceting query";
        } else {
            Document facet_dom = DOM.stringToDOM(xml_facet_result);
            facetTiming = DOM.selectString(facet_dom,
                        "/responsecollection/@timing", "N/A");
            facetXSLT = -System.currentTimeMillis();
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
            facetXSLT += System.currentTimeMillis();
        }

        try {
            long RUNS = 1;
            long totalPing = -System.currentTimeMillis();
            long maxPing = 0;
            long minPing = 999999;
            for (int i = 0 ; i < RUNS ; i++) {
                long currentPing = -System.currentTimeMillis();
                pingResult = (String)services.execute("summaping", "foo" + i);
                currentPing += System.currentTimeMillis();
                maxPing = Math.max(maxPing, currentPing);
                minPing = Math.min(minPing, currentPing);
            }
            totalPing += System.currentTimeMillis();
            pingResult = RUNS + " pings: Min=" + minPing + "ms, max=" 
                + maxPing + "ms, average=" + (totalPing / RUNS) + "ms";
        } catch (Exception e) {
            pingResult = "Ping failed: " + e.getMessage();
        }
    }
    out.clearBuffer();
    totalTime += System.currentTimeMillis();
    timing = "Search: " + searchCall + "ms + " + searchXSLT + "ms, facet: " 
        + facetCall + "ms + " + facetXSLT + "ms, dym: " 
        + dymCall + "ms + " + dymXSLT + "ms, total: " + totalTime + "ms";
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
            $('#j1').autocomplete({ serviceUrl:'service/autocomplete.jsp' });
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
                <label for="j1">JSON:</label> <textarea cols="55" rows="4" name="json" id="j1"><%= form_json %></textarea><br />
                <label for="s3">Sort field:</label> <input type="text" name="sort" size="20" id="s3" value="<%= form_sort %>" />
                <% if (reverseSort) { %>
                  <label for="reverse">Reversed:</label> <input type="checkbox" name="reverse" id="reverse" checked="checked" /><br />
                <% } else { %>
                  <label for="reverse">Reversed:</label> <input type="checkbox" name="reverse" id="reverse"  /><br />
                <% } %>
                <label for="dodidyoumean">Do Didyoumean Search:</label> <input type="checkbox" name="dodidyoumean" id="dodidyoumean" /><br />
                <input type="submit" value="Search" />
                <input type="hidden" name="userfiltersortsearch" value="true" />
            </div>
        </form>
<%= didyoumean_html %>
<p>
        <strong>Latency:</strong> <%= pingResult %><br />
        <strong>Response time:</strong> <%= timing %><br />

    </div>
</div>


<div class="clusterLeft" style="clear: both;">
    <%= search_html %>
</div>
<div class="clusterRight">
    <%= facet_html %>
</div>

        <h3 style="clear: left">Detailed timing from sub system (full stack incl. website: <%= totalTime %> ms)</h3>
<h4>Search</h4>
<ul>
<% for (String element: searchTiming.split("[|]")) { 
     String[] tokens = element.split(":");
     long ms = tokens.length == 2 ? Long.parseLong(tokens[1]) : -2;
%>
     <li><%= tokens[0] + ": " + (ms > 1000 ? "<strong>" + ms + "</strong>" : Long.toString(ms)) %> ms</li>          
<% } %>
</ul>

<h4>Facet</h4>
<ul>
<% for (String element: facetTiming.split("[|]")) { 
     String[] tokens = element.split(":");
     long ms = tokens.length == 2 ? Long.parseLong(tokens[1]) : -2;
%>
     <li><%= tokens[0] + ": " + (ms > 1000 ? "<strong>" + ms + "</strong>" : Long.toString(ms)) %> ms</li>          
<% } %>
</ul>

</body>
</html>
