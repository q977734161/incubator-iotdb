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
## 概览
- Cli / Shell工具
    - Cli  / Shell运行方式
    - Cli / Shell运行参数

<!-- /TOC -->

# Cli / Shell工具
IOTDB为用户提供CLI/Shell工具用于启动客户端和服务端程序。下面介绍每个CLI/Shell工具的运行方式和相关参数。
> \$IOTDB\_HOME表示IoTDB的安装目录所在路径。

## Cli  / Shell运行方式
安装后的IoTDB中有一个默认用户：`root`，默认密码为`root`。用户可以使用该用户尝试运行IoTDB客户端以测试服务器是否正常启动。客户端启动脚本为$IOTDB_HOME/bin文件夹下的`start-client`脚本。启动脚本时需要指定运行IP和PORT。以下为服务器在本机启动，且用户未更改运行端口号的示例，默认端口为6667。若用户尝试连接远程服务器或更改了服务器运行的端口号，请在-h和-p项处使用服务器的IP和PORT。	

Linux系统与MacOS系统启动命令如下：

```
  Shell > ./bin/start-client.sh -h 127.0.0.1 -p 6667 -u root -pw root
```
Windows系统启动命令如下：

```
  Shell > \bin\start-client.bat -h 127.0.0.1 -p 6667 -u root -pw root
```
回车后即可成功启动客户端。启动后出现如图提示即为启动成功。
```
 _____       _________  ______   ______
|_   _|     |  _   _  ||_   _ `.|_   _ \
  | |   .--.|_/ | | \_|  | | `. \ | |_) |
  | | / .'`\ \  | |      | |  | | |  __'.
 _| |_| \__. | _| |_    _| |_.' /_| |__) |
|_____|'.__.' |_____|  |______.'|_______/  version <version>


IoTDB> login successfully
IoTDB>
```
输入`quit`或`exit`可退出Client结束本次会话，Client输出`quit normally`表示退出成功。

## Cli / Shell运行参数

|参数名|参数类型|是否为必需参数| 说明| 例子 |
|:---|:---|:---|:---|:---|
|-disableIS08601 |没有参数 | 否 |如果设置了这个参数，IoTDB将以数字的形式打印时间戳(timestamp)。|-disableIS08601|
|-h <`host`> |string类型，不需要引号|是|IoTDB客户端连接IoTDB服务器的IP地址。|-h 10.129.187.21|
|-help|没有参数|否|打印IoTDB的帮助信息|-help|
|-p <`port`>|int类型|是|IoTDB连接服务器的端口号，IoTDB默认运行在6667端口。|-p 6667|
|-pw <`password`>|string类型，不需要引号|否|IoTDB连接服务器所使用的密码。如果没有输入密码IoTDB会在Cli端提示输入密码。|-pw root|
|-u <`username`>|string类型，不需要引号|是|IoTDB连接服务器锁使用的用户名。|-u root|
|-maxPRC <`maxPrintRowCount`>|int类型|否|设置IoTDB返回客户端命令行中所显示的最大行数。|-maxPRC 10|

下面展示一条客户端命令，功能是连接IP为10.129.187.21的主机，端口为6667 ，用户名为root，密码为root，以数字的形式打印时间戳，IoTDB命令行显示的最大行数为10。

Linux系统与MacOS系统启动命令如下：

```
  Shell >./bin/start-client.sh -h 10.129.187.21 -p 6667 -u root -pw root -disableIS08601 -maxPRC 10
```
Windows系统启动命令如下：

```
  Shell > \bin\start-client.bat -h 10.129.187.21 -p 6667 -u root -pw root -disableIS08601 -maxPRC 10
```
