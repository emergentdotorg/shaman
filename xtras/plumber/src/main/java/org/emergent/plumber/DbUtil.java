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
import org.emergent.plumber.util.DerbyUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Patrick Woodworth
 */
public class DbUtil {

  public static void resetDatabase(ServletContext ctx) throws ServletException {
    Connection conn = null;
    try {
      conn = Config.getInstance(ctx).getDatabaseConnection();
      DatabaseMetaData dmd = conn.getMetaData();
      DerbyUtil.dropSchema(dmd, "APP");
      Config.createDatabaseSchema(conn);
    } catch (SQLException e) {
      throw new ServletException(e);
    } catch (IOException e) {
      throw new ServletException(e);
    } finally {
      WeaveUtil.close(conn);
    }
  }

  public static int getUserId(Connection conn, String username) throws SQLException {
    int retval = -1;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = conn.prepareStatement("SELECT userid FROM WeaveUser WHERE username = ?");
      st.setString(1, username);
      rs = st.executeQuery();
      if (rs.next()) {
        retval = rs.getInt(1);
      }
    } finally {
      WeaveUtil.close(rs);
      WeaveUtil.close(st);
    }
    return retval;
  }

  public static void runSqlScript(String scriptName, Statement st) throws SQLException, IOException {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new InputStreamReader(DbUtil.class.getResourceAsStream(scriptName)));
      runSqlScript(in, st);
    } finally {
      WeaveUtil.close(in);
    }
  }

  public static void runSqlScript(BufferedReader in, Statement st) throws SQLException, IOException {
    //Now read line bye line
    String thisLine;
    String sqlQuery;
//    try {
    sqlQuery = "";
    while ((thisLine = in.readLine()) != null) {
      //Skip comments and empty lines
      if (thisLine.length() > 0 && thisLine.charAt(0) == '-' || thisLine.length() == 0)
        continue;
      sqlQuery = sqlQuery + " " + thisLine;
      //If one command complete
      if (sqlQuery.charAt(sqlQuery.length() - 1) == ';') {
        sqlQuery = sqlQuery.replace(';', ' '); //Remove the ; since jdbc complains
//          try {
        System.out.println("EXECUTING: " + sqlQuery);
        st.execute(sqlQuery);
//          } catch (SQLException ex) {
//            ex.printStackTrace();
//          }
        sqlQuery = "";
      }
    }
//    } catch (Exception ex) {
//      ex.printStackTrace();
//    }
  }

  public static void createTestData(ServletContext ctx) throws ServletException {
    Connection conn = null;
    Statement st = null;
    try {
      conn = Config.getInstance(ctx).getDatabaseConnection();
      st = conn.createStatement();
      runSqlScript("testdata.sql", st);
    } catch (SQLException e) {
      throw new ServletException(e);
    } catch (IOException e) {
      throw new ServletException(e);
    } finally {
      WeaveUtil.close(st);
      WeaveUtil.close(conn);
    }
  }

  private static List<String> getColumnNames(ResultSet rs) throws SQLException {
    List<String> retval = new ArrayList<String>();
    ResultSetMetaData metaData = rs.getMetaData();
    int colCnt = metaData.getColumnCount();
    for (int ii = 0; ii < colCnt; ii++) {
      String colname = metaData.getColumnName(ii + 1);
      retval.add(colname);
//      log(String.format("column (%d) : %s", ii+1, colname));
    }
    return retval;
  }

  private static String createEntryId() {
    return WeaveUtil.createEntryId();
  }
}
