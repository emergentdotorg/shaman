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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;

/**
 * @author Patrick Woodworth
 */
public class Config implements ServletContextListener {

  public static final boolean USING_JNDI = true;
  public static final boolean LEGACY_DRIVER = !USING_JNDI;

  static final String PLUMBER_PACKAGE_NAME = Config.class.getPackage().getName();

  private static final String ATTRIBUTE_NAME = PLUMBER_PACKAGE_NAME + ".config";

  private ServletContext mContext;

  private DataSource mDataSource;

  public static Config getInstance(ServletContext servletContext) {
    return (Config)servletContext.getAttribute(ATTRIBUTE_NAME);
  }

  @Override
  public void contextInitialized(ServletContextEvent event) {
    mContext = event.getServletContext();
    if (USING_JNDI) {
      try {
        Context ctx = new InitialContext();
        String databaseName = mContext.getInitParameter("database.name");
        mDataSource = (DataSource)ctx.lookup("java:/comp/env/" + databaseName);
      } catch (NamingException e) {
        throw new RuntimeException("Config failed: datasource not found", e);
      }
    }
    mContext.setAttribute(ATTRIBUTE_NAME, this);

    Connection conn = null;
    try {
      conn = getDatabaseConnection();
      createDatabaseSchema(conn);
    } catch (Exception e) {
      mContext.log(e.getMessage(), e);
    } finally {
      MiscUtil.close(conn);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    if (LEGACY_DRIVER) {
      LegacyDriver.shutdownDatabase();
    }
  }

  public synchronized Connection getDatabaseConnection() throws SQLException {
    Connection conn = null;
    if (LEGACY_DRIVER) {
      conn = LegacyDriver.getConnection("create=true");
    } else {
      conn = getDataSource().getConnection();
    }
    return conn;
  }

  static void createDatabaseSchema(Connection conn) throws SQLException, IOException {
    if (!checkSchemaNeedsInit(conn))
      return;

    Statement st = null;
    ResultSet rs = null;
    try {
      st = conn.createStatement();
      DbUtil.runSqlScript("createdb.sql", st);
    } finally {
      MiscUtil.close(rs);
      MiscUtil.close(st);
    }
  }

  static boolean checkSchemaNeedsInit(Connection conn) throws SQLException, IOException {
    Statement st = null;
    ResultSet rs = null;
    try {
      boolean needInit = true;
      DatabaseMetaData metaData = conn.getMetaData();
      rs = metaData.getTables(null, "APP", "GLOBALDAT".toUpperCase(), null);
      while (rs.next()) {
        needInit = false;
        String tableName = rs.getString("TABLE_NAME");
        String tableSchema =  rs.getString("TABLE_SCHEM");
        String tableType =  rs.getString("TABLE_TYPE");
      }
      return needInit;
    } finally {
      MiscUtil.close(rs);
      MiscUtil.close(st);
    }
  }


  private DataSource getDataSource() {
    return mDataSource;
  }

  public static class LegacyDriver {

    private static final String JDBC_DRIVER_NAME = "org.apache.derby.jdbc.EmbeddedDriver";

    private static final String DATABASE_NAME = "WeaveDB";

    private static final String DB_CONNECTION_PREFIX = "jdbc:derby:";

    private static final String DB_CONNECTION_URL = DB_CONNECTION_PREFIX + "memory:" + DATABASE_NAME + ";";

    static {
      try {
        if (LEGACY_DRIVER)
          Class.forName(JDBC_DRIVER_NAME).newInstance();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public static Connection getConnection(String urlSuffix) throws SQLException {
      return DriverManager.getConnection(DB_CONNECTION_URL + urlSuffix);
    }

    public static void shutdownDatabase() {
      try {
        DriverManager.getConnection(DB_CONNECTION_PREFIX + ";shutdown=true");
      } catch (SQLException e) {
        e.printStackTrace();
      }
      try {
        for (Enumeration enu = DriverManager.getDrivers(); enu.hasMoreElements();) {
          Driver driver = (Driver)enu.nextElement();
          DriverManager.deregisterDriver(driver);
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

  }
}
