/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.integration;

import static org.apache.iotdb.db.integration.Constant.count;
import static org.apache.iotdb.db.integration.Constant.first;
import static org.apache.iotdb.db.integration.Constant.last;
import static org.apache.iotdb.db.integration.Constant.max_time;
import static org.apache.iotdb.db.integration.Constant.max_value;
import static org.apache.iotdb.db.integration.Constant.mean;
import static org.apache.iotdb.db.integration.Constant.min_time;
import static org.apache.iotdb.db.integration.Constant.min_value;
import static org.apache.iotdb.db.integration.Constant.sum;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.iotdb.db.service.IoTDB;
import org.apache.iotdb.db.utils.EnvironmentUtils;
import org.apache.iotdb.jdbc.Config;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IoTDBAggregationTestIT {
  private static IoTDB daemon;

  private static String[] creationSqls = new String[]{
      "SET STORAGE GROUP TO root.vehicle.d0", "SET STORAGE GROUP TO root.vehicle.d1",

      "CREATE TIMESERIES root.vehicle.d0.s0 WITH DATATYPE=INT32, ENCODING=RLE",
      "CREATE TIMESERIES root.vehicle.d0.s1 WITH DATATYPE=INT64, ENCODING=RLE",
      "CREATE TIMESERIES root.vehicle.d0.s2 WITH DATATYPE=FLOAT, ENCODING=RLE",
      "CREATE TIMESERIES root.vehicle.d0.s3 WITH DATATYPE=TEXT, ENCODING=PLAIN",
  };

  private String insertTemplate = "INSERT INTO root.vehicle.d0(timestamp,s0,s1,s2,s3"
      + ") VALUES(%d,%d,%d,%f,%s)";

  private static final String TIMESTAMP_STR = "Time";
  private final String d0s0 = "root.vehicle.d0.s0";
  private final String d0s1 = "root.vehicle.d0.s1";
  private final String d0s2 = "root.vehicle.d0.s2";
  private final String d0s3 = "root.vehicle.d0.s3";

  @Before
  public void setUp() throws Exception {
    EnvironmentUtils.closeStatMonitor();
    EnvironmentUtils.closeMemControl();
    daemon = IoTDB.getInstance();
    daemon.active();
    EnvironmentUtils.envSetUp();
    Class.forName(Config.JDBC_DRIVER_NAME);
    prepareData();
  }

  @After
  public void tearDown() throws Exception {
    daemon.stop();
    EnvironmentUtils.cleanEnv();
  }

  @Test
  public void countTest() throws SQLException {
    String[] retArray = new String[]{
        "0,2001,2001,2001,2001"
    };
    Connection connection = null;
    try {
      connection = DriverManager.getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select count(s0),count(s1),count(s2),count(s3) " +
          "from root.vehicle.d0 where time >= 6000 and time <= 9000");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(count(d0s0))
            + "," + resultSet.getString(count(d0s1)) + "," + resultSet.getString(count(d0s2))
            + "," + resultSet.getString(count(d0s3));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(1, cnt);
      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @Test
  public void firstTest() throws SQLException {
    String[] retArray = new String[]{
        "0,2000,2000,2000.0,2000"
    };
    Connection connection = null;
    try {
      connection = DriverManager.getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select first(s0),first(s1),first(s2),first(s3) " +
          "from root.vehicle.d0 where time >= 1500 and time <= 9000");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(first(d0s0))
            + "," + resultSet.getString(first(d0s1)) + "," + resultSet.getString(first(d0s2))
            + "," + resultSet.getString(first(d0s3));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(1, cnt);
      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @Test
  public void lastTest() throws SQLException {
    String[] retArray = new String[]{
        "0,8499,8499.0"
    };
    Connection connection = null;
    try {
      connection = DriverManager.getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select last(s0),last(s2) " +
          "from root.vehicle.d0 where time >= 1500 and time < 9000");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(last(d0s0))
            + "," + resultSet.getString(last(d0s2));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(1, cnt);
      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @Test
  public void maxminTimeTest() throws SQLException {
    String[] retArray = new String[]{
        "0,8499,500"
    };
    Connection connection = null;
    try {
      connection = DriverManager.getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select max_time(s0),min_time(s2) " +
          "from root.vehicle.d0 where time >= 100 and time < 9000");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(max_time(d0s0))
            + "," + resultSet.getString(min_time(d0s2));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(1, cnt);
      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @Test
  public void maxminValueTest() throws SQLException {
    String[] retArray = new String[]{
        "0,8499,500.0"
    };
    Connection connection = null;
    try {
      connection = DriverManager.getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select max_value(s0),min_value(s2) " +
          "from root.vehicle.d0 where time >= 100 and time < 9000");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(max_value(d0s0))
            + "," + resultSet.getString(min_value(d0s2));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(1, cnt);
      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @Test
  public void meanSumTest() throws SQLException {
    String[] retArray = new String[]{
        "0,1.4508E7,7250.374812593703"
    };
    Connection connection = null;
    try {
      connection = DriverManager.getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select sum(s0),mean(s2)" +
          "from root.vehicle.d0 where time >= 6000 and time <= 9000");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(sum(d0s0))
            + "," + resultSet.getString(mean(d0s2));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(1, cnt);
      statement.close();
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  private void prepareData() throws SQLException {
    Connection connection = null;
    try {
      connection = DriverManager
          .getConnection(Config.IOTDB_URL_PREFIX + "127.0.0.1:6667/", "root",
              "root");
      Statement statement = connection.createStatement();
      for (String sql : creationSqls) {
        statement.execute(sql);
      }
      statement.close();

      statement = connection.createStatement();
      // prepare BufferWrite file
      for (int i = 5000; i < 7000; i++) {
        statement.execute(String.format(insertTemplate, i, i, i, (double) i, "\'" + i + "\'"));
      }
      statement.execute("flush");
      for (int i = 7500; i < 8500; i++) {
        statement.execute(String.format(insertTemplate, i, i, i, (double) i, "\'" + i + "\'"));
      }
      statement.execute("flush");
      // prepare Unseq-File
      for (int i = 500; i < 1500; i++) {
        statement.execute(String.format(insertTemplate, i, i, i, (double) i, "\'" + i + "\'"));
      }
      statement.execute("flush");
      for (int i = 3000; i < 6500; i++) {
        statement.execute(String.format(insertTemplate, i, i, i, (double) i, "\'" + i + "\'"));
      }
      statement.execute("merge");

      // prepare BufferWrite cache
      for (int i = 9000; i < 10000; i++) {
        statement.execute(String.format(insertTemplate, i, i, i, (double) i, "\'" + i + "\'"));
      }
      // prepare Overflow cache
      for (int i = 2000; i < 2500; i++) {
        statement.execute(String.format(insertTemplate, i, i, i, (double) i, "\'" + i + "\'"));
      }
      statement.close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }
}