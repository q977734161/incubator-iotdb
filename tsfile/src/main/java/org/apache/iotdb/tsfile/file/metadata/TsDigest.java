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

package org.apache.iotdb.tsfile.file.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.iotdb.tsfile.utils.ReadWriteIOUtils;

/**
 * Digest/statistics per chunk group and per page.
 */
public class TsDigest {

  private Map<String, ByteBuffer> statistics;

  private int serializedSize = Integer.BYTES;

  private int sizeOfList;

  public TsDigest() {
    // allowed to clair an empty TsDigest whose fields will be assigned later.
  }

  public static int getNullDigestSize() {
    return Integer.BYTES;
  }

  public static int serializeNullTo(OutputStream outputStream) throws IOException {
    return ReadWriteIOUtils.write(0, outputStream);
  }

  public static int serializeNullTo(ByteBuffer buffer) {
    return ReadWriteIOUtils.write(0, buffer);
  }

  /**
   * use given input stream to deserialize.
   *
   * @param inputStream -given input stream
   * @return -an instance of TsDigest
   */
  public static TsDigest deserializeFrom(InputStream inputStream) throws IOException {
    TsDigest digest = new TsDigest();

    int size = ReadWriteIOUtils.readInt(inputStream);
    if (size > 0) {
      Map<String, ByteBuffer> statistics = new HashMap<>();
      String key;
      ByteBuffer value;
      for (int i = 0; i < size; i++) {
        key = ReadWriteIOUtils.readString(inputStream);
        value = ReadWriteIOUtils.readByteBufferWithSelfDescriptionLength(inputStream);
        statistics.put(key, value);
      }
      digest.statistics = statistics;
    }

    return digest;
  }

  /**
   * use given buffer to deserialize.
   *
   * @param buffer -given buffer
   * @return -an instance of TsDigest
   */
  public static TsDigest deserializeFrom(ByteBuffer buffer) {
    TsDigest digest = new TsDigest();

    int size = ReadWriteIOUtils.readInt(buffer);
    if (size > 0) {
      Map<String, ByteBuffer> statistics = new HashMap<>();
      String key;
      ByteBuffer value;
      for (int i = 0; i < size; i++) {
        key = ReadWriteIOUtils.readString(buffer);
        value = ReadWriteIOUtils.readByteBufferWithSelfDescriptionLength(buffer);
        statistics.put(key, value);
      }
      digest.statistics = statistics;
    }

    return digest;
  }

  private void reCalculateSerializedSize() {
    serializedSize = Integer.BYTES;
    if (statistics != null) {
      for (Map.Entry<String, ByteBuffer> entry : statistics.entrySet()) {
        serializedSize += Integer.BYTES + entry.getKey().length() + Integer.BYTES
            + entry.getValue().remaining();
      }
      sizeOfList = statistics.size();
    } else {
      sizeOfList = 0;
    }
  }

  /**
   * get statistics of the current object.
   *
   * @return -unmodifiableMap of the current object's statistics
   */
  public Map<String, ByteBuffer> getStatistics() {
    if (statistics == null) {
      return null;
    }
    return Collections.unmodifiableMap(this.statistics);
  }

  public void setStatistics(Map<String, ByteBuffer> statistics) {
    this.statistics = statistics;
    reCalculateSerializedSize();
  }

  /**
   * add statistics using given param.
   *
   * @param key -key of the entry
   * @param value -value of the entry
   */
  public void addStatistics(String key, ByteBuffer value) {
    if (statistics == null) {
      statistics = new HashMap<>();
    }
    statistics.put(key, value);
    serializedSize += Integer.BYTES + key.length() + Integer.BYTES + value.remaining();
    sizeOfList++;
  }

  @Override
  public String toString() {
    return statistics != null ? statistics.toString() : "";
  }

  /**
   * use given outputStream to serialize.
   *
   * @param outputStream -given outputStream
   * @return -byte length
   */
  public int serializeTo(OutputStream outputStream) throws IOException {
    if ((statistics != null && sizeOfList != statistics.size()) || (statistics == null
        && sizeOfList != 0)) {
      reCalculateSerializedSize();
    }
    int byteLen = 0;
    if (statistics == null || statistics.size() == 0) {
      byteLen += ReadWriteIOUtils.write(0, outputStream);
    } else {
      byteLen += ReadWriteIOUtils.write(statistics.size(), outputStream);
      for (Map.Entry<String, ByteBuffer> entry : statistics.entrySet()) {
        byteLen += ReadWriteIOUtils
            .write(entry.getKey(), outputStream);
        byteLen += ReadWriteIOUtils
            .write(entry.getValue(), outputStream);
      }
    }
    return byteLen;
  }

  /**
   * use given buffer to serialize.
   *
   * @param buffer -given buffer
   * @return -byte length
   */
  public int serializeTo(ByteBuffer buffer) {
    if ((statistics != null && sizeOfList != statistics.size()) || (statistics == null
        && sizeOfList != 0)) {
      reCalculateSerializedSize();
    }
    int byteLen = 0;

    if (statistics == null || statistics.size() == 0) {
      byteLen += ReadWriteIOUtils.write(0, buffer);
    } else {
      byteLen += ReadWriteIOUtils.write(statistics.size(), buffer);
      for (Map.Entry<String, ByteBuffer> entry : statistics.entrySet()) {
        byteLen += ReadWriteIOUtils.write(entry.getKey(), buffer);
        byteLen += ReadWriteIOUtils
            .write(entry.getValue(), buffer);
      }
    }
    return byteLen;
  }

  /**
   * get the serializedSize of the current object.
   *
   * @return -serializedSize
   */
  public int getSerializedSize() {
    if (statistics == null || (sizeOfList != statistics.size())) {
      reCalculateSerializedSize();
    }
    return serializedSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TsDigest digest = (TsDigest) o;
    if (serializedSize != digest.serializedSize || sizeOfList != digest.sizeOfList
        || statistics.size() != digest.statistics.size()) {
      return false;
    }
    for (Entry<String, ByteBuffer> entry : statistics.entrySet()) {
      String key = entry.getKey();
      ByteBuffer value = entry.getValue();
      if (!digest.statistics.containsKey(key) || !value.equals(digest.statistics.get(key))) {
        return false;
      }
    }
    return true;
  }
}
