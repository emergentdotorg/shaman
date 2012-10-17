<%@ page import="org.emergent.plumber.Config" %>
<%@ page import="org.emergent.plumber.DbUtil" %>
<%@ page import="org.emergent.plumber.MiscUtil" %>
<%@ page import="org.emergent.android.weave.client.UserWeave" %>
<%@ page import="org.emergent.android.weave.client.WeaveException" %>
<%@ page import="org.emergent.android.weave.client.WeaveFactory" %>
<%@ page import="org.json.JSONException" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.net.URI" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.ResultSetMetaData" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.sql.Statement" %>
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
        if ("createaccount".equals(submitValue)) {
            try {
                String serverUrl = request.getParameter("authserverurl");
                String userName = "newweave@emergent.org";
                String userPassword = "foobarbang";
                String syncKey = "xrwjwg42i6q9ds6ctwhudb9x24";
                char[] userSecret = syncKey.toCharArray();

                String capchal = request.getParameter("capchallenge");
                String capresp = request.getParameter("capresponse");

                UserWeave weave = (new WeaveFactory(true)).createUserWeave(
                        URI.create(serverUrl),
                        userName,
                        userPassword
                );
                JSONObject putObj = new JSONObject();
                putObj.put("password", userPassword);
                putObj.put("email", userName);
                putObj.put("captcha-challenge", capchal);
                putObj.put("captcha-response", capresp);
                String putBodyStr = putObj.toString();
                System.out.println(putBodyStr);
                weave.putNode("", putBodyStr);
            } catch (WeaveException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if ("inittestdata".equals(submitValue)) {
            DbUtil.createTestData(application);
        }
        response.sendRedirect(request.getRequestURI());
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
Posted: <%= request.getParameter("posted") %><br/>
URL: <%= request.getRequestURL().toString() %><br/>
URI: <%= request.getRequestURI() %><br/>
<p> CAPTCHA: <a href="https://auth.services.mozilla.com/misc/1.0/captcha_html">https://auth.services.mozilla.com/misc/1.0/captcha_html</a></p>
<p> CUR URL: <a href="http://localhost:8080/weave/">http://localhost:8080/weave/</a></p>
<p> ALT URL: <a href="https://auth.services.mozilla.com/">https://auth.services.mozilla.com/</a></p>
<hr/>
<form action="" method="post">
    Server URL: <input type="text" name="authserverurl" size="50" value="http://localhost:8080/weave/"/><br/>
    Challenge: <input type="text" name="capchallenge" size="50"/><br/>
    Response: <input type="text" name="capresponse" size="50"/>
    <input type="hidden" name="posted" value="true"/>
    <input type="submit" name="submit" value="createaccount"/>
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
            MiscUtil.close(rs);
            MiscUtil.close(st);
            out.append("</table>");
        }
    }
%>
<%
    if (!"POST".equals(request.getMethod())) {
        Connection conn = null;
        try {
            conn = Config.getInstance(application).getDatabaseConnection();
            writeTable(out, conn, "WeaveUser");
            writeTable(out, conn, "EngineWbo");
        } catch (Exception e) {
            application.log(e.getMessage(), e);
        } finally {
            MiscUtil.close(conn);
        }
    }
%>

</table>
</body>
</html>
