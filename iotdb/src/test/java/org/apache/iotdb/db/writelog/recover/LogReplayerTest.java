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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import org.apache.iotdb.db.engine.storagegroup.TsFileResource;
import org.apache.iotdb.db.engine.memtable.IMemTable;
import org.apache.iotdb.db.engine.memtable.PrimitiveMemTable;
import org.apache.iotdb.db.engine.modification.Deletion;
import org.apache.iotdb.db.engine.modification.Modification;
import org.apache.iotdb.db.engine.modification.ModificationFile;
import org.apache.iotdb.db.engine.querycontext.ReadOnlyMemChunk;
import org.apache.iotdb.db.engine.version.VersionController;
import org.apache.iotdb.db.exception.ProcessorException;
import org.apache.iotdb.db.qp.physical.crud.DeletePlan;
import org.apache.iotdb.db.qp.physical.crud.InsertPlan;
import org.apache.iotdb.db.utils.TimeValuePair;
import org.apache.iotdb.db.writelog.manager.MultiFileLogNodeManager;
import org.apache.iotdb.db.writelog.node.WriteLogNode;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSEncoding;
import org.apache.iotdb.tsfile.read.common.Path;
import org.apache.iotdb.tsfile.write.schema.FileSchema;
import org.apache.iotdb.tsfile.write.schema.MeasurementSchema;
import org.junit.Test;

public class LogReplayerTest {

  @Test
  public void test() throws IOException, ProcessorException {
    String logNodePrefix = "testLogNode";
    File tsFile = new File("temp", "test.ts");
    File modF = new File("test.mod");
    ModificationFile modFile = new ModificationFile(modF.getPath());
    VersionController versionController = new VersionController() {
      @Override
      public long nextVersion() {
        return 5;
      }

      @Override
      public long currVersion() {
        return 5;
      }
    };
    TsFileResource tsFileResource = new TsFileResource(tsFile);
    IMemTable memTable = new PrimitiveMemTable();
    FileSchema schema = new FileSchema();

    try {
      for (int i = 0; i < 5; i++) {
        schema.registerMeasurement(new MeasurementSchema("sensor" + i, TSDataType.INT64, TSEncoding.PLAIN));
      }

      LogReplayer replayer = new LogReplayer(logNodePrefix, tsFile.getPath(), modFile,
          versionController, tsFileResource, schema, memTable, true);

      WriteLogNode node =
          MultiFileLogNodeManager.getInstance().getNode(logNodePrefix + tsFile.getName());
      for (int i = 0; i < 5; i++) {
        node.write(new InsertPlan("device" + i, i, "sensor" + i, String.valueOf(i)));
      }
      DeletePlan deletePlan = new DeletePlan(3, new Path("device0", "sensor0"));
      node.write(deletePlan);
      node.close();

      replayer.replayLogs();

      for (int i = 0; i < 5; i++) {
        ReadOnlyMemChunk chunk = memTable.query("device" + i, "sensor" + i, TSDataType.INT64,
            Collections.emptyMap());
        Iterator<TimeValuePair> iterator = chunk.getIterator();
        if (i == 0) {
          assertFalse(iterator.hasNext());
        } else {
          assertTrue(iterator.hasNext());
          TimeValuePair timeValuePair = iterator.next();
          assertEquals(i, timeValuePair.getTimestamp());
          assertEquals(i, timeValuePair.getValue().getLong());
          assertFalse(iterator.hasNext());
        }
      }

      Modification[] mods = modFile.getModifications().toArray(new Modification[0]);
      assertEquals(1, mods.length);
      assertEquals(new Deletion(new Path("device0", "sensor0"), 5, 3), mods[0]);

      for (int i = 0; i < 5; i++) {
        assertEquals(i, (long)tsFileResource.getStartTimeMap().get("device" + i));
        assertEquals(i, (long)tsFileResource.getEndTimeMap().get("device" + i));
      }
    } finally {
      modFile.close();
      MultiFileLogNodeManager.getInstance().deleteNode(logNodePrefix + tsFile.getName());
      modF.delete();
      tsFile.delete();
      tsFile.getParentFile().delete();
    }
  }
}
