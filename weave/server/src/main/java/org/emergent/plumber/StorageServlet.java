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

import org.json.JSONArray;
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
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * @author Patrick Woodworth
 */
public class StorageServlet extends AbstractSyncServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String msg = null;
    try {
      msg = getMsg(req);
      log("resp: " + msg);
    } catch (SQLException e) {
      log(e.getMessage(), e);
    }

    if (msg != null) {
      resp.setContentType("application/json");
      PrintWriter writer = resp.getWriter();
      writer.append(msg);
    } else {
      resp.sendError(404);
    }
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String body = MiscUtil.readBody(req);
    log("BODY:\n" + body);
    long tsmillis = getServerTimestamp(req);
    Connection dbCon = null;
    PreparedStatement st = null;
    String rspMsg = null;
    try {
      JSONObject retval = new JSONObject();
      double serverTimestamp = MiscUtil.toWeaveTimestampDouble(tsmillis);
      retval.put("modified", serverTimestamp);
      JSONObject clientObj = new JSONObject(body);
      updateClient(req, tsmillis, clientObj);
      rspMsg = retval.toString();
    } catch (SQLException e) {
      log(e.getMessage(), e);
    } catch (JSONException e) {
      log(e.getMessage(), e);
    } finally {
      MiscUtil.close(st);
      MiscUtil.close(dbCon);
    }

    if (rspMsg != null) {
//      resp.setCharacterEncoding("UTF-8");
      resp.setContentType("application/json");
      PrintWriter writer = resp.getWriter();
      writer.append(rspMsg);
    } else {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//    int length = req.getContentLength();
    String ctype = req.getContentType();
    String cenc = req.getCharacterEncoding();
//    this.log("CLEN: " + length);
    this.log("CTYPE: " + ctype);
    this.log("CENC : " + cenc);
    String body = MiscUtil.readBody(req);
    this.log("BODY:\n" + body);
    long tsmillis = getServerTimestamp(req);

    String rspMsg = null;
    try {
      JSONObject retval = new JSONObject();
      double serverTimestamp = MiscUtil.toWeaveTimestampDouble(tsmillis);
      retval.put("modified", serverTimestamp);
      JSONArray successArray = new JSONArray();
      JSONArray failedArray = new JSONArray();
      JSONArray clientsArray = new JSONArray(body);
      for (int ii = 0; ii < clientsArray.length(); ii++) {
        // todo catch exceptions in loop
        JSONObject clientObj = clientsArray.getJSONObject(ii);
        String nodeId = clientObj.getString("id");
        updateClient(req, tsmillis, clientObj);
        successArray.put(nodeId);
      }
//      failedArray.put(nodeId);
      retval.put("success", successArray);
      retval.put("failed", failedArray);
      rspMsg = retval.toString();
    } catch (SQLException e) {
      log(e.getMessage(), e);
    } catch (JSONException e) {
      log(e.getMessage(), e);
    }

    log("RESPONSE: " + rspMsg);
    if (rspMsg != null) {
      resp.setContentType("application/json");
      PrintWriter writer2 = resp.getWriter();
      writer2.append(rspMsg);
    } else {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String username = (String)req.getAttribute(ATTRIB_USERNAME_KEY);
    String colname = (String)req.getAttribute(ATTRIB_COLNAME_KEY);
    String entryid = (String)req.getAttribute(ATTRIB_ENTRYID_KEY);

    boolean singleton = !MiscUtil.isEmpty(entryid);
    String idlist = req.getParameter("ids");
    boolean deleteall = !singleton && MiscUtil.isEmpty(idlist);

    Connection conn = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      conn = getDatabaseConnection(req);
      int userid = DbUtil.getUserId(conn, username);

      String selectStr =
          "DELETE FROM EngineWbo"
              + " WHERE userid = ?"
              + " AND engine = ?";

      if (!deleteall) {
        selectStr += " AND nodeid = ?";
      }

      st = conn.prepareStatement(selectStr);

      int modCnt = 0;

      if (!deleteall) {

        ArrayList<String> ids = new ArrayList<String>();
        if (singleton) {
          ids.add(entryid);
        } else {
          String[] idarray = idlist.split(",");
          if (idarray != null) {
            for (String idval : idarray) {
              if (!MiscUtil.isEmpty(idval))
                ids.add(idval);
            }
          }
        }

        for (String idval : ids) {
          st.setInt(1, userid);
          st.setString(2, colname);
          st.setString(3, idval);
          modCnt += st.executeUpdate();
        }
      } else {
        st.setInt(1, userid);
        st.setString(2, colname);
        modCnt = st.executeUpdate();
      }

    } catch (SQLException e) {
      log(e.getMessage(), e);
    } finally {
      MiscUtil.close(rs);
      MiscUtil.close(st);
      MiscUtil.close(conn);
    }

    resp.setContentType("application/json");
    String weaveTimestamp = getServerTimestampString(req);
    PrintWriter writer = resp.getWriter();
    writer.append(String.format("{\"modified\":%s}", weaveTimestamp));
  }

  protected String getMsg(HttpServletRequest req) throws SQLException {
    String username = (String)req.getAttribute(ATTRIB_USERNAME_KEY);
    String colname = (String)req.getAttribute(ATTRIB_COLNAME_KEY);
    String entryid = (String)req.getAttribute(ATTRIB_ENTRYID_KEY);

    boolean singleton = !MiscUtil.isEmpty(entryid);
    String fullParmValue = req.getParameter("full");
    boolean fullrecords = singleton || "1".equals(fullParmValue);

    Connection conn = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      conn = getDatabaseConnection(req);
      String selectStr =
          "SELECT E.userid, E.engine, E.nodeid, E.modified, E.sortindex, E.payload, E.ttl"
              + " FROM WeaveUser U, EngineWbo E"
              + " WHERE U.userid = E.userid"
              + " AND U.username = ?"
              + " AND E.engine = ?";

      if (singleton) {
        selectStr += " AND E.nodeid = ?";
      }

      st = conn.prepareStatement(selectStr);
      st.setString(1, username);
      st.setString(2, colname);
      if (singleton) {
        st.setString(3, entryid);
      }
      rs = st.executeQuery();

      if (singleton) {
        if (!rs.next()) {
          return null;
        }
        JSONObject clientObj = readEntry(rs);
        return clientObj.toString();
      } else {
        JSONArray clientsArray = new JSONArray();
        while (rs.next()) {
          if (fullrecords) {
            JSONObject clientObj = readEntry(rs);
            clientsArray.put(clientObj);
          } else {
            String id = rs.getString("nodeid");
            clientsArray.put(id);
          }
        }
        return clientsArray.toString();
      }
    } catch (JSONException e) {
      throw new SQLException(e);
    } finally {
      MiscUtil.close(rs);
      MiscUtil.close(st);
      MiscUtil.close(conn);
    }
  }


  private JSONObject readEntry(ResultSet rs) throws JSONException, SQLException {
    JSONObject clientObj = new JSONObject();
    String id = rs.getString("nodeid");
    clientObj.put("id", id);

    Timestamp modified = rs.getTimestamp("modified");
    long mod = modified.getTime();
    double modDouble = MiscUtil.toWeaveTimestampDouble(mod);
    clientObj.put("modified", modDouble);

    String payload = rs.getString("payload");
    clientObj.put("payload", payload);

    int sortindex = rs.getInt("sortindex");
    if (!rs.wasNull())
      clientObj.put("sortindex", sortindex);

    log("returning : " + clientObj.toString());
    return clientObj;
  }

  protected void updateClient(HttpServletRequest req, long tsmillis, JSONObject clientObj) throws SQLException, JSONException {
    String username = (String)req.getAttribute(ATTRIB_USERNAME_KEY);
    String colname = (String)req.getAttribute(ATTRIB_COLNAME_KEY);
    Connection conn = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      conn = getDatabaseConnection(req);
      int userId = DbUtil.getUserId(conn, username);
      int modCnt = updateWbo(conn, userId, colname, tsmillis, clientObj);
    } finally {
      MiscUtil.close(rs);
      MiscUtil.close(st);
      MiscUtil.close(conn);
    }
  }

  public static int updateWbo(Connection conn, int userId, String col, long tsmillis, JSONObject clientObj) throws SQLException, JSONException {
    String id = clientObj.getString("id");
    String payload = clientObj.has("payload") ? clientObj.getString("payload") : null;

    int retval = 0;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      String updPrefix = "UPDATE EngineWbo SET modified = ?";

      if (clientObj.has("sortindex"))
        updPrefix += ", sortindex = ?";

      if (clientObj.has("payload"))
        updPrefix += ", payload = ?";

      if (clientObj.has("ttl"))
        updPrefix += ", ttl = ?";

      String updSuffix = " WHERE userid = ? AND engine = ? AND nodeid = ?";

      st = conn.prepareStatement(updPrefix + updSuffix);

      Timestamp modSqlTs = new Timestamp(tsmillis);

      int varIdx = 1;
      st.setTimestamp(varIdx++, modSqlTs);
      if (clientObj.has("sortindex"))
        st.setInt(varIdx++, clientObj.getInt("sortindex"));
      if (clientObj.has("payload"))
        st.setString(varIdx++, payload);
      if (clientObj.has("ttl"))
        st.setInt(varIdx++, clientObj.getInt("ttl"));

      st.setInt(varIdx++, userId);
      st.setString(varIdx++, col);
      st.setString(varIdx++, id);
      retval = st.executeUpdate();


      if (retval < 1 && clientObj.has("payload")) {


        String insPrefix = "INSERT INTO EngineWbo(userid, engine, nodeid, modified, payload";
        String insSuffix = ") VALUES(?,?,?,?,?";

        if (clientObj.has("sortindex")) {
          insPrefix += ", sortindex";
          insSuffix += ",?";
        }

        if (clientObj.has("ttl")) {
          insPrefix += ", ttl";
          insSuffix += ",?";
        }

        insSuffix += ")";

        st = conn.prepareStatement(insPrefix + insSuffix);

        varIdx = 1;
        st.setInt(varIdx++, userId);
        st.setString(varIdx++, col);
        st.setString(varIdx++, id);
        st.setTimestamp(varIdx++, modSqlTs);
        st.setString(varIdx++, payload);
        if (clientObj.has("sortindex"))
          st.setInt(varIdx++, clientObj.getInt("sortindex"));
        if (clientObj.has("ttl"))
          st.setInt(varIdx++, clientObj.getInt("ttl"));

        retval = st.executeUpdate();
      }
    } finally {
      MiscUtil.close(rs);
      MiscUtil.close(st);
    }
    return retval;
  }
}
