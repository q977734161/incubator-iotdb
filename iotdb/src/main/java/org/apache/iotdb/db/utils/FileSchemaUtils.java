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
package org.apache.iotdb.db.utils;

import java.util.List;
import org.apache.iotdb.db.metadata.MManager;
import org.apache.iotdb.tsfile.exception.write.WriteProcessException;
import org.apache.iotdb.tsfile.write.schema.FileSchema;
import org.apache.iotdb.tsfile.write.schema.MeasurementSchema;


public class FileSchemaUtils {

  private FileSchemaUtils(){}

  /**
   * Construct the FileSchema of the FileNode named processorName.
   * @param processorName the name of a FileNode.
   * @return the schema of the FileNode named processorName.
   * @throws WriteProcessException when the fileSchema cannot be created.
   */
  public static FileSchema constructFileSchema(String processorName) {
    List<MeasurementSchema> columnSchemaList;
    columnSchemaList = MManager.getInstance().getSchemaForStorageGroup(processorName);
    return getFileSchemaFromColumnSchema(columnSchemaList);
  }

  /**
   * getFileSchemaFromColumnSchema construct a FileSchema using the schema of the columns and the
   * device type.
   * @param schemaList the schema of the columns in this file.
   * @return a FileSchema contains the provided schemas.
   */
  public static FileSchema getFileSchemaFromColumnSchema(List<MeasurementSchema> schemaList) {
    FileSchema schema = new FileSchema();
    for (MeasurementSchema measurementSchema : schemaList) {
      schema.registerMeasurement(measurementSchema);
    }
    return schema;
  }
}
