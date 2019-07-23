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
package org.apache.iotdb.tsfile.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Scanner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.iotdb.tsfile.common.conf.TSFileConfig;
import org.apache.iotdb.tsfile.common.conf.TSFileDescriptor;
import org.apache.iotdb.tsfile.common.constant.JsonFormatConstant;
import org.apache.iotdb.tsfile.encoding.encoder.Encoder;
import org.apache.iotdb.tsfile.exception.write.WriteProcessException;
import org.apache.iotdb.tsfile.file.metadata.enums.CompressionType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSEncoding;
import org.apache.iotdb.tsfile.write.TsFileWriter;
import org.apache.iotdb.tsfile.write.record.TSRecord;
import org.apache.iotdb.tsfile.write.schema.FileSchema;
import org.apache.iotdb.tsfile.write.schema.SchemaBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore
public class TsFileGeneratorForTest {

  public static final long START_TIMESTAMP = 1480562618000L;
  private static final Logger LOG = LoggerFactory.getLogger(TsFileGeneratorForTest.class);
  public static TsFileWriter innerWriter;
  public static String inputDataFile;
  public static String outputDataFile = "target/testTsFile.tsfile";
  public static String errorOutputDataFile;
  private static int rowCount;
  private static int chunkGroupSize;
  private static int pageSize;

  public static void generateFile(int rowCount, int chunkGroupSize, int pageSize)
      throws IOException, InterruptedException, WriteProcessException {
    generateFile(rowCount, rowCount, chunkGroupSize, pageSize);
  }

  public static void generateFile(int minRowCount, int maxRowCount,int chunkGroupSize, int pageSize)
      throws IOException, InterruptedException, WriteProcessException {
    TsFileGeneratorForTest.rowCount = maxRowCount;
    TsFileGeneratorForTest.chunkGroupSize = chunkGroupSize;
    TsFileGeneratorForTest.pageSize = pageSize;
    prepare(minRowCount, maxRowCount);
    write();
  }

  public static void prepare(int minrowCount, int maxRowCount) throws IOException {
    inputDataFile = "target/perTestInputData";
    errorOutputDataFile = "target/perTestErrorOutputData.tsfile";
    generateSampleInputDataFile(minrowCount, maxRowCount);
  }

  public static void after() {
    File file = new File(inputDataFile);
    if (file.exists()) {
      Assert.assertTrue(file.delete());
    }
    file = new File(outputDataFile);
    if (file.exists()) {
      Assert.assertTrue(file.delete());
    }
    file = new File(errorOutputDataFile);
    if (file.exists()) {
      Assert.assertTrue(file.delete());
    }
  }

  static private void generateSampleInputDataFile(int minRowCount, int maxRowCount) throws IOException {
    File file = new File(inputDataFile);
    if (file.exists()) {
      Assert.assertTrue(file.delete());
    }
    file.getParentFile().mkdirs();
    FileWriter fw = new FileWriter(file);

    long startTime = START_TIMESTAMP;
    for (int i = 0; i < maxRowCount; i++) {
      // write d1
      String d1 = "d1," + (startTime + i) + ",s1," + (i * 10 + 1) + ",s2," + (i * 10 + 2);
      if (i % 5 == 0) {
        d1 += ",s3," + (i * 10 + 3);
      }
      if (i % 8 == 0) {
        d1 += ",s4," + "dog" + i;
      }
      if (i % 9 == 0) {
        d1 += ",s5," + "false";
      }
      if (i % 10 == 0 && i < minRowCount) {
        d1 += ",s6," + ((int) (i / 9.0) * 100) / 100.0;
      }
      if (i % 11 == 0) {
        d1 += ",s7," + ((int) (i / 10.0) * 100) / 100.0;
      }
      fw.write(d1 + "\r\n");

      // write d2
      String d2 = "d2," + (startTime + i) + ",s2," + (i * 10 + 2) + ",s3," + (i * 10 + 3);
      if (i % 20 < 5) {
        // LOG.info("write null to d2:" + (startTime + i));
        d2 = "d2," + (startTime + i) + ",s2,,s3," + (i * 10 + 3);
      }
      if (i % 5 == 0) {
        d2 += ",s1," + (i * 10 + 1);
      }
      if (i % 8 == 0) {
        d2 += ",s4," + "dog" + i % 4;
      }
      fw.write(d2 + "\r\n");
    }
    // write error
    String d =
        "d2,3," + (startTime + rowCount) + ",s2," + (rowCount * 10 + 2) + ",s3," + (rowCount * 10
            + 3);
    fw.write(d + "\r\n");
    d = "d2," + (startTime + rowCount + 1) + ",2,s-1," + (rowCount * 10 + 2);
    fw.write(d + "\r\n");
    fw.close();
  }

