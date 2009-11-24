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

    String suggs = "";

    String prefix = request.getParameter("query");
    if (prefix != null && !prefix.equals("")) {
        String xml_suggest_result = (String) services.execute("summagetsuggest", prefix, 10);
        Document dom_suggest = DOM.stringToDOM(xml_suggest_result);
        NodeList nl = DOM.selectNodeList(dom_suggest, "//suggestion");
        if (nl != null) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                suggs += "'" + (DOM.selectString(n, "./text()")) + "'";
                if (i < nl.getLength() - 1) {
                    // only add the , if this isn't the very last element
                    suggs += ",";
                }
            }
        }
    }
    out.clearBuffer();
%>{query:'<%= prefix %>',suggestions:[<%= suggs %>]}
