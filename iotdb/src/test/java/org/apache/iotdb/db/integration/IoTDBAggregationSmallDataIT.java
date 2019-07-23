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

/**
 * Multiple aggregation with filter test.
 */
public class IoTDBAggregationSmallDataIT {

  private static final String TIMESTAMP_STR = "Time";
  private final String d0s0 = "root.vehicle.d0.s0";
  private final String d0s1 = "root.vehicle.d0.s1";
  private final String d0s2 = "root.vehicle.d0.s2";
  private final String d0s3 = "root.vehicle.d0.s3";
  private final String d0s4 = "root.vehicle.d0.s4";
  private final String d1s0 = "root.vehicle.d1.s0";
  private final String d1s1 = "root.vehicle.d1.s1";

  private static String[] sqls = new String[]{
      "SET STORAGE GROUP TO root.vehicle",
      "CREATE TIMESERIES root.vehicle.d1.s0 WITH DATATYPE=INT32, ENCODING=RLE",
      "CREATE TIMESERIES root.vehicle.d1.s1 WITH DATATYPE=INT64, ENCODING=RLE",

      "CREATE TIMESERIES root.vehicle.d0.s0 WITH DATATYPE=INT32, ENCODING=RLE",
      "CREATE TIMESERIES root.vehicle.d0.s1 WITH DATATYPE=INT64, ENCODING=RLE",
      "CREATE TIMESERIES root.vehicle.d0.s2 WITH DATATYPE=FLOAT, ENCODING=RLE",
      "CREATE TIMESERIES root.vehicle.d0.s3 WITH DATATYPE=TEXT, ENCODING=PLAIN",
      "CREATE TIMESERIES root.vehicle.d0.s4 WITH DATATYPE=BOOLEAN, ENCODING=PLAIN",

      "insert into root.vehicle.d0(timestamp,s0) values(1,101)",
      "insert into root.vehicle.d0(timestamp,s0) values(2,198)",
      "insert into root.vehicle.d0(timestamp,s0) values(100,99)",
      "insert into root.vehicle.d0(timestamp,s0) values(101,99)",
      "insert into root.vehicle.d0(timestamp,s0) values(102,80)",
      "insert into root.vehicle.d0(timestamp,s0) values(103,99)",
      "insert into root.vehicle.d0(timestamp,s0) values(104,90)",
      "insert into root.vehicle.d0(timestamp,s0) values(105,99)",
      "insert into root.vehicle.d0(timestamp,s0) values(106,99)",
      "insert into root.vehicle.d0(timestamp,s0) values(2,10000)",
      "insert into root.vehicle.d0(timestamp,s0) values(50,10000)",
      "insert into root.vehicle.d0(timestamp,s0) values(1000,22222)",
      "insert into root.vehicle.d0(timestamp,s0) values(106,199)",
      "DELETE FROM root.vehicle.d0.s0 WHERE time < 104",

      "insert into root.vehicle.d0(timestamp,s1) values(1,1101)",
      "insert into root.vehicle.d0(timestamp,s1) values(2,198)",
      "insert into root.vehicle.d0(timestamp,s1) values(100,199)",
      "insert into root.vehicle.d0(timestamp,s1) values(101,199)",
      "insert into root.vehicle.d0(timestamp,s1) values(102,180)",
      "insert into root.vehicle.d0(timestamp,s1) values(103,199)",
      "insert into root.vehicle.d0(timestamp,s1) values(104,190)",
      "insert into root.vehicle.d0(timestamp,s1) values(105,199)",
      "insert into root.vehicle.d0(timestamp,s1) values(2,40000)",
      "insert into root.vehicle.d0(timestamp,s1) values(50,50000)",
      "insert into root.vehicle.d0(timestamp,s1) values(1000,55555)",

      "insert into root.vehicle.d0(timestamp,s2) values(1000,55555)",
      "insert into root.vehicle.d0(timestamp,s2) values(2,2.22)",
      "insert into root.vehicle.d0(timestamp,s2) values(3,3.33)",
      "insert into root.vehicle.d0(timestamp,s2) values(4,4.44)",
      "insert into root.vehicle.d0(timestamp,s2) values(102,10.00)",
      "insert into root.vehicle.d0(timestamp,s2) values(105,11.11)",
      "insert into root.vehicle.d0(timestamp,s2) values(1000,1000.11)",

      "insert into root.vehicle.d0(timestamp,s3) values(60,'aaaaa')",
      "insert into root.vehicle.d0(timestamp,s3) values(70,'bbbbb')",
      "insert into root.vehicle.d0(timestamp,s3) values(80,'ccccc')",
      "insert into root.vehicle.d0(timestamp,s3) values(101,'ddddd')",
      "insert into root.vehicle.d0(timestamp,s3) values(102,'fffff')",

      "insert into root.vehicle.d1(timestamp,s0) values(1,999)",
      "insert into root.vehicle.d1(timestamp,s0) values(1000,888)",

      "insert into root.vehicle.d0(timestamp,s1) values(2000-01-01T08:00:00+08:00, 100)",
      "insert into root.vehicle.d0(timestamp,s3) values(2000-01-01T08:00:00+08:00, 'good')",

      "insert into root.vehicle.d0(timestamp,s4) values(100, false)",
      "insert into root.vehicle.d0(timestamp,s4) values(100, true)"
  };

