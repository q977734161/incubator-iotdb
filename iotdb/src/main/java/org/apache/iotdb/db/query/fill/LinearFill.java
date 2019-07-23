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

package org.apache.iotdb.db.query.fill;

import java.io.IOException;
import org.apache.iotdb.db.exception.StorageEngineException;
import org.apache.iotdb.db.exception.UnSupportedFillTypeException;
import org.apache.iotdb.db.query.context.QueryContext;
import org.apache.iotdb.db.query.reader.IPointReader;
import org.apache.iotdb.db.utils.TimeValuePair;
import org.apache.iotdb.db.utils.TsPrimitiveType;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.Path;

public class LinearFill extends IFill {

  private long beforeRange;
  private long afterRange;

  public LinearFill(long beforeRange, long afterRange) {
    this.beforeRange = beforeRange;
    this.afterRange = afterRange;
  }

  /**
   * Constructor of LinearFill.
   */
  public LinearFill(TSDataType dataType, long queryTime, long beforeRange,
      long afterRange) {
    super(dataType, queryTime);
    this.beforeRange = beforeRange;
    this.afterRange = afterRange;
  }

  public long getBeforeRange() {
    return beforeRange;
  }

  public void setBeforeRange(long beforeRange) {
    this.beforeRange = beforeRange;
  }

  public long getAfterRange() {
    return afterRange;
  }

  public void setAfterRange(long afterRange) {
    this.afterRange = afterRange;
  }

  @Override
  public IFill copy(Path path) {
    return new LinearFill(dataType, queryTime, beforeRange, afterRange);
  }

  @Override
  public void constructReaders(Path path, QueryContext context)
      throws IOException, StorageEngineException {
    super.constructReaders(path, context, beforeRange);
  }

  @Override
  public IPointReader getFillResult() throws IOException {
    TimeValuePair beforePair = null;
    TimeValuePair afterPair = null;
    while (allDataReader.hasNext()) {
      afterPair = allDataReader.next();
      if (afterPair.getTimestamp() <= queryTime) {
        beforePair = afterPair;
      } else {
        break;
      }
    }

    if (beforePair == null || beforePair.getTimestamp() == queryTime) {
      return new TimeValuePairPointReader(beforePair);
    }

    // if afterRange equals -1, this means that there is no time-bound filling.
    if (afterRange == -1) {
      return new TimeValuePairPointReader(average(beforePair, afterPair));
    }

    if (afterPair.getTimestamp() > queryTime + afterRange || afterPair.getTimestamp() < queryTime) {
      return new TimeValuePairPointReader(new TimeValuePair(queryTime, null));
    }
    return new TimeValuePairPointReader(average(beforePair, afterPair));
  }

  // returns the average of two points
  private TimeValuePair average(TimeValuePair beforePair, TimeValuePair afterPair) {
    double totalTimeLength = (double) afterPair.getTimestamp() - beforePair.getTimestamp();
    double beforeTimeLength = (double) (queryTime - beforePair.getTimestamp());
    switch (dataType) {
      case INT32:
        int startIntValue = beforePair.getValue().getInt();
        int endIntValue = afterPair.getValue().getInt();
        int fillIntValue =
            startIntValue + (int) ((double) (endIntValue - startIntValue) / totalTimeLength
                * beforeTimeLength);
        beforePair.setValue(TsPrimitiveType.getByType(TSDataType.INT32, fillIntValue));
        break;
      case INT64:
        long startLongValue = beforePair.getValue().getLong();
        long endLongValue = afterPair.getValue().getLong();
        long fillLongValue =
            startLongValue + (long) ((double) (endLongValue - startLongValue) / totalTimeLength
                * beforeTimeLength);
        beforePair.setValue(TsPrimitiveType.getByType(TSDataType.INT64, fillLongValue));
        break;
      case FLOAT:
        float startFloatValue = beforePair.getValue().getFloat();
        float endFloatValue = afterPair.getValue().getFloat();
        float fillFloatValue =
            startFloatValue + (float) ((endFloatValue - startFloatValue) / totalTimeLength
                * beforeTimeLength);
        beforePair.setValue(TsPrimitiveType.getByType(TSDataType.FLOAT, fillFloatValue));
        break;
      case DOUBLE:
        double startDoubleValue = beforePair.getValue().getDouble();
        double endDoubleValue = afterPair.getValue().getDouble();
        double fillDoubleValue =
            startDoubleValue + ((endDoubleValue - startDoubleValue) / totalTimeLength
                * beforeTimeLength);
        beforePair.setValue(TsPrimitiveType.getByType(TSDataType.DOUBLE, fillDoubleValue));
        break;
      default:
        throw new UnSupportedFillTypeException("Unsupported linear fill data type : " + dataType);

    }
    beforePair.setTimestamp(queryTime);
    return beforePair;
  }
}