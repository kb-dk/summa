<%@ page pageEncoding="UTF-8" %>
<%
    response.setContentType("text/html; charset=UTF-8");
    request.setCharacterEncoding("UTF-8");
%>
<html>
<head>
    <title>summa example website</title>
</head>
<body>
<form action="search.jsp">
    <input type="text" name="query" />
    <input type="submit" value="Search" />
</form>
</body>
</html>