  private IoTDB daemon;

  @Before
  public void setUp() throws Exception {
    EnvironmentUtils.closeStatMonitor();
    daemon = IoTDB.getInstance();
    daemon.active();
    EnvironmentUtils.envSetUp();

    //Thread.sleep(5000);
    insertSQL();
  }

  @After
  public void tearDown() throws Exception {
    daemon.stop();
    EnvironmentUtils.cleanEnv();
  }

  @Test
  public void countOnlyTimeFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,3,7,4,5,1"
    };

    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute(
          "select count(s0),count(s1),count(s2),count(s3),count(s4) " +
              "from root.vehicle.d0 where time >= 3 and time <= 106");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(count(d0s0))
            + "," + resultSet.getString(count(d0s1)) + "," + resultSet.getString(count(d0s2))
            + "," + resultSet.getString(count(d0s3)) + "," + resultSet.getString(count(d0s4));
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
  public void functionsNoFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,4,0,6,1",
        "0,22222,null,good",
        "0,90,null,aaaaa",
        "0,22222,null,good",
        "0,22610.0,0.0"
    };

    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");

      //select count(d0.s0),count(d1.s1),count(d0.s3),count(d0.s4) from root.vehicle
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute(
          "select count(d0.s0),count(d1.s1),count(d0.s3),count(d0.s4) from root.vehicle");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(count(d0s0))
            + "," + resultSet.getString(count(d1s1)) + "," + resultSet.getString(count(d0s3))
            + "," + resultSet.getString(count(d0s4));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(1, cnt);
      statement.close();

      //select max_value(d0.s0),max_value(d1.s1),max_value(d0.s3) from root.vehicle
      statement = connection.createStatement();
      hasResultSet = statement.execute(
          "select max_value(d0.s0),max_value(d1.s1),max_value(d0.s3) from root.vehicle");
      resultSet = statement.getResultSet();
      Assert.assertTrue(hasResultSet);
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(max_value(d0s0))
            + "," + resultSet.getString(max_value(d1s1)) + ","
            + resultSet.getString(max_value(d0s3));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(2, cnt);
      statement.close();

      //select first(d0.s0),first(d1.s1),first(d0.s3) from root.vehicle
      statement = connection.createStatement();
      hasResultSet = statement.execute(
          "select first(d0.s0),first(d1.s1),first(d0.s3) from root.vehicle");
      resultSet = statement.getResultSet();
      Assert.assertTrue(hasResultSet);
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(first(d0s0))
            + "," + resultSet.getString(first(d1s1)) + "," + resultSet.getString(first(d0s3));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(3, cnt);
      statement.close();

      //select last(d0.s0),last(d1.s1),last(d0.s3) from root.vehicle
      statement = connection.createStatement();
      hasResultSet = statement.execute(
          "select last(d0.s0),last(d1.s1),last(d0.s3) from root.vehicle");
      resultSet = statement.getResultSet();
      Assert.assertTrue(hasResultSet);
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(last(d0s0))
            + "," + resultSet.getString(last(d1s1)) + "," + resultSet.getString(last(d0s3));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(4, cnt);
      statement.close();

      //select sum(d0.s0),sum(d1.s1),sum(d0.s3) from root.vehicle
      statement = connection.createStatement();
      hasResultSet = statement.execute("select sum(d0.s0),sum(d1.s1) from root.vehicle");
      resultSet = statement.getResultSet();
      Assert.assertTrue(hasResultSet);
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(sum(d0s0))
            + "," + resultSet.getString(sum(d1s1));
        Assert.assertEquals(retArray[cnt], ans);
        cnt++;
      }
      Assert.assertEquals(5, cnt);
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
  public void lastAggreWithSingleFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,22222,55555"
    };
    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute(
          "select last(s0),last(s1) from root.vehicle.d0 where s2 >= 3.33");
      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans =
            resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(last(d0s0)) + ","
                + resultSet.getString(last(d0s1));
        //System.out.println("!!!!!============ " + ans);
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
  public void firstAggreWithSingleFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,99,180"
    };
    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute(
          "select first(s0),first(s1) from root.vehicle.d0 where s2 >= 3.33");
      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans =
            resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(first(d0s0)) + ","
                + resultSet.getString(first(d0s1));
        //System.out.println("!!!!!============ " + ans);
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
  public void sumAggreWithSingleFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,22321.0,55934.0,1029"
    };
    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute(
          "select sum(s0),sum(s1),sum(s2) from root.vehicle.d0 where s2 >= 3.33");
      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(sum(d0s0))
            + "," + resultSet.getString(sum(d0s1)) + "," + Math
            .round(resultSet.getDouble(sum(d0s2)));
        //System.out.println("!!!!!============ " + ans);
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
  public void meanAggreWithSingleFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,11160.5,18645,206"
    };
    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute(
          "select mean(s0),mean(s1),mean(s2) from root.vehicle.d0 where s2 >= 3.33");
      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(mean(d0s0))
            + "," + Math.round(resultSet.getDouble(mean(d0s1))) + ","
            + Math.round(resultSet.getDouble(mean(d0s2)));
        //System.out.println("!!!!!============ " + ans);
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
  public void countAggreWithSingleFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,2,3,5,1,0"
    };

    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select count(s0),count(s1),count(s2),count(s3),"
          + "count(s4) from root.vehicle.d0 where s2 >= 3.33");
      // System.out.println(hasResultSet + "...");
      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(count(d0s0))
            + "," + resultSet.getString(count(d0s1)) + "," + resultSet.getString(count(d0s2))
            + "," + resultSet.getString(count(d0s3)) + "," + resultSet.getString(count(d0s4));
        // System.out.println("============ " + ans);
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
  public void minTimeAggreWithSingleFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,104,1,2,101,100"
    };

    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select min_time(s0),min_time(s1),min_time(s2)"
          + ",min_time(s3),min_time(s4) from root.vehicle.d0 " +
          "where s1 < 50000 and s1 != 100");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(min_time(d0s0))
            + "," + resultSet.getString(min_time(d0s1)) + "," + resultSet.getString(min_time(d0s2))
            + "," + resultSet.getString(min_time(d0s3)) + "," + resultSet.getString(min_time(d0s4));
        // System.out.println("============ " + ans);
        Assert.assertEquals(ans, retArray[cnt]);
        cnt++;
        Assert.assertEquals(1, cnt);
      }
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
  public void maxTimeAggreWithSingleFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,105,105,105,102,100"
    };

    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select max_time(s0),max_time(s1),max_time(s2)"
          + ",max_time(s3),max_time(s4) from root.vehicle.d0 " +
          "where s1 < 50000 and s1 != 100");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(max_time(d0s0))
            + "," + resultSet.getString(max_time(d0s1)) + "," + resultSet.getString(max_time(d0s2))
            + "," + resultSet.getString(max_time(d0s3)) + "," + resultSet.getString(max_time(d0s4));
        // System.out.println("============ " + ans);
        Assert.assertEquals(ans, retArray[cnt]);
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
  public void minValueAggreWithSingleFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,90,180,2.22,ddddd,true"
    };

    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select min_value(s0),min_value(s1),min_value(s2)"
          + ",min_value(s3),min_value(s4) from root.vehicle.d0 " +
          "where s1 < 50000 and s1 != 100");
      Assert.assertTrue(hasResultSet);

      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(min_value(d0s0))
            + "," + resultSet.getString(min_value(d0s1)) +
            "," + resultSet.getString(min_value(d0s2))
            + "," + resultSet.getString(min_value(d0s3)) + ","
            + resultSet.getString(min_value(d0s4));
        // System.out.println("============ " + ans);
        Assert.assertEquals(ans, retArray[cnt]);
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
  public void maxValueAggreWithSingleFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,99,50000,11.11,fffff,true"
    };

    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select max_value(s0),max_value(s1),max_value(s2),"
          + "max_value(s3),max_value(s4) from root.vehicle.d0 " +
          "where s1 < 50000 and s1 != 100");

      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(max_value(d0s0))
            + "," + resultSet.getString(max_value(d0s1)) + "," + resultSet
            .getString(max_value(d0s2))
            + "," + resultSet.getString(max_value(d0s3)) + "," + resultSet
            .getString(max_value(d0s4));
        //System.out.println("============ " + ans);
        //Assert.assertEquals(ans, retArray[cnt]);
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
  public void countAggreWithMultiMultiFilterTest() throws ClassNotFoundException, SQLException {
    String[] retArray = new String[]{
        "0,2",
    };

    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute(
          "select count(s0) from root.vehicle.d0 where s2 >= 3.33");
      // System.out.println(hasResultSet + "...");
      Assert.assertTrue(hasResultSet);
      ResultSet resultSet = statement.getResultSet();
      int cnt = 0;
      while (resultSet.next()) {
        String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(count(d0s0));
        //System.out.println("============ " + ans);
        Assert.assertEquals(ans, retArray[cnt]);
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
  public void selectAllSQLTest() throws ClassNotFoundException, SQLException {
    //d0s0,d0s1,d0s2,d0s3,d1s0
    String[] retArray = new String[]{
        "1,null,1101,null,null,999",
        "2,null,40000,2.22,null,null",
        "3,null,null,3.33,null,null",
        "4,null,null,4.44,null,null",
        "50,null,50000,null,null,null",
        "60,null,null,null,aaaaa,null",
        "70,null,null,null,bbbbb,null",
        "80,null,null,null,ccccc,null",
        "100,null,199,null,null,null",
        "101,null,199,null,ddddd,null",
        "102,null,180,10.0,fffff,null",
        "103,null,199,null,null,null",
        "104,90,190,null,null,null",
        "105,99,199,11.11,null,null",
        "106,199,null,null,null,null",
        "1000,22222,55555,1000.11,null,888",
        "946684800000,null,100,null,good,null"
    };

    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.
          getConnection("jdbc:iotdb://127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      boolean hasResultSet = statement.execute("select * from root");
      // System.out.println(hasResultSet + "...");
      if (hasResultSet) {
        ResultSet resultSet = statement.getResultSet();
        int cnt = 0;
        while (resultSet.next()) {
          String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(d0s0) + ","
              + resultSet.getString(d0s1) + "," + resultSet.getString(d0s2) + "," +
              resultSet.getString(d0s3) + "," + resultSet.getString(d1s0);
          // System.out.println(ans);
          Assert.assertEquals(ans, retArray[cnt]);
          cnt++;
        }
        Assert.assertEquals(17, cnt);
      }
      statement.close();

      retArray = new String[]{
          "100,true"
      };
      statement = connection.createStatement();
      hasResultSet = statement.execute("select s4 from root.vehicle.d0");
      if (hasResultSet) {
        ResultSet resultSet = statement.getResultSet();
        int cnt = 0;
        while (resultSet.next()) {
          String ans = resultSet.getString(TIMESTAMP_STR) + "," + resultSet.getString(d0s4);
          Assert.assertEquals(ans, retArray[cnt]);
          cnt++;
        }
        Assert.assertEquals(1, cnt);
      }
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

  public static void insertSQL() throws ClassNotFoundException, SQLException {
    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager.getConnection
          (Config.IOTDB_URL_PREFIX + "127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();
      for (String sql : sqls) {
        statement.execute(sql);
      }
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

}
