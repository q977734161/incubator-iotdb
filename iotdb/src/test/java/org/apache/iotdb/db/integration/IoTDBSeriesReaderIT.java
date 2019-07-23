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

import static org.apache.iotdb.db.utils.EnvironmentUtils.TEST_QUERY_CONTEXT;
import static org.apache.iotdb.db.utils.EnvironmentUtils.TEST_QUERY_JOB_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.exception.StorageEngineException;
import org.apache.iotdb.db.query.context.QueryContext;
import org.apache.iotdb.db.query.control.QueryResourceManager;
import org.apache.iotdb.db.query.executor.EngineQueryRouter;
import org.apache.iotdb.db.service.IoTDB;
import org.apache.iotdb.db.utils.EnvironmentUtils;
import org.apache.iotdb.jdbc.Config;
import org.apache.iotdb.tsfile.common.conf.TSFileConfig;
import org.apache.iotdb.tsfile.common.conf.TSFileDescriptor;
import org.apache.iotdb.tsfile.read.common.Path;
import org.apache.iotdb.tsfile.read.common.RowRecord;
import org.apache.iotdb.tsfile.read.expression.QueryExpression;
import org.apache.iotdb.tsfile.read.expression.impl.SingleSeriesExpression;
import org.apache.iotdb.tsfile.read.filter.TimeFilter;
import org.apache.iotdb.tsfile.read.filter.ValueFilter;
import org.apache.iotdb.tsfile.read.query.dataset.QueryDataSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Notice that, all test begins with "IoTDB" is integration test. All test which will start the IoTDB server should be
 * defined as integration test.
 */
public class IoTDBSeriesReaderIT {

  private static IoTDB daemon;

  private static TSFileConfig tsFileConfig = TSFileDescriptor.getInstance().getConfig();
  private static int maxNumberOfPointsInPage;
  private static int pageSizeInByte;
  private static int groupSizeInByte;

  private static Connection connection;

  @BeforeClass
  public static void setUp() throws Exception {
    EnvironmentUtils.closeStatMonitor();

    // use small page setting
    // origin value
    maxNumberOfPointsInPage = tsFileConfig.maxNumberOfPointsInPage;
    pageSizeInByte = tsFileConfig.pageSizeInByte;
    groupSizeInByte = tsFileConfig.groupSizeInByte;

    // new value
    tsFileConfig.maxNumberOfPointsInPage = 1000;
    tsFileConfig.pageSizeInByte = 1024 * 1024 * 150;
    tsFileConfig.groupSizeInByte = 1024 * 1024 * 1000;
    IoTDBDescriptor.getInstance().getConfig().setMemtableSizeThreshold(1024 * 1024 * 1000);

    daemon = IoTDB.getInstance();
    daemon.active();
    EnvironmentUtils.envSetUp();

    Thread.sleep(1000);
    insertData();
    connection = DriverManager
        .getConnection(Config.IOTDB_URL_PREFIX + "127.0.0.1:6667/", "root", "root");

  }

  @AfterClass
  public static void tearDown() throws Exception {
    connection.close();
    daemon.stop();
    // recovery value
    tsFileConfig.maxNumberOfPointsInPage = maxNumberOfPointsInPage;
    tsFileConfig.pageSizeInByte = pageSizeInByte;
    tsFileConfig.groupSizeInByte = groupSizeInByte;
    IoTDBDescriptor.getInstance().getConfig().setMemtableSizeThreshold(groupSizeInByte);

    EnvironmentUtils.cleanEnv();
  }

