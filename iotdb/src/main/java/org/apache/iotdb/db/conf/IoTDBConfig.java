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
package org.apache.iotdb.db.conf;

import java.io.File;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.iotdb.db.metadata.MManager;
import org.apache.iotdb.db.service.TSServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoTDBConfig {

  private static final Logger logger = LoggerFactory.getLogger(IoTDBConfig.class);
  public static final String CONFIG_NAME = "iotdb-engine.properties";
  private static final String MULTI_DIR_STRATEGY_PREFIX =
      "org.apache.iotdb.db.conf.directories.strategy.";
  private static final String DEFAULT_MULTI_DIR_STRATEGY = "MaxDiskUsableSpaceFirstStrategy";

  private String rpcAddress = "0.0.0.0";

  /**
   * Port which the JDBC server listens to.
   */
  private int rpcPort = 6667;

  /**
   * Memory allocated for the read process
   */
  private long allocateMemoryForWrite = Runtime.getRuntime().maxMemory() * 6 / 10;

  /**
   * Memory allocated for the write process
   */
  private long allocateMemoryForRead = Runtime.getRuntime().maxMemory() * 3 / 10;

  /**
   * Is dynamic parameter adapter enable.
   */
  private boolean enableParameterAdapter = true;

  /**
   * Is the write ahead log enable.
   */
  private boolean enableWal = true;

  private volatile boolean readOnly = false;

  /**
   * When a certain amount of write ahead logs is reached, they will be flushed to the disk. It is
   * possible to lose at most flush_wal_threshold operations.
   */
  private int flushWalThreshold = 10000;

  /**
   * The cycle when write ahead log is periodically forced to be written to disk(in milliseconds) If
   * set this parameter to 0 it means call outputStream.force(true) after every each insert
   */
  private long forceWalPeriodInMs = 10;

  /**
   * Size of log buffer in each log node(in byte). If WAL is enabled and the size of a insert plan
   * is smaller than this parameter, then the insert plan will be rejected by WAL.
   */
  private int walBufferSize = 16 * 1024 * 1024;

  /**
   * system base dir, stores all system metadata and wal
   */
  private String baseDir = "data";

  /**
   * System directory, including version file for each storage group and metadata
   */
  private String systemDir = "data/system";

  /**
   * Data directory of data. It can be settled as dataDirs = {"data1", "data2", "data3"};
   */
  private String[] dataDirs = {"data/data"};

  /**
   * Strategy of multiple directories.
   */
  private String multiDirStrategyClassName = null;

  /**
   * Wal directory.
   */
  private String walFolder = "data/wal";

  /**
   * Data directory for index files (KV-match indexes).
   */
  private String indexFileDir = "data/index";

  /**
   * Maximum MemTable number in MemTable pool.
   */
  private int maxMemtableNumber = 20;

  /**
   * The maximum concurrent thread number for merging. When the value <=0 or > CPU core number, use
   * the CPU core number.
   */
  private int mergeConcurrentThreads = Runtime.getRuntime().availableProcessors();

  /**
   * The amount of data that is read every time when IoTDB merges data.
   */
  private int fetchSize = 10000;

  /**
   * How many threads can concurrently flush. When <= 0, use CPU core number.
   */
  private int concurrentFlushThread = Runtime.getRuntime().availableProcessors();

  private ZoneId zoneID = ZoneId.systemDefault();

  /**
   * When a TsFile's file size (in byte) exceed this, the TsFile is forced closed.
   */
  private long tsFileSizeThreshold = 512 * 1024 * 1024L;

  /**
   * When a memTable's size (in byte) exceeds this, the memtable is flushed to disk.
   */
  private long memtableSizeThreshold = 128 * 1024 * 1024L;

  /**
   * The statMonitor writes statistics info into IoTDB every backLoopPeriodSec secs. The default
   * value is 5s.
   */
  private int backLoopPeriodSec = 5;
  /**
   * Set true to enable statistics monitor service, false to disable statistics service.
   */
  private boolean enableStatMonitor = false;
  /**
   * Set the time interval when StatMonitor performs delete detection. The default value is 600s.
   */
  private int statMonitorDetectFreqSec = 60 * 10;
  /**
   * Set the maximum time to keep monitor statistics information in IoTDB. The default value is
   * 600s.
   */
  private int statMonitorRetainIntervalSec = 60 * 10;

  /**
   * Cache size of {@code checkAndGetDataTypeCache} in {@link MManager}.
   */
  private int mManagerCacheSize = 400000;

  /**
   * Is this IoTDB instance a receiver of sync or not.
   */
  private boolean isSyncEnable = true;
  /**
   * If this IoTDB instance is a receiver of sync, set the server port.
   */
  private int syncServerPort = 5555;
  /**
   * Set the language version when loading file including error information, default value is "EN"
   */
  private String languageVersion = "EN";
  /**
   * Choose a postBack strategy of merging historical data: 1. It's more likely to update historical
   * data, choose "true". 2. It's more likely not to update historical data or you don't know
   * exactly, choose "false".
   */
  private boolean updateHistoricalDataPossibility = false;

  private String ipWhiteList = "0.0.0.0/0";
  /**
   * Examining period of cache file reader : 100 seconds.
   */
  private long cacheFileReaderClearPeriod = 100000;

  /**
   * Replace implementation class of JDBC service
   */
  private String rpcImplClassName = TSServiceImpl.class.getName();

  /**
   * Is stat performance of sub-module enable.
   */
  private boolean enablePerformanceStat = false;

  /**
   * The display of stat performance interval in ms.
   */
  private long performanceStatDisplayInterval = 60000;

  /**
   * The memory used for stat performance.
   */
  private int performanceStatMemoryInKB = 20;
  /**
   * whether use chunkBufferPool.
   */
  private boolean chunkBufferPoolEnable = false;

  public IoTDBConfig() {
    // empty constructor
  }

  public ZoneId getZoneID() {
    return zoneID;
  }

  void updatePath() {
    formulateFolders();
    confirmMultiDirStrategy();
  }


  /**
   * if the folders are relative paths, add IOTDB_HOME as the path prefix
   */
  private void formulateFolders() {
    List<String> dirs = new ArrayList<>();
    dirs.add(baseDir);
    dirs.add(systemDir);
    dirs.add(walFolder);
    dirs.add(indexFileDir);
    dirs.addAll(Arrays.asList(dataDirs));

    String homeDir = System.getProperty(IoTDBConstant.IOTDB_HOME, null);
    for (int i = 0; i < dirs.size(); i++) {
      String dir = dirs.get(i);
      if (!new File(dir).isAbsolute() && homeDir != null && homeDir.length() > 0) {
        if (!homeDir.endsWith(File.separator)) {
          dir = homeDir + File.separatorChar + dir;
        } else {
          dir = homeDir + dir;
        }
        dirs.set(i, dir);
      }
    }
    baseDir = dirs.get(0);
    systemDir = dirs.get(1);
    walFolder = dirs.get(2);
    indexFileDir = dirs.get(3);
    for (int i = 0; i < dataDirs.length; i++) {
      dataDirs[i] = dirs.get(i + 4);
    }
  }


  private void confirmMultiDirStrategy() {
    if (getMultiDirStrategyClassName() == null) {
      multiDirStrategyClassName = DEFAULT_MULTI_DIR_STRATEGY;
    }
    if (!getMultiDirStrategyClassName().contains(".")) {
      multiDirStrategyClassName = MULTI_DIR_STRATEGY_PREFIX + multiDirStrategyClassName;
    }

    try {
      Class.forName(multiDirStrategyClassName);
    } catch (ClassNotFoundException e) {
      logger.warn("Cannot find given directory strategy {}, using the default value",
          getMultiDirStrategyClassName(), e);
      setMultiDirStrategyClassName(MULTI_DIR_STRATEGY_PREFIX + DEFAULT_MULTI_DIR_STRATEGY);
    }
  }

  public String[] getDataDirs() {
    return dataDirs;
  }

  public String getRpcAddress() {
    return rpcAddress;
  }

  void setRpcAddress(String rpcAddress) {
    this.rpcAddress = rpcAddress;
  }

  public int getRpcPort() {
    return rpcPort;
  }

  void setRpcPort(int rpcPort) {
    this.rpcPort = rpcPort;
  }

  public boolean isEnableWal() {
    return enableWal;
  }

  public void setEnableWal(boolean enableWal) {
    this.enableWal = enableWal;
  }

  public int getFlushWalThreshold() {
    return flushWalThreshold;
  }

  public void setFlushWalThreshold(int flushWalThreshold) {
    this.flushWalThreshold = flushWalThreshold;
  }

  public long getForceWalPeriodInMs() {
    return forceWalPeriodInMs;
  }

  public void setForceWalPeriodInMs(long forceWalPeriodInMs) {
    this.forceWalPeriodInMs = forceWalPeriodInMs;
  }

  public String getSystemDir() {
    return systemDir;
  }

  void setSystemDir(String systemDir) {
    this.systemDir = systemDir;
  }

  public String getWalFolder() {
    return walFolder;
  }

  void setWalFolder(String walFolder) {
    this.walFolder = walFolder;
  }

  void setDataDirs(String[] dataDirs) {
    this.dataDirs = dataDirs;
  }

  public String getMultiDirStrategyClassName() {
    return multiDirStrategyClassName;
  }

  void setMultiDirStrategyClassName(String multiDirStrategyClassName) {
    this.multiDirStrategyClassName = multiDirStrategyClassName;
  }

  public String getIndexFileDir() {
    return indexFileDir;
  }

  private void setIndexFileDir(String indexFileDir) {
    this.indexFileDir = indexFileDir;
  }

  public int getMergeConcurrentThreads() {
    return mergeConcurrentThreads;
  }

  void setMergeConcurrentThreads(int mergeConcurrentThreads) {
    this.mergeConcurrentThreads = mergeConcurrentThreads;
  }

  public int getFetchSize() {
    return fetchSize;
  }

  void setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
  }

  public int getMaxMemtableNumber() {
    return maxMemtableNumber;
  }

  public void setMaxMemtableNumber(int maxMemtableNumber) {
    this.maxMemtableNumber = maxMemtableNumber;
  }

  public int getConcurrentFlushThread() {
    return concurrentFlushThread;
  }

  void setConcurrentFlushThread(int concurrentFlushThread) {
    this.concurrentFlushThread = concurrentFlushThread;
  }

  void setZoneID(ZoneId zoneID) {
    this.zoneID = zoneID;
  }

  public long getTsFileSizeThreshold() {
    return tsFileSizeThreshold;
  }

  public void setTsFileSizeThreshold(long tsFileSizeThreshold) {
    this.tsFileSizeThreshold = tsFileSizeThreshold;
  }

  public int getBackLoopPeriodSec() {
    return backLoopPeriodSec;
  }

  void setBackLoopPeriodSec(int backLoopPeriodSec) {
    this.backLoopPeriodSec = backLoopPeriodSec;
  }

  public boolean isEnableStatMonitor() {
    return enableStatMonitor;
  }

  public void setEnableStatMonitor(boolean enableStatMonitor) {
    this.enableStatMonitor = enableStatMonitor;
  }

  public int getStatMonitorDetectFreqSec() {
    return statMonitorDetectFreqSec;
  }

  void setStatMonitorDetectFreqSec(int statMonitorDetectFreqSec) {
    this.statMonitorDetectFreqSec = statMonitorDetectFreqSec;
  }

  public int getStatMonitorRetainIntervalSec() {
    return statMonitorRetainIntervalSec;
  }

  void setStatMonitorRetainIntervalSec(int statMonitorRetainIntervalSec) {
    this.statMonitorRetainIntervalSec = statMonitorRetainIntervalSec;
  }

  public int getmManagerCacheSize() {
    return mManagerCacheSize;
  }

  void setmManagerCacheSize(int mManagerCacheSize) {
    this.mManagerCacheSize = mManagerCacheSize;
  }

  public boolean isSyncEnable() {
    return isSyncEnable;
  }

  void setSyncEnable(boolean syncEnable) {
    isSyncEnable = syncEnable;
  }

  public int getSyncServerPort() {
    return syncServerPort;
  }

  void setSyncServerPort(int syncServerPort) {
    this.syncServerPort = syncServerPort;
  }

  public String getLanguageVersion() {
    return languageVersion;
  }

  void setLanguageVersion(String languageVersion) {
    this.languageVersion = languageVersion;
  }

  public boolean isUpdateHistoricalDataPossibility() {
    return updateHistoricalDataPossibility;
  }

  void setUpdateHistoricalDataPossibility(boolean updateHistoricalDataPossibility) {
    this.updateHistoricalDataPossibility = updateHistoricalDataPossibility;
  }

  public String getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(String baseDir) {
    this.baseDir = baseDir;
  }

  public String getIpWhiteList() {
    return ipWhiteList;
  }

  public void setIpWhiteList(String ipWhiteList) {
    this.ipWhiteList = ipWhiteList;
  }

  public long getCacheFileReaderClearPeriod() {
    return cacheFileReaderClearPeriod;
  }

  public void setCacheFileReaderClearPeriod(long cacheFileReaderClearPeriod) {
    this.cacheFileReaderClearPeriod = cacheFileReaderClearPeriod;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getRpcImplClassName() {
    return rpcImplClassName;
  }

  public void setRpcImplClassName(String rpcImplClassName) {
    this.rpcImplClassName = rpcImplClassName;
  }

  public int getWalBufferSize() {
    return walBufferSize;
  }

  void setWalBufferSize(int walBufferSize) {
    this.walBufferSize = walBufferSize;
  }

  public boolean isChunkBufferPoolEnable() {
    return chunkBufferPoolEnable;
  }

  void setChunkBufferPoolEnable(boolean chunkBufferPoolEnable) {
    this.chunkBufferPoolEnable = chunkBufferPoolEnable;
  }

  public boolean isEnableParameterAdapter() {
    return enableParameterAdapter;
  }

  public void setEnableParameterAdapter(boolean enableParameterAdapter) {
    this.enableParameterAdapter = enableParameterAdapter;
  }

  public long getAllocateMemoryForWrite() {
    return allocateMemoryForWrite;
  }

  public void setAllocateMemoryForWrite(long allocateMemoryForWrite) {
    this.allocateMemoryForWrite = allocateMemoryForWrite;
  }

  public long getAllocateMemoryForRead() {
    return allocateMemoryForRead;
  }

  public void setAllocateMemoryForRead(long allocateMemoryForRead) {
    this.allocateMemoryForRead = allocateMemoryForRead;
  }

  public boolean isEnablePerformanceStat() {
    return enablePerformanceStat;
  }

  public void setEnablePerformanceStat(boolean enablePerformanceStat) {
    this.enablePerformanceStat = enablePerformanceStat;
  }

  public long getPerformanceStatDisplayInterval() {
    return performanceStatDisplayInterval;
  }

  public void setPerformanceStatDisplayInterval(long performanceStatDisplayInterval) {
    this.performanceStatDisplayInterval = performanceStatDisplayInterval;
  }

  public int getPerformanceStatMemoryInKB() {
    return performanceStatMemoryInKB;
  }

  public void setPerformanceStatMemoryInKB(int performanceStatMemoryInKB) {
    this.performanceStatMemoryInKB = performanceStatMemoryInKB;
  }

  public long getMemtableSizeThreshold() {
    return memtableSizeThreshold;
  }

  public void setMemtableSizeThreshold(long memtableSizeThreshold) {
    this.memtableSizeThreshold = memtableSizeThreshold;
  }
}
