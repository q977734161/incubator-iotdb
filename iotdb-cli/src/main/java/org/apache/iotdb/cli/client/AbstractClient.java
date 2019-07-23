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
package org.apache.iotdb.cli.client;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.iotdb.cli.exception.ArgsErrorException;
import org.apache.iotdb.cli.tool.ImportCsv;
import org.apache.iotdb.jdbc.IoTDBConnection;
import org.apache.iotdb.jdbc.IoTDBDatabaseMetadata;
import org.apache.iotdb.jdbc.IoTDBMetadataResultSet;
import org.apache.iotdb.jdbc.IoTDBQueryResultSet;
import org.apache.iotdb.jdbc.IoTDBSQLException;
import org.apache.iotdb.service.rpc.thrift.ServerProperties;
import org.apache.thrift.TException;

public abstract class AbstractClient {

  protected static final String HOST_ARGS = "h";
  protected static final String HOST_NAME = "host";

  protected static final String HELP_ARGS = "help";

  protected static final String PORT_ARGS = "p";
  protected static final String PORT_NAME = "port";

  protected static final String PASSWORD_ARGS = "pw";
  protected static final String PASSWORD_NAME = "password";

  protected static final String USERNAME_ARGS = "u";
  protected static final String USERNAME_NAME = "username";

  protected static final String ISO8601_ARGS = "disableISO8601";
  protected static final List<String> AGGREGRATE_TIME_LIST = new ArrayList<>();
  protected static final String MAX_PRINT_ROW_COUNT_ARGS = "maxPRC";
  protected static final String MAX_PRINT_ROW_COUNT_NAME = "maxPrintRowCount";
  protected static final String SET_MAX_DISPLAY_NUM = "set max_display_num";
  protected static final String SET_TIMESTAMP_DISPLAY = "set time_display_type";
  protected static final String SHOW_TIMESTAMP_DISPLAY = "show time_display_type";
  protected static final String SET_TIME_ZONE = "set time_zone";
  protected static final String SHOW_TIMEZONE = "show time_zone";
  protected static final String SET_FETCH_SIZE = "set fetch_size";
  protected static final String SHOW_FETCH_SIZE = "show fetch_size";
  protected static final String HELP = "help";
  protected static final String IOTDB_CLI_PREFIX = "IoTDB";
  protected static final String SCRIPT_HINT = "./start-client.sh(start-client.bat if Windows)";
  protected static final String QUIT_COMMAND = "quit";
  protected static final String EXIT_COMMAND = "exit";
  protected static final String SHOW_METADATA_COMMAND = "show timeseries";
  protected static final int MAX_HELP_CONSOLE_WIDTH = 88;
  protected static final String TIMESTAMP_STR = "Time";
  protected static final int ISO_DATETIME_LEN = 26;
  protected static final String IMPORT_CMD = "import";
  private static final String NEED_NOT_TO_PRINT_TIMESTAMP = "AGGREGATION";
  private static final String DEFAULT_TIME_FORMAT = "default";
  protected static String timeFormat = DEFAULT_TIME_FORMAT;
  protected static int maxPrintRowCount = 1000;
  protected static int fetchSize = 10000;
  protected static int maxTimeLength = ISO_DATETIME_LEN;
  protected static int maxValueLength = 15;
  protected static boolean isQuit = false;
  /**
   * control the width of columns for 'show timeseries path' and 'show storage group'.
   * <p>
   * for 'show timeseries path':
   * <table>
   * <tr>
   * <th>Timeseries (width:75)</th>
   * <th>Storage Group (width:45)</th>
   * <th>DataType width:8)</th>
   * <th>Encoding (width:8)</th>
   * </tr>
   * <tr>
   * <td>root.vehicle.d1.s1</td>
   * <td>root.vehicle</td>
   * <td>INT32</td>
   * <td>PLAIN</td>
   * </tr>
   * <tr>
   * <td>...</td>
   * <td>...</td>
   * <td>...</td>
   * <td>...</td>
   * </tr>
   * </table>
   * </p>
   * <p>
   * for "show storage group path":
   * <table>
   * <tr>
   * <th>STORAGE_GROUP (width:75)</th>
   * </tr>
   * <tr>
   * <td>root.vehicle</td>
   * </tr>
   * <tr>
   * <td>...</td>
   * </tr>
   * </table>
   * </p>
   */
  protected static int[] maxValueLengthForShow = new int[]{75, 45, 8, 8};
  protected static String formatTime = "%" + maxTimeLength + "s|";
  protected static String formatValue = "%" + maxValueLength + "s|";
  private static final int DIVIDING_LINE_LENGTH = 40;
  protected static String host = "127.0.0.1";
  protected static String port = "6667";
  protected static String username;
  protected static String password;

