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

import org.emergent.android.weave.client.WeaveUtil;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

/**
 * @author Patrick Woodworth
 */
public class InfoServlet extends AbstractSyncServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String msg = "";
    try {
      msg = getMsg(req);
    } catch (SQLException e) {
      log(e.getMessage(), e);
    }

    if (msg != null) {
      resp.setContentType("application/json");
      PrintWriter writer = resp.getWriter();
      log("infocolresp: " + msg);
      writer.append(msg);
    } else {
      resp.sendError(404);
    }
  }

  protected String getMsg(HttpServletRequest req) throws SQLException {
    String username = (String)req.getAttribute(ATTRIB_USERNAME_KEY);
    Connection conn = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      conn = getDatabaseConnection(req);
      st = conn.prepareStatement(
          "SELECT E.engine, MAX(E.modified)"
              + " FROM WeaveUser U, EngineWbo E"
              + " WHERE U.userid = E.userid"
              + " AND U.username = ?"
              + " GROUP BY E.engine"
      );
      st.setString(1, username);
      rs = st.executeQuery();

      JSONObject retvalObj = new JSONObject();
      while (rs.next()) {
        String id = rs.getString(1);
        Timestamp modified = rs.getTimestamp(2);
        long mod = modified.getTime();
        double modDouble = MiscUtil.toWeaveTimestampDouble(mod);
        retvalObj.put(id, modDouble);
      }
      return retvalObj.toString();
    } catch (JSONException e) {
      throw new SQLException(e);
    } finally {
      WeaveUtil.close(rs);
      WeaveUtil.close(st);
      WeaveUtil.close(conn);
    }
  }

}
