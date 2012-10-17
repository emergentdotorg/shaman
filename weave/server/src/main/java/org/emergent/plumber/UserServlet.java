/*
 * Copyright 2011 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.emergent.plumber;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * @author Patrick Woodworth
 */
public class UserServlet extends AbstractBaseServlet {

  public static int addUser(Connection conn, String username, String password, String email) throws SQLException, JSONException {
    int retval = 0;
    PreparedStatement st = null;
    try {
      st = conn.prepareStatement("INSERT INTO WeaveUser(username,password,email) VALUES(?,?,?)");
      st.setString(1, username);
      st.setString(2, password);
      st.setString(3, email);
      retval = st.executeUpdate();
    } finally {
      MiscUtil.close(st);
    }
    return retval;
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    parseUserAndNode(req);
    super.service(req, resp);
  }

  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String subPath = (String)req.getAttribute(ATTRIB_SUBPATH_KEY);
    if ("".equals(subPath)) {
      doGetDefault(req, resp);
    } else if ("/node/weave".equals(subPath)) {
      doGetNodeWeave(req, resp);
    } else {
      resp.sendError(404);
    }
  }

  protected void doGetDefault(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String userName = (String)req.getAttribute(ATTRIB_USERNAME_KEY);
    Connection dbCon = null;
    boolean exists = false;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      dbCon = getDatabaseConnection(req);
      st = dbCon.prepareStatement("SELECT COUNT(*) FROM WeaveUser WHERE username = ?");
      st.setString(1, userName);
      rs = st.executeQuery();
      while (rs.next()) {
        int count = rs.getInt(1);
        log ("COUNT: " + count);
        if (count > 0)
          exists = true;
      }
    } catch (SQLException e) {
      log(e.getMessage(), e);
    } finally {
      MiscUtil.close(rs);
      MiscUtil.close(st);
      MiscUtil.close(dbCon);
    }
    resp.setContentType("text/html");
    resp.setCharacterEncoding("UTF-8");
    PrintWriter writer = resp.getWriter();
    writer.append(exists ? "1" : "0");
  }

  protected void doGetNodeWeave(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String reqUrl = req.getRequestURL().toString();
    String reqUri = req.getRequestURI();
    String ctxPath = req.getContextPath();

//    String pathInfo = req.getPathInfo();
//    String servletPath = req.getServletPath();
//    log("requrl: " + reqUrl);
//    log("requri: " + req.getRequestURI());
//    log("ctxpath: " + ctxPath);
//    log("pathinfo: " + pathInfo);
//    log("servletpath: " + servletPath);

    StringBuilder retval = new StringBuilder();
    retval.append(reqUrl.substring(0, reqUrl.indexOf(reqUri)));
    retval.append(ctxPath).append('/');

    resp.setContentType("text/html");
    resp.setCharacterEncoding("UTF-8");
    PrintWriter writer = resp.getWriter();
    writer.append(retval);
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String subPath = (String)req.getAttribute(ATTRIB_SUBPATH_KEY);
    if (!"".equals(subPath)) {
      super.doPut(req, resp);
      return;
    }

    String userName = (String)req.getAttribute(ATTRIB_USERNAME_KEY);
    String body = MiscUtil.readBody(req);
    for (Enumeration enu = req.getHeaderNames(); enu.hasMoreElements();) {
      String hname = (String)enu.nextElement();
      String hval = req.getHeader(hname);
      log(String.format("HEADER (%s) : \"%s\"", hname, hval));
    }
    log("BODY:\n" + body);

    Connection dbCon = null;
    PreparedStatement st = null;
    boolean success = false;
    try {
      JSONObject obj = new JSONObject(body);
      String password = obj.getString("password");
      String email = obj.getString("email");
      dbCon = getDatabaseConnection(req);
      int modCnt = addUser(dbCon, userName, password, email);
      log("SUCCESS: " + modCnt + " " + userName);
      success = modCnt == 1;
    } catch (JSONException e) {
      log(e.getMessage(), e);
    } catch (SQLException e) {
      log(e.getMessage(), e);
    } finally {
      MiscUtil.close(st);
      MiscUtil.close(dbCon);
    }
    if (success) {
      String weaveTimestamp = String.format("%.2f", System.currentTimeMillis() / 1000.0);
      resp.setHeader("X-Weave-Timestamp", weaveTimestamp);
      PrintWriter writer = resp.getWriter();
      resp.setContentType("text/html");
      resp.setCharacterEncoding("UTF-8");
      writer.append(userName);
    } else {
      super.doPut(req, resp);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String subPath = (String)req.getAttribute(ATTRIB_SUBPATH_KEY);
    if ("/email".equals(subPath)) {
      // todo update email
      super.doPut(req, resp);
    } else if ("/password".equals(subPath)) {
      // todo update password
      super.doPut(req, resp);
    } else {
      super.doPost(req, resp);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String subPath = (String)req.getAttribute(ATTRIB_SUBPATH_KEY);
    if (!"".equals(subPath)) {
      super.doPut(req, resp);
      return;
    }

    // todo delete the user (correct response is undocumented)
    String userName = (String)req.getAttribute(ATTRIB_USERNAME_KEY);
    String weaveTimestamp = String.format("%.2f", System.currentTimeMillis() / 1000.0);
    resp.setHeader("X-Weave-Timestamp", weaveTimestamp);
    PrintWriter writer = resp.getWriter();
    resp.setContentType("text/html");
    resp.setCharacterEncoding("UTF-8");
    writer.append(userName);
  }
}