  protected static boolean printToConsole = true;

  protected static Set<String> keywordSet = new HashSet<>();

  protected static ServerProperties properties = null;

  private static boolean printHeader = false;
  private static int displayCnt = 0;

  private static final PrintStream SCREEN_PRINTER = new PrintStream(System.out);
  /**
   * showException is currently fixed to false because the display of exceptions is not elaborate.
   * We can make it an option in future versions.
   */
  private static boolean showException = false;

  protected static void init() {
    keywordSet.add("-" + HOST_ARGS);
    keywordSet.add("-" + HELP_ARGS);
    keywordSet.add("-" + PORT_ARGS);
    keywordSet.add("-" + PASSWORD_ARGS);
    keywordSet.add("-" + USERNAME_ARGS);
    keywordSet.add("-" + ISO8601_ARGS);
    keywordSet.add("-" + MAX_PRINT_ROW_COUNT_ARGS);
  }

  /**
   * CLI result output.
   *
   * @param res result set
   * @param printToConsole print to console
   * @param zoneId time-zone ID
   * @throws SQLException SQLException
   */
  public static void output(ResultSet res, boolean printToConsole, ZoneId zoneId)
      throws SQLException {
    int cnt = 0;
    boolean printTimestamp = true;
    displayCnt = 0;
    printHeader = false;
    ResultSetMetaData resultSetMetaData = res.getMetaData();

    int colCount = resultSetMetaData.getColumnCount();

    boolean isShow = res instanceof IoTDBMetadataResultSet;
    if (!isShow && resultSetMetaData.getColumnTypeName(0) != null) {
      printTimestamp = !res.getMetaData().getColumnTypeName(0).equalsIgnoreCase(NEED_NOT_TO_PRINT_TIMESTAMP);
    }
    if (res instanceof IoTDBQueryResultSet) {
      printTimestamp = printTimestamp && !((IoTDBQueryResultSet) res).isIgnoreTimeStamp();
    }

    // Output values
    while (res.next()) {
      printRow(printTimestamp, colCount, resultSetMetaData, isShow, res, zoneId);
      cnt++;
      if (!printToConsole && cnt % 10000 == 0) {
        println(cnt);
      }
    }

    if (printToConsole) {
      if (!printHeader) {
        printBlockLine(printTimestamp, colCount, resultSetMetaData, isShow);
        printName(printTimestamp, colCount, resultSetMetaData, isShow);
        printBlockLine(printTimestamp, colCount, resultSetMetaData, isShow);
      } else {
        printBlockLine(printTimestamp, colCount, resultSetMetaData, isShow);
      }
      if (displayCnt == maxPrintRowCount) {
        println(String.format("Reach maxPrintRowCount = %s lines", maxPrintRowCount));
      }
    }

    println(StringUtils.repeat('-', DIVIDING_LINE_LENGTH));
    printCount(isShow, res, cnt);
  }

  protected static void printCount(boolean isShow, ResultSet res, int cnt) throws SQLException {
    if (isShow) {
      int type = res.getType();
      if (type == IoTDBMetadataResultSet.MetadataType.STORAGE_GROUP.ordinal()) { // storage group
        println("Total storage group number = " + cnt);
      } else if (type == IoTDBMetadataResultSet.MetadataType.TIMESERIES
          .ordinal()) { // show timeseries <path>
        println("Total timeseries number = " + cnt);
      }
    } else {
      println("Total line number = " + cnt);
    }
  }

  protected static void printRow(boolean printTimestamp, int colCount,
      ResultSetMetaData resultSetMetaData, boolean isShow, ResultSet res, ZoneId zoneId)
      throws SQLException {
    // Output Labels
    if (!printToConsole) {
      return;
    }
    printHeader(printTimestamp, colCount, resultSetMetaData, isShow);

    if (isShow) { // 'show timeseries <path>' or 'show storage group' metadata results
      printShow(colCount, res);
    } else { // queried data results
      printRowData(printTimestamp, res, zoneId, resultSetMetaData, colCount);
    }
  }