  private static void insertData() throws ClassNotFoundException, SQLException {
    Class.forName(Config.JDBC_DRIVER_NAME);
    Connection connection = null;
    try {
      connection = DriverManager
          .getConnection(Config.IOTDB_URL_PREFIX + "127.0.0.1:6667/", "root", "root");
      Statement statement = connection.createStatement();

      for (String sql : Constant.create_sql) {
        statement.execute(sql);
      }

      // insert large amount of data time range : 3000 ~ 13600
      for (int time = 3000; time < 13600; time++) {
        // System.out.println("===" + time);
        String sql = String
            .format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, time % 100);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, time % 17);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, time % 22);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s3) values(%s,'%s')", time,
            Constant.stringValue[time % 5]);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s4) values(%s, %s)", time,
            Constant.booleanValue[time % 2]);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s5) values(%s, %s)", time, time);
        statement.execute(sql);
      }

      // statement.execute("flush");

      // insert large amount of data time range : 13700 ~ 24000
      for (int time = 13700; time < 24000; time++) {

        String sql = String
            .format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, time % 70);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, time % 40);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, time % 123);
        statement.execute(sql);
      }

//      statement.execute("merge");

      Thread.sleep(5000);

      // buffwrite data, unsealed file
      for (int time = 100000; time < 101000; time++) {

        String sql = String
            .format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, time % 20);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, time % 30);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, time % 77);
        statement.execute(sql);
      }

      statement.execute("flush");

      // sequential data, memory data
      for (int time = 200000; time < 201000; time++) {

        String sql = String
            .format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, -time % 20);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, -time % 30);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, -time % 77);
        statement.execute(sql);
      }

      // unsequence insert, time < 3000
      for (int time = 2000; time < 2500; time++) {

        String sql = String
            .format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, time);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, time + 1);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, time + 2);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s3) values(%s,'%s')", time,
            Constant.stringValue[time % 5]);
        statement.execute(sql);
      }

      // unsequence insert, time > 200000
      for (int time = 200900; time < 201000; time++) {

        String sql = String
            .format("insert into root.vehicle.d0(timestamp,s0) values(%s,%s)", time, 6666);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s1) values(%s,%s)", time, 7777);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s2) values(%s,%s)", time, 8888);
        statement.execute(sql);
        sql = String
            .format("insert into root.vehicle.d0(timestamp,s3) values(%s,'%s')", time, "goodman");
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s4) values(%s, %s)", time,
            Constant.booleanValue[time % 2]);
        statement.execute(sql);
        sql = String.format("insert into root.vehicle.d0(timestamp,s5) values(%s, %s)", time, 9999);
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

  @Test
  public void selectAllTest() throws IOException, StorageEngineException {
    String selectSql = "select * from root";
    //System.out.println("Test >>> " + selectSql);

    EngineQueryRouter engineExecutor = new EngineQueryRouter();
    QueryExpression queryExpression = QueryExpression.create();
    queryExpression.addSelectedPath(new Path(Constant.d0s0));
    queryExpression.addSelectedPath(new Path(Constant.d0s1));
    queryExpression.addSelectedPath(new Path(Constant.d0s2));
    queryExpression.addSelectedPath(new Path(Constant.d0s3));
    queryExpression.addSelectedPath(new Path(Constant.d0s4));
    queryExpression.addSelectedPath(new Path(Constant.d0s5));
    queryExpression.addSelectedPath(new Path(Constant.d1s0));
    queryExpression.addSelectedPath(new Path(Constant.d1s1));
    queryExpression.setExpression(null);

    TEST_QUERY_JOB_ID = QueryResourceManager.getInstance().assignJobId();
    TEST_QUERY_CONTEXT = new QueryContext(TEST_QUERY_JOB_ID);
    QueryDataSet queryDataSet = engineExecutor.query(queryExpression, TEST_QUERY_CONTEXT);

    int cnt = 0;
    while (queryDataSet.hasNext()) {
      RowRecord rowRecord = queryDataSet.next();
      cnt++;
    }
    assertEquals(23400, cnt);

    QueryResourceManager.getInstance().endQueryForGivenJob(TEST_QUERY_JOB_ID);
  }

  @Test
  public void selectOneSeriesWithValueFilterTest() throws IOException, StorageEngineException {

    String selectSql = "select s0 from root.vehicle.d0 where s0 >= 20";
    //System.out.println("Test >>> " + selectSql);

    EngineQueryRouter engineExecutor = new EngineQueryRouter();
    QueryExpression queryExpression = QueryExpression.create();
    Path p = new Path(Constant.d0s0);
    queryExpression.addSelectedPath(p);
    SingleSeriesExpression singleSeriesExpression = new SingleSeriesExpression(p,
        ValueFilter.gtEq(20));
    queryExpression.setExpression(singleSeriesExpression);

    TEST_QUERY_JOB_ID = QueryResourceManager.getInstance().assignJobId();
    TEST_QUERY_CONTEXT = new QueryContext(TEST_QUERY_JOB_ID);
    QueryDataSet queryDataSet = engineExecutor.query(queryExpression, TEST_QUERY_CONTEXT);

    int cnt = 0;
    while (queryDataSet.hasNext()) {
      RowRecord rowRecord = queryDataSet.next();
      String result = rowRecord.toString();
      // System.out.println(result);
      cnt++;
    }
    assertEquals(16440, cnt);

    QueryResourceManager.getInstance().endQueryForGivenJob(TEST_QUERY_JOB_ID);
  }

  @Test
  public void seriesTimeDigestReadTest() throws IOException, StorageEngineException {
    String selectSql = "select s0 from root.vehicle.d0 where time >= 22987";
    //System.out.println("Test >>> " + selectSql);

    EngineQueryRouter engineExecutor = new EngineQueryRouter();
    QueryExpression queryExpression = QueryExpression.create();
    Path path = new Path(Constant.d0s0);
    queryExpression.addSelectedPath(path);
    SingleSeriesExpression expression = new SingleSeriesExpression(path, TimeFilter.gt(22987L));
    queryExpression.setExpression(expression);

    TEST_QUERY_JOB_ID = QueryResourceManager.getInstance().assignJobId();
    TEST_QUERY_CONTEXT = new QueryContext(TEST_QUERY_JOB_ID);
    QueryDataSet queryDataSet = engineExecutor.query(queryExpression, TEST_QUERY_CONTEXT);

    int cnt = 0;
    while (queryDataSet.hasNext()) {
      RowRecord rowRecord = queryDataSet.next();
      String result = rowRecord.toString();
      // System.out.println(result);
      cnt++;
    }
    assertEquals(3012, cnt);

    QueryResourceManager.getInstance().endQueryForGivenJob(TEST_QUERY_JOB_ID);
  }

  @Test
  public void crossSeriesReadUpdateTest() throws IOException, StorageEngineException {
    //System.out.println("Test >>> select s1 from root.vehicle.d0 where s0 < 111");
    EngineQueryRouter engineExecutor = new EngineQueryRouter();
    QueryExpression queryExpression = QueryExpression.create();
    Path path1 = new Path(Constant.d0s0);
    Path path2 = new Path(Constant.d0s1);
    queryExpression.addSelectedPath(path1);
    queryExpression.addSelectedPath(path2);
    SingleSeriesExpression singleSeriesExpression = new SingleSeriesExpression(path1,
        ValueFilter.lt(111));
    queryExpression.setExpression(singleSeriesExpression);

    TEST_QUERY_JOB_ID = QueryResourceManager.getInstance().assignJobId();
    TEST_QUERY_CONTEXT = new QueryContext(TEST_QUERY_JOB_ID);
    QueryDataSet queryDataSet = engineExecutor.query(queryExpression, TEST_QUERY_CONTEXT);

    int cnt = 0;
    while (queryDataSet.hasNext()) {
      RowRecord rowRecord = queryDataSet.next();
      // long time = rowRecord.getTimestamp();
      // String value = rowRecord.getFields().get(1).getStringValue();
      cnt++;
    }
    assertEquals(22800, cnt);

    QueryResourceManager.getInstance().endQueryForGivenJob(TEST_QUERY_JOB_ID);
  }

  @Test
  public void queryEmptySeriesTest() throws SQLException {
    Statement statement = connection.createStatement();
    statement.execute("CREATE TIMESERIES root.vehicle.d_empty.s1 WITH DATATYPE=INT64, ENCODING=RLE");
    ResultSet resultSet = statement.executeQuery("select * from root.vehicle.d_empty");
    assertFalse (resultSet.next());
    resultSet.close();
  }
}
