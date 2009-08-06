<%@ page import="dk.statsbiblioteket.gwsc.WebServices" %>
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="dk.statsbiblioteket.util.xml.DOM" %>
<%@ page import="org.w3c.dom.NodeList" %>
<%@ page import="org.w3c.dom.Node" %>
<%@ page pageEncoding="UTF-8" %>
<%
    response.setContentType("text/plain; charset=UTF-8");
    request.setCharacterEncoding("UTF-8");

    String basepath = request.getSession().getServletContext().getRealPath("/");
    WebServices services = WebServices.getInstance();

    String index = "";
    String indexQuery = request.getParameter("query");
    if (indexQuery != null && indexQuery.contains(":")) {
        String field = indexQuery.split(":")[0];
        String term = indexQuery.split(":", 2)[1];
        int delta = -5;
        int length = 10;

        String xmlIndexResult = (String)services.execute(
                "summaindexlookup", field, term, delta, length);

        Document domIndex = DOM.stringToDOM(xmlIndexResult);
        NodeList nl = DOM.selectNodeList(domIndex, "//term");
        if (nl != null) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                index += ("'" + field + ":" + (DOM.selectString(n, "./text()").
                        replace("'", "\"")) + "'");
                if (i < nl.getLength() - 1) {
                    // only add the , if this isn't the very last element
                    index += ",";
                }
            }
        }
    }
    /*
    Due to the caching used by jquery.autocomplete, a result must be returned
    at all times, so we return a list with an empty element.
    This is ugly and another autocompleter framework should be used.
     */
    index = "".equals(index) ? "''" : index;
    out.clearBuffer();
%>{query:'<%= indexQuery %>',suggestions:[<%= index %>]}