  protected static void printHeader(boolean printTimestamp, int colCount,
      ResultSetMetaData resultSetMetaData, boolean isShow) throws SQLException {
    if (!printHeader) {
      printBlockLine(printTimestamp, colCount, resultSetMetaData, isShow);
      printName(printTimestamp, colCount, resultSetMetaData, isShow);
      printBlockLine(printTimestamp, colCount, resultSetMetaData, isShow);
      printHeader = true;
    }
  }

  protected static void printShow(int colCount, ResultSet res) throws SQLException {
    print("|");
    for (int i = 1; i <= colCount; i++) {
      formatValue = "%" + maxValueLengthForShow[i - 1] + "s|";
      printf(formatValue, String.valueOf(res.getString(i)));
    }
    println();
  }

  protected static void printRowData(boolean printTimestamp, ResultSet res, ZoneId zoneId,
      ResultSetMetaData resultSetMetaData, int colCount)
      throws SQLException {
    if (displayCnt < maxPrintRowCount) { // NOTE displayCnt only works on queried data results
      print("|");
      if (printTimestamp) {
        printf(formatTime, formatDatetime(res.getLong(TIMESTAMP_STR), zoneId));
      }
      for (int i = 2; i <= colCount; i++) {
        printColumnData(resultSetMetaData, res, i, zoneId);
      }
      println();
      displayCnt++;
    }
  }

  protected static void printColumnData(ResultSetMetaData resultSetMetaData, ResultSet res, int i,
      ZoneId zoneId) throws SQLException {
    boolean flag = false;
    for (String timeStr : AGGREGRATE_TIME_LIST) {
      if (resultSetMetaData.getColumnLabel(i).toUpperCase().contains(timeStr.toUpperCase())) {
        flag = true;
        break;
      }
    }
    if (flag) {
      try {
        printf(formatValue, formatDatetime(res.getLong(i), zoneId));
      } catch (Exception e) {
        printf(formatValue, "null");
        handleException(e);
      }
    } else {
      printf(formatValue, String.valueOf(res.getString(i)));
    }
  }

  protected static Options createOptions() {
    Options options = new Options();
    Option help = new Option(HELP_ARGS, false, "Display help information(optional)");
    help.setRequired(false);
    options.addOption(help);

    Option timeFormat = new Option(ISO8601_ARGS, false, "Display timestamp in number(optional)");
    timeFormat.setRequired(false);
    options.addOption(timeFormat);

    Option host = Option.builder(HOST_ARGS).argName(HOST_NAME).hasArg()
        .desc("Host Name (optional, default 127.0.0.1)").build();
    options.addOption(host);

    Option port = Option.builder(PORT_ARGS).argName(PORT_NAME).hasArg()
        .desc("Port (optional, default 6667)")
        .build();
    options.addOption(port);

    Option username = Option.builder(USERNAME_ARGS).argName(USERNAME_NAME).hasArg()
        .desc("User name (required)")
        .required().build();
    options.addOption(username);

    Option password = Option.builder(PASSWORD_ARGS).argName(PASSWORD_NAME).hasArg()
        .desc("password (optional)")
        .build();
    options.addOption(password);

    Option maxPrintCount = Option.builder(MAX_PRINT_ROW_COUNT_ARGS)
        .argName(MAX_PRINT_ROW_COUNT_NAME).hasArg()
        .desc("Maximum number of rows displayed (optional)").build();
    options.addOption(maxPrintCount);
    return options;
  }

