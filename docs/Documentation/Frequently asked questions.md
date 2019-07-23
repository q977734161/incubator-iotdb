<!--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

-->

<!-- TOC -->

## Outline

- Frequently Asked Questions
    - How can I identify my version of IoTDB?
    - Where can I find IoTDB logs?
    - Where can I find IoTDB data files?
    - How do I know how many time series are stored in IoTDB?
    - Can I use Hadoop and Spark to read TsFile in IoTDB?
    - How does IoTDB handle duplicate points?
    - How can I tell what type of the specific timeseries?
    - How can I change IoTDB's CLI time display format?

<!-- /TOC -->
# Frequently Asked Questions

## How can I identify my version of IoTDB?

There are several ways to identify the version of IoTDB that you are using:

* Launch IoTDB's Command Line Interface:

```
> ./start-client.sh -p 6667 -pw root -u root -h localhost
 _____       _________  ______   ______    
|_   _|     |  _   _  ||_   _ `.|_   _ \   
  | |   .--.|_/ | | \_|  | | `. \ | |_) |  
  | | / .'`\ \  | |      | |  | | |  __'.  
 _| |_| \__. | _| |_    _| |_.' /_| |__) | 
|_____|'.__.' |_____|  |______.'|_______/  version 0.7.0
```

* Check pom.xml file:

```
<version>0.7.0</version>
```

* Use JDBC API:

```
String iotdbVersion = tsfileDatabaseMetadata.getDatabaseProductVersion();
```

## Where can I find IoTDB logs?

Suppose your root directory is:

```
$ pwd
/workspace/incubator-iotdb

$ ls -l
iotdb/
iotdb-cli/
pom.xml
Readme.md
...
```

Let $IOTDB_HOME = /workspace/incubator-iotdb/iotdb/iotdb
Let $IOTDB_CLI_HOME = /workspace/incubator-iotdb/iotdb-cli/cli/

By default settings, the logs are stored under ```IOTDB_HOME/logs```. You can change log level and storage path by configuring ```logback.xml``` under ```IOTDB_HOME/conf```.

## Where can I find IoTDB data files?

By default settings, the data files (including tsfile, metadata, and WAL files) are stored under ```IOTDB_HOME/data```.

## How do I know how many time series are stored in IoTDB?

Use IoTDB's Command Line Interface:

```
IoTDB> show timeseries root.*
```

In the result, there will be a statement shows `Total timeseries number`, this number is the timeseries number in IoTDB.

If you are using Linux, you can use the following shell command:

```
> grep "0,root" $IOTDB_HOME/data/system/schema/mlog.txt |  wc -l
>   6
```

## Can I use Hadoop and Spark to read TsFile in IoTDB?

Yes. IoTDB has intense integration with Open Source Ecosystem. IoTDB supports [Hadoop](https://github.com/apache/incubator-iotdb/tree/master/hadoop), [Spark](https://github.com/apache/incubator-iotdb/tree/master/spark) and [Grafana](https://github.com/apache/incubator-iotdb/tree/master/grafana) visualization tool.

## How does IoTDB handle duplicate points?

A data point is uniquely identified by a full time series path (e.g. ```root.vehicle.d0.s0```) and timestamp. If you submit a new point with the same path and timestamp as an existing point, 

## How can I tell what type of the specific timeseries?

Use ```SHOW TIMESERIES <timeseries path>``` SQL in IoTDB's Command Line Interface:

For example, if you want to know the type of all timeseries, the \<timeseries path> should be `root`. The statement will be:

```
IoTDB> show timeseries root
```

If you want to query specific sensor, you can replace the \<timeseries path> with the sensor name. For example:

```
IoTDB> show timeseries root.fit.d1.s1
```

Otherwise, you can also use wildcard in timeseries path:

```
IoTDB> show timeseries root.fit.d1.*
```

## How can I change IoTDB's CLI time display format?

The default IoTDB's CLI time display format is human readable (e.g. ```1970-01-01T08:00:00.001```), if you want to display time in timestamp type or other readable format, add parameter ```-disableIS08601``` in start command:

```
> $IOTDB_CLI_HOME/bin/start-client.sh -h 127.0.0.1 -p 6667 -u root -pw root -disableIS08601
```