  static public void write() throws IOException {
    File file = new File(outputDataFile);
    File errorFile = new File(errorOutputDataFile);
    if (file.exists()) {
      Assert.assertTrue(file.delete());
    }
    if (errorFile.exists()) {
      Assert.assertTrue(errorFile.delete());
    }

    FileSchema schema = generateTestSchema();

    TSFileDescriptor.getInstance().getConfig().groupSizeInByte = chunkGroupSize;
    TSFileDescriptor.getInstance().getConfig().maxNumberOfPointsInPage = pageSize;
    innerWriter = new TsFileWriter(file, schema, TSFileDescriptor.getInstance().getConfig());

    // write
    try (Scanner in = new Scanner(new File(inputDataFile))) {
      assert in != null;
      while (in.hasNextLine()) {
        String str = in.nextLine();
        TSRecord record = RecordUtils.parseSimpleTupleRecord(str, schema);
        innerWriter.write(record);
      }
    } catch (WriteProcessException e) {
      e.printStackTrace();
    } finally {
      innerWriter.close();
    }
  }

  private static JSONObject generateTestData() {
    TSFileConfig conf = TSFileDescriptor.getInstance().getConfig();
    JSONObject s1 = new JSONObject();
    s1.put(JsonFormatConstant.MEASUREMENT_UID, "s1");
    s1.put(JsonFormatConstant.DATA_TYPE, TSDataType.INT32.toString());
    s1.put(JsonFormatConstant.MEASUREMENT_ENCODING, conf.valueEncoder);
    JSONObject s2 = new JSONObject();
    s2.put(JsonFormatConstant.MEASUREMENT_UID, "s2");
    s2.put(JsonFormatConstant.DATA_TYPE, TSDataType.INT64.toString());
    s2.put(JsonFormatConstant.MEASUREMENT_ENCODING, conf.valueEncoder);
    JSONObject s3 = new JSONObject();
    s3.put(JsonFormatConstant.MEASUREMENT_UID, "s3");
    s3.put(JsonFormatConstant.DATA_TYPE, TSDataType.INT64.toString());
    s3.put(JsonFormatConstant.MEASUREMENT_ENCODING, conf.valueEncoder);
    JSONObject s4 = new JSONObject();
    s4.put(JsonFormatConstant.MEASUREMENT_UID, "s4");
    s4.put(JsonFormatConstant.DATA_TYPE, TSDataType.TEXT.toString());
    s4.put(JsonFormatConstant.MEASUREMENT_ENCODING, TSEncoding.PLAIN.toString());
    JSONObject s5 = new JSONObject();
    s5.put(JsonFormatConstant.MEASUREMENT_UID, "s5");
    s5.put(JsonFormatConstant.DATA_TYPE, TSDataType.BOOLEAN.toString());
    s5.put(JsonFormatConstant.MEASUREMENT_ENCODING, TSEncoding.PLAIN.toString());
    JSONObject s6 = new JSONObject();
    s6.put(JsonFormatConstant.MEASUREMENT_UID, "s6");
    s6.put(JsonFormatConstant.DATA_TYPE, TSDataType.FLOAT.toString());
    s6.put(JsonFormatConstant.MEASUREMENT_ENCODING, TSEncoding.RLE.toString());
    JSONObject s7 = new JSONObject();
    s7.put(JsonFormatConstant.MEASUREMENT_UID, "s7");
    s7.put(JsonFormatConstant.DATA_TYPE, TSDataType.DOUBLE.toString());
    s7.put(JsonFormatConstant.MEASUREMENT_ENCODING, TSEncoding.RLE.toString());

    JSONArray measureGroup1 = new JSONArray();
    measureGroup1.add(s1);
    measureGroup1.add(s2);
    measureGroup1.add(s3);
    measureGroup1.add(s4);
    measureGroup1.add(s5);
    measureGroup1.add(s6);
    measureGroup1.add(s7);

    JSONObject jsonSchema = new JSONObject();
    jsonSchema.put(JsonFormatConstant.DELTA_TYPE, "test_type");
    jsonSchema.put(JsonFormatConstant.JSON_SCHEMA, measureGroup1);
    return jsonSchema;
  }

  private static FileSchema generateTestSchema() {
    SchemaBuilder schemaBuilder = new SchemaBuilder();
    schemaBuilder.addSeries("s1", TSDataType.INT32, TSEncoding.RLE);
    schemaBuilder.addSeries("s2", TSDataType.INT64, TSEncoding.PLAIN);
    schemaBuilder.addSeries("s3", TSDataType.INT64, TSEncoding.TS_2DIFF);
    schemaBuilder.addSeries("s4", TSDataType.TEXT, TSEncoding.PLAIN, CompressionType.UNCOMPRESSED,
        Collections.singletonMap(Encoder.MAX_STRING_LENGTH, "20"));
    schemaBuilder.addSeries("s5", TSDataType.BOOLEAN, TSEncoding.RLE);
    schemaBuilder.addSeries("s6", TSDataType.FLOAT, TSEncoding.RLE, CompressionType.SNAPPY,
        Collections.singletonMap(Encoder.MAX_POINT_NUMBER, "5"));
    schemaBuilder.addSeries("s7", TSDataType.DOUBLE, TSEncoding.GORILLA);
    return schemaBuilder.build();
  }

}