  private static String formatDatetime(long timestamp, ZoneId zoneId) {
    ZonedDateTime dateTime;
    switch (timeFormat) {
      case "long":
      case "number":
        return Long.toString(timestamp);
      case DEFAULT_TIME_FORMAT:
      case "iso8601":
        dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId);
        return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      default:
        dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId);
        return dateTime.format(DateTimeFormatter.ofPattern(timeFormat));
    }
  }

  protected static String checkRequiredArg(String arg, String name, CommandLine commandLine,
      boolean isRequired,
      String defaultValue) throws ArgsErrorException {
    String str = commandLine.getOptionValue(arg);
    if (str == null) {
      if (isRequired) {
        String msg = String
            .format("%s: Required values for option '%s' not provided", IOTDB_CLI_PREFIX, name);
        println(msg);
        println("Use -help for more information");
        throw new ArgsErrorException(msg);
      } else if (defaultValue == null) {
        String msg = String
            .format("%s: Required values for option '%s' is null.", IOTDB_CLI_PREFIX, name);
        throw new ArgsErrorException(msg);
      } else {
        return defaultValue;
      }
    }
    return str;
  }

  protected static void setTimeFormat(String newTimeFormat) {
    switch (newTimeFormat.trim().toLowerCase()) {
      case "long":
      case "number":
        maxTimeLength = maxValueLength;
        timeFormat = newTimeFormat.trim().toLowerCase();
        break;
      case DEFAULT_TIME_FORMAT:
      case "iso8601":
        maxTimeLength = ISO_DATETIME_LEN;
        timeFormat = newTimeFormat.trim().toLowerCase();
        break;
      default:
        // use java default SimpleDateFormat to check whether input time format is legal
        // if illegal, it will throw an exception
        new SimpleDateFormat(newTimeFormat.trim());
        maxTimeLength = TIMESTAMP_STR.length() > newTimeFormat.length() ? TIMESTAMP_STR.length()
            : newTimeFormat.length();
        timeFormat = newTimeFormat;
        break;
    }
    formatTime = "%" + maxTimeLength + "s|";
  }

  private static void setFetchSize(String fetchSizeString) {
    long tmp = Long.parseLong(fetchSizeString.trim());
    if (tmp > Integer.MAX_VALUE || tmp < 0) {
      fetchSize = Integer.MAX_VALUE;
    } else {
      fetchSize = Integer.parseInt(fetchSizeString.trim());
    }
  }

  protected static void setMaxDisplayNumber(String maxDisplayNum) {
    long tmp = Long.parseLong(maxDisplayNum.trim());
    if (tmp > Integer.MAX_VALUE || tmp < 0) {
      maxPrintRowCount = Integer.MAX_VALUE;
    } else {
      maxPrintRowCount = Integer.parseInt(maxDisplayNum.trim());
    }
  }

  protected static void printBlockLine(boolean printTimestamp, int colCount,
      ResultSetMetaData resultSetMetaData,
      boolean isShowTs) throws SQLException {
    StringBuilder blockLine = new StringBuilder();
    if (isShowTs) {
      blockLine.append("+");
      for (int i = 1; i <= colCount; i++) {
        blockLine.append(StringUtils.repeat('-', maxValueLengthForShow[i - 1])).append("+");
      }
    } else {
      int tmp = Integer.MIN_VALUE;
      for (int i = 1; i <= colCount; i++) {
        int len = resultSetMetaData.getColumnLabel(i).length();
        tmp = tmp > len ? tmp : len;
      }
      maxValueLength = tmp;
      if (printTimestamp) {
        blockLine.append("+").append(StringUtils.repeat('-', maxTimeLength)).append("+");
      } else {
        blockLine.append("+");
      }
      for (int i = 0; i < colCount - 1; i++) {
        blockLine.append(StringUtils.repeat('-', maxValueLength)).append("+");
      }
    }
    println(blockLine);
  }

  protected static void printName(boolean printTimestamp, int colCount,
      ResultSetMetaData resultSetMetaData,
      boolean isShowTs) throws SQLException {
    print("|");
    if (isShowTs) {
      for (int i = 1; i <= colCount; i++) {
        formatValue = "%" + maxValueLengthForShow[i - 1] + "s|";
        printf(formatValue, resultSetMetaData.getColumnName(i));
      }
    } else {
      formatValue = "%" + maxValueLength + "s|";
      if (printTimestamp) {
        printf(formatTime, TIMESTAMP_STR);
      }
      for (int i = 2; i <= colCount; i++) {
        printf(formatValue, resultSetMetaData.getColumnLabel(i));
      }
    }
    println();
  }

  protected static String[] removePasswordArgs(String[] args) {
    int index = -1;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-" + PASSWORD_ARGS)) {
        index = i;
        break;
      }
    }
    if (index >= 0 && ((index + 1 >= args.length) || (index + 1 < args.length && keywordSet
        .contains(args[index + 1])))) {
      return ArrayUtils.remove(args, index);
    }
    return args;
  }

  protected static void displayLogo(String version) {
    println(" _____       _________  ______   ______    \n"
        + "|_   _|     |  _   _  ||_   _ `.|_   _ \\   \n"
        + "  | |   .--.|_/ | | \\_|  | | `. \\ | |_) |  \n"
        + "  | | / .'`\\ \\  | |      | |  | | |  __'.  \n"
        + " _| |_| \\__. | _| |_    _| |_.' /_| |__) | \n"
        + "|_____|'.__.' |_____|  |______.'|_______/  version " + version + "\n"
        + "                                           \n");
  }

  protected static OperationResult handleInputCmd(String cmd, IoTDBConnection connection) {
    String specialCmd = cmd.toLowerCase().trim();

    if (QUIT_COMMAND.equals(specialCmd) || EXIT_COMMAND.equals(specialCmd)) {
      isQuit = true;
      return OperationResult.STOP_OPER;
    }
    if (HELP.equals(specialCmd)) {
      showHelp();
      return OperationResult.CONTINUE_OPER;
    }
    if (SHOW_METADATA_COMMAND.equals(specialCmd)) {
      showMetaData(connection);
      return OperationResult.CONTINUE_OPER;
    }
    if (specialCmd.startsWith(SET_TIMESTAMP_DISPLAY)) {
      setTimestampDisplay(specialCmd, cmd);
      return OperationResult.CONTINUE_OPER;
    }

    if (specialCmd.startsWith(SET_TIME_ZONE)) {
      setTimeZone(specialCmd, cmd, connection);
      return OperationResult.CONTINUE_OPER;
    }

    if (specialCmd.startsWith(SET_FETCH_SIZE)) {
      setFetchSize(specialCmd, cmd);
      return OperationResult.CONTINUE_OPER;
    }

    if (specialCmd.startsWith(SET_MAX_DISPLAY_NUM)) {
      setMaxDisplaNum(specialCmd, cmd);
      return OperationResult.CONTINUE_OPER;
    }

    if (specialCmd.startsWith(SHOW_TIMEZONE)) {
      showTimeZone(connection);
      return OperationResult.CONTINUE_OPER;
    }
    if (specialCmd.startsWith(SHOW_TIMESTAMP_DISPLAY)) {
      println("Current time format: " + timeFormat);
      return OperationResult.CONTINUE_OPER;
    }
    if (specialCmd.startsWith(SHOW_FETCH_SIZE)) {
      println("Current fetch size: " + fetchSize);
      return OperationResult.CONTINUE_OPER;
    }

    if (specialCmd.startsWith(IMPORT_CMD)) {
      importCmd(specialCmd, cmd, connection);
      return OperationResult.CONTINUE_OPER;
    }

    executeQuery(connection, cmd);
    return OperationResult.NO_OPER;
  }

  protected static void showHelp() {
    println("    <your-sql>\t\t\t execute your sql statment");
    println(String.format("    %s\t\t show how many timeseries are in iotdb",
        SHOW_METADATA_COMMAND));
    println(String.format("    %s=xxx\t eg. long, default, ISO8601, yyyy-MM-dd HH:mm:ss.",
        SET_TIMESTAMP_DISPLAY));
    println(String.format("    %s\t show time display type", SHOW_TIMESTAMP_DISPLAY));
    println(String.format("    %s=xxx\t\t eg. +08:00, Asia/Shanghai.", SET_TIME_ZONE));
    println(String.format("    %s\t\t show cli time zone", SHOW_TIMEZONE));
    println(
        String.format("    %s=xxx\t\t set fetch size when querying data from server.",
            SET_FETCH_SIZE));
    println(String.format("    %s\t\t show fetch size", SHOW_FETCH_SIZE));
    println(
        String.format("    %s=xxx\t eg. set max lines for cli to ouput, -1 equals to unlimited.",
            SET_MAX_DISPLAY_NUM));
  }

  protected static void showMetaData(IoTDBConnection connection) {
    try {
      println(((IoTDBDatabaseMetadata) connection.getMetaData()).getMetadataInJson());
    } catch (SQLException e) {
      println("Failed to show timeseries because: " + e.getMessage());
      handleException(e);
    }
  }

  protected static void setTimestampDisplay(String specialCmd, String cmd) {
    String[] values = specialCmd.split("=");
    if (values.length != 2) {
      println(String.format("Time display format error, please input like %s=ISO8601",
          SET_TIMESTAMP_DISPLAY));
      return;
    }
    try {
      setTimeFormat(cmd.split("=")[1]);
    } catch (Exception e) {
      println(String.format("time display format error, %s", e.getMessage()));
      handleException(e);
      return;
    }
    println("Time display type has set to " + cmd.split("=")[1].trim());
  }

  protected static void setTimeZone(String specialCmd, String cmd, IoTDBConnection connection) {
    String[] values = specialCmd.split("=");
    if (values.length != 2) {
      println(
          String.format("Time zone format error, please input like %s=+08:00", SET_TIME_ZONE));
      return;
    }
    try {
      connection.setTimeZone(cmd.split("=")[1].trim());
    } catch (Exception e) {
      println(String.format("Time zone format error: %s", e.getMessage()));
      handleException(e);
      return;
    }
    println("Time zone has set to " + values[1].trim());
  }

  protected static void setFetchSize(String specialCmd, String cmd) {
    String[] values = specialCmd.split("=");
    if (values.length != 2) {
      println(String
          .format("Fetch size format error, please input like %s=10000", SET_FETCH_SIZE));
      return;
    }
    try {
      setFetchSize(cmd.split("=")[1]);
    } catch (Exception e) {
      println(String.format("Fetch size format error, %s", e.getMessage()));
      handleException(e);
      return;
    }
    println("Fetch size has set to " + values[1].trim());
  }

  protected static void setMaxDisplaNum(String specialCmd, String cmd) {
    String[] values = specialCmd.split("=");
    if (values.length != 2) {
      println(String.format("Max display number format error, please input like %s = 10000",
          SET_MAX_DISPLAY_NUM));
      return;
    }
    try {
      setMaxDisplayNumber(cmd.split("=")[1]);
    } catch (Exception e) {
      println(String.format("Max display number format error, %s", e.getMessage()));
      handleException(e);
      return;
    }
    println("Max display number has set to " + values[1].trim());
  }

  protected static void showTimeZone(IoTDBConnection connection) {
    try {
      println("Current time zone: " + connection.getTimeZone());
    } catch (Exception e) {
      println("Cannot get time zone from server side because: " + e.getMessage());
      handleException(e);
    }
  }

  protected static void importCmd(String specialCmd, String cmd, IoTDBConnection connection) {
    String[] values = specialCmd.split(" ");
    if (values.length != 2) {
      println("Please input like: import /User/myfile. "
          + "Noted that your file path cannot contain any space character)");
      return;
    }
    try {
      println(cmd.split(" ")[1]);
      ImportCsv.importCsvFromFile(host, port, username, password, cmd.split(" ")[1],
          connection.getTimeZone());
    } catch (SQLException e) {
      println(String.format("Failed to import from %s because %s",
          cmd.split(" ")[1], e.getMessage()));
      handleException(e);
    } catch (TException e) {
      println("Cannot connect to server");
      handleException(e);
    }
  }

  protected static void executeQuery(IoTDBConnection connection, String cmd) {
    Statement statement = null;
    long startTime = System.currentTimeMillis();
    try {
      ZoneId zoneId = ZoneId.of(connection.getTimeZone());
      statement = connection.createStatement();
      statement.setFetchSize(fetchSize);
      boolean hasResultSet = statement.execute(cmd.trim());
      if (hasResultSet) {
        ResultSet resultSet = statement.getResultSet();
        output(resultSet, printToConsole, zoneId);
        if (resultSet != null) {
          resultSet.close();
        }
      }
    } catch (Exception e) {
      println("Msg: " + e.getMessage());
      handleException(e);
    } finally {
      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          println("Cannot close statement because: " + e.getMessage());
          handleException(e);
        }
      }
    }
    long costTime = System.currentTimeMillis() - startTime;
    println(String.format("It costs %.3fs", costTime / 1000.0));
  }

  enum OperationResult {
    STOP_OPER, CONTINUE_OPER, NO_OPER
  }
  
  protected static void printf(String format, Object ... args) {
    SCREEN_PRINTER.printf(format, args);
  }
  
  protected static void print(String msg) {
    SCREEN_PRINTER.println(msg);
  }

  protected static void println() {
    SCREEN_PRINTER.println();
  }

  protected static void println(String msg) {
    SCREEN_PRINTER.println(msg);
  }

  protected static void println(Object obj) {
    SCREEN_PRINTER.println(obj);
  }

  protected static void handleException(Exception e) {
    if (showException) {
      e.printStackTrace(SCREEN_PRINTER);
    }
  }
}
