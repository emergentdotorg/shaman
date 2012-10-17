<%@ page import="org.emergent.plumber.DbUtil" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.sql.*" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.json.JSONException" %>
<%@ page import="org.emergent.plumber.MiscUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.emergent.plumber.Config" %>
<%@ page import="org.emergent.android.weave.client.WeaveUtil" %>
<%@page contentType="text/html" pageEncoding="UTF-8" language="java" %>
<%
    if ("POST".equals(request.getMethod())) {
//        Map paramMap = request.getParameterMap();
//        out.append("<pre>");
//        for (Object o : paramMap.entrySet()) {
//            Map.Entry param = (Map.Entry)o;
//            out.append("param : ").append((String)param.getKey()).append(" = ");
//            out.append("<br/>");
//            String[] values = (String[])param.getValue();
//            for (String value : values) {
//                out.append("  ").append(value);
//                out.append("<br/>");
//            }
//
//        }
//        out.append("</pre>");
        String submitValue = request.getParameter("submit");
        if ("resetdb".equals(submitValue)) {
            DbUtil.resetDatabase(application);
        }
        if ("inittestdata".equals(submitValue)) {
            DbUtil.createTestData(application);
        }
        response.sendRedirect("index.jsp");
    }
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>JSP Page</title>
</head>
<body>
<h1>Hello World!</h1>
Method: <%= request.getMethod() %>
Name: <%= request.getParameter("name") %>
Posted: <%= request.getParameter("posted") %>
<hr/>
<form action="index.jsp" method="post">
    First name: <input type="text" name="name"/><br/>
    Last name: <input type="text" name="lastname"/>
    <input type="hidden" name="posted" value="true"/>
    <input type="submit" name="submit" value="resetdb"/>
    <input type="submit" name="submit" value="inittestdata"/>
</form>
<hr/>

<table border="1">


<%!
    public void writeTable(JspWriter out, Connection conn, String tableName) throws SQLException, IOException {
        out.append("<h2>").append(tableName).append("</h2>");
        out.append("<table border='1'>");
        Statement st = null;
        ResultSet rs = null;
        try {
            st = conn.createStatement();
            rs = st.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData metaData = rs.getMetaData();
            int colCnt = metaData.getColumnCount();
            out.append("<tr>");
            for (int ii = 0; ii < colCnt; ii++) {
                out.append("<th>");
                out.append(String.valueOf(metaData.getColumnName(ii + 1)));
                out.append("</th>");
            }
            out.append("</tr>");
            while (rs.next()) {
                out.append("<tr>");
                for (int ii = 0; ii < colCnt; ii++) {
                    out.append("<td><pre>");
                    String colVal = String.valueOf(rs.getObject(ii + 1));
                    if (colVal.startsWith("{")) {
                        try {
                            JSONObject colObj = new JSONObject(colVal);
                            colVal = colObj.toString(2);
                        } catch (JSONException ignored) {
                        }
                    }
                    out.append(colVal);
                    out.append("</pre></td>");
                }
                out.append("</tr>");
            }
        } finally {
            WeaveUtil.close(rs);
            WeaveUtil.close(st);
            out.append("</table>");
        }
    }
%>
<%
    if (!"POST".equals(request.getMethod())) {
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = Config.getInstance(application).getDatabaseConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getTables(null, "APP", "%", null);
            List<String> names = new ArrayList<String>();
            while (rs.next()) {
                String tablename = rs.getString("TABLE_NAME");
                names.add(tablename);
            }
            WeaveUtil.close(rs);
            rs = null;
            for (String tablename : names) {
                writeTable(out, conn, tablename);
            }
        } catch (Exception e) {
            application.log(e.getMessage(), e);
        } finally {
            WeaveUtil.close(rs);
            WeaveUtil.close(conn);
        }
    }
%>

</table>
</body>
</html>
