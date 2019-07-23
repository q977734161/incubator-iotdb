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

package org.apache.iotdb.db.engine.version;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleFileVersionController uses a local file and its file name to store the version.
 */
public class SimpleFileVersionController implements VersionController {
  private static final Logger logger = LoggerFactory.getLogger(SimpleFileVersionController.class);

  /**
   * Every time currVersion - prevVersion >= saveInterval, currVersion is persisted and prevVersion
   * is set to currVersion. When recovering from file, the version number is automatically increased
   * by saveInterval to avoid conflicts.
   */
  private static long saveInterval = 100;
  private static final String FILE_PREFIX = "Version-";
  private long prevVersion;
  private long currVersion;
  private String directoryPath;

  public SimpleFileVersionController(String directoryPath) throws IOException {
    this.directoryPath = directoryPath;
    restore();
  }

  @Override
  public synchronized long nextVersion() {
    currVersion ++;
    try {
      checkPersist();
    } catch (IOException e) {
      logger.error("Error occurred when getting next version.", e);
    }
    return currVersion;
  }

  /**
   * Test only method, no need for concurrency.
   * @return the current version.
   */
  @Override
  public long currVersion() {
    return currVersion;
  }

  private void checkPersist() throws IOException {
    if ((currVersion - prevVersion) >= saveInterval) {
      persist();
    }
  }

  private void persist() throws IOException {
    File oldFile = new File(directoryPath, FILE_PREFIX + prevVersion);
    File newFile = new File(directoryPath, FILE_PREFIX + currVersion);
    FileUtils.moveFile(oldFile, newFile);
    logger.info("Version file updated, previous: {}, current: {}",
        oldFile.getAbsolutePath(), newFile.getAbsolutePath());
    prevVersion = currVersion;
  }

  private void restore() throws IOException {
    File directory = new File(directoryPath);
    File[] versionFiles = directory.listFiles((dir, name) -> name.startsWith(FILE_PREFIX));
    File versionFile;
    if (versionFiles != null && versionFiles.length > 0) {
      long maxVersion = 0;
      int maxVersionIndex = 0;
      for (int i = 0; i < versionFiles.length; i ++) {
        // extract version from "Version-123456"
        long fileVersion = Long.parseLong(versionFiles[i].getName().split("-")[1]);
        if (fileVersion > maxVersion) {
          maxVersion = fileVersion;
          maxVersionIndex = i;
        }
      }
      prevVersion = maxVersion;
      for(int i = 0; i < versionFiles.length; i ++) {
        if (i != maxVersionIndex) {
          versionFiles[i].delete();
        }
      }
    } else {
      versionFile = new File(directory, FILE_PREFIX + "0");
      prevVersion = 0;
      new FileOutputStream(versionFile).close();
    }
    // prevent overlapping in case of failure
    currVersion = prevVersion + saveInterval;
    persist();
  }

  // test only method
  public static void setSaveInterval(long saveInterval) {
    SimpleFileVersionController.saveInterval = saveInterval;
  }

  public static long getSaveInterval() {
    return saveInterval;
  }
}
