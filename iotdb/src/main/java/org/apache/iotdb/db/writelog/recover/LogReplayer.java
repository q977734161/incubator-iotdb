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

package org.apache.iotdb.db.writelog.recover;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.iotdb.db.engine.storagegroup.TsFileResource;
import org.apache.iotdb.db.engine.memtable.IMemTable;
import org.apache.iotdb.db.engine.modification.Deletion;
import org.apache.iotdb.db.engine.modification.ModificationFile;
import org.apache.iotdb.db.engine.version.VersionController;
import org.apache.iotdb.db.exception.ProcessorException;
import org.apache.iotdb.db.qp.physical.PhysicalPlan;
import org.apache.iotdb.db.qp.physical.crud.DeletePlan;
import org.apache.iotdb.db.qp.physical.crud.InsertPlan;
import org.apache.iotdb.db.qp.physical.crud.UpdatePlan;
import org.apache.iotdb.db.writelog.io.ILogReader;
import org.apache.iotdb.db.writelog.manager.MultiFileLogNodeManager;
import org.apache.iotdb.db.writelog.node.WriteLogNode;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.Path;
import org.apache.iotdb.tsfile.write.schema.FileSchema;

/**
 * LogReplayer finds the logNode of the TsFile given by insertFilePath and logNodePrefix, reads
 * the WALs from the logNode and redoes them into a given MemTable and ModificationFile.
 */
public class LogReplayer {

  private String logNodePrefix;
  private String insertFilePath;
  private ModificationFile modFile;
  private VersionController versionController;
  private TsFileResource currentTsFileResource;
  // fileSchema is used to get the measurement data type
  private FileSchema fileSchema;
  private IMemTable recoverMemTable;

  // unsequence file tolerates duplicated data
  private boolean acceptDuplication;

  private Map<String, Long> tempStartTimeMap = new HashMap<>();
  private Map<String, Long> tempEndTimeMap = new HashMap<>();

  public LogReplayer(String logNodePrefix, String insertFilePath,
      ModificationFile modFile,
      VersionController versionController,
      TsFileResource currentTsFileResource,
      FileSchema fileSchema, IMemTable memTable, boolean acceptDuplication) {
    this.logNodePrefix = logNodePrefix;
    this.insertFilePath = insertFilePath;
    this.modFile = modFile;
    this.versionController = versionController;
    this.currentTsFileResource = currentTsFileResource;
    this.fileSchema = fileSchema;
    this.recoverMemTable = memTable;
    this.acceptDuplication = acceptDuplication;
  }

  /**
   * finds the logNode of the TsFile given by insertFilePath and logNodePrefix, reads
   * the WALs from the logNode and redoes them into a given MemTable and ModificationFile.
   * @throws ProcessorException
   */
  public void replayLogs() throws ProcessorException {
    WriteLogNode logNode = MultiFileLogNodeManager.getInstance().getNode(
        logNodePrefix + new File(insertFilePath).getName());

    ILogReader logReader = logNode.getLogReader();
    try {
      while (logReader.hasNext()) {
        PhysicalPlan plan = logReader.next();
        if (plan instanceof InsertPlan) {
          replayInsert((InsertPlan) plan);
        } else if (plan instanceof DeletePlan) {
          replayDelete((DeletePlan) plan);
        } else if (plan instanceof UpdatePlan) {
          replayUpdate((UpdatePlan) plan);
        }
      }
    } catch (IOException e) {
      throw new ProcessorException("Cannot replay logs", e);
    } finally {
      logReader.close();
    }
    tempStartTimeMap.forEach((k, v) -> currentTsFileResource.updateTime(k, v));
    tempEndTimeMap.forEach((k, v) -> currentTsFileResource.updateTime(k, v));
  }

  private void replayDelete(DeletePlan deletePlan) throws IOException {
    List<Path> paths = deletePlan.getPaths();
    for (Path path : paths) {
      recoverMemTable.delete(path.getDevice(), path.getMeasurement(), deletePlan.getDeleteTime());
      modFile.write(new Deletion(path, versionController.nextVersion(),deletePlan.getDeleteTime()));
    }
  }

  private void replayInsert(InsertPlan insertPlan) {
    if (currentTsFileResource != null) {
      // the last chunk group may contain the same data with the logs, ignore such logs in seq file
      Long lastEndTime = currentTsFileResource.getEndTimeMap().get(insertPlan.getDeviceId());
      if ( lastEndTime != null && lastEndTime >= insertPlan.getTime() &&
          !acceptDuplication) {
        return;
      }
      tempStartTimeMap.putIfAbsent(insertPlan.getDeviceId(), insertPlan.getTime());
      Long endTime = tempEndTimeMap.get(insertPlan.getDeviceId());
      if (endTime == null || endTime < insertPlan.getTime()) {
        tempEndTimeMap.put(insertPlan.getDeviceId(), insertPlan.getTime());
      }
    }
    String[] measurementList = insertPlan.getMeasurements();
    TSDataType[] dataTypes = new TSDataType[measurementList.length];
    for (int i = 0; i < measurementList.length; i++) {
      dataTypes[i] = fileSchema.getMeasurementDataType(measurementList[i]);
    }
    insertPlan.setDataTypes(dataTypes);
    recoverMemTable.insert(insertPlan);
  }

  @SuppressWarnings("unused")
  private void replayUpdate(UpdatePlan updatePlan) {
    // TODO: support update
    throw new UnsupportedOperationException("Update not supported");
  }
}
