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
package org.apache.iotdb.db.query.reader.chunkRelated;

import java.io.IOException;
import org.apache.iotdb.db.query.reader.IPointReader;
import org.apache.iotdb.db.utils.TimeValuePair;
import org.apache.iotdb.db.utils.TimeValuePairUtils;
import org.apache.iotdb.tsfile.read.common.BatchData;
import org.apache.iotdb.tsfile.read.reader.chunk.ChunkReader;

/**
 * To read chunk data on disk, this class implements an interface {@link IPointReader} based on the
 * data reader {@link ChunkReader}.
 * <p>
 * Note that <code>ChunkReader</code> is an abstract class with three concrete classes, two of which
 * are used here: <code>ChunkReaderWithoutFilter</code> and <code>ChunkReaderWithFilter</code>.
 * <p>
 * This class is used in {@link org.apache.iotdb.db.query.reader.resourceRelated.UnseqResourceMergeReader}.
 */
public class DiskChunkReader implements IPointReader {

  private ChunkReader chunkReader;
  private BatchData data;

  public DiskChunkReader(ChunkReader chunkReader) {
    this.chunkReader = chunkReader;
  }

  @Override
  public boolean hasNext() throws IOException {
    if (data != null && data.hasNext()) {
      return true;
    }
    while (chunkReader.hasNextBatch()) {
      data = chunkReader.nextBatch();
      if (data.hasNext()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public TimeValuePair next() {
    TimeValuePair timeValuePair = TimeValuePairUtils.getCurrentTimeValuePair(data);
    data.next();
    return timeValuePair;
  }

  @Override
  public TimeValuePair current() {
    return TimeValuePairUtils.getCurrentTimeValuePair(data);
  }

  @Override
  public void close() {
    this.chunkReader.close();
  }
}
