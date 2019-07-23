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

# 第2章 IoTDB基本概念
## 主要概念及术语

IoTDB中涉及如下基本概念：

* 设备

设备指的是在实际场景中拥有传感器的装置。在IoTDB当中，所有的传感器都应有其对应的归属的设备。

* 传感器

传感器是指在实际场景中的一种检测装置，它能感受到被测量的信息，并能将感受到的信息按一定规律变换成为电信号或其他所需形式的信息输出并发送给IoTDB。在IoTDB当中，存储的所有的数据及路径，都是以传感器为单位进行组织。

* 存储组

用户可以将任意前缀路径设置成存储组。如有4条时间序列`root.vehicle.d1.s1`, `root.vehicle.d1.s2`, `root.vehicle.d2.s1`, `root.vehicle.d2.s2`，路径`root.vehicle`下的两个设备d1,d2可能属于同一个业主，或者同一个厂商，因此关系紧密。这时候就可以将前缀路径`root.vehicle`指定为一个存储组，这将使得IoTDB将其下的所有设备的数据存储在同一个文件夹下。未来`root.vehicle`下增加了新的设备，也将属于该存储组。

> 注意：不允许将一个完整路径(如上例的`root.vehicle.d1.s1`)设置成存储组。

设置合理数量的存储组可以带来性能的提升：既不会因为产生过多的存储文件（夹）导致频繁切换IO降低系统速度（并且会占用大量内存且出现频繁的内存-文件切换），也不会因为过少的存储文件夹（降低了并发度从而）导致写入命令阻塞。

用户应根据自己的数据规模和使用场景，平衡存储文件的存储组设置，以达到更好的系统性能。（未来会有官方提供的存储组规模与性能测试报告）

> 注意：一个时间序列其前缀必须属于某个存储组。在创建时间序列之前，用户必须设定该序列属于哪个存储组（Storage Group）。只有设置了存储组的时间序列才可以被持久化在磁盘上。

一个前缀路径一旦被设定成存储组后就不可以再更改这个存储组的设置。

一个存储组设定后，其对应的前缀路径的所有父层级与子层级也不允许再设置存储组（如，`root.ln`设置存储组后，root层级与`root.ln.wf01`不允许被设置为存储组）。

* 路径

在IoTDB中，路径是指符合以下约束的表达式：

```
path: LayerName (DOT LayerName)+
LayerName: Identifier | STAR
```

其中STAR为“*”，DOT为“.”。

我们称一个路径中在两个“.”中间的部分叫做一个层级，则`root.a.b.c`为一个层级为4的路径。

值得说明的是，在路径中，root为一个保留字符，它只允许出现在下文提到的时间序列的开头，若其他层级出现root，则无法解析，提示报错。

* 时间序列

时间序列是IoTDB中的核心概念。时间序列可以被看作产生时序数据的传感器的所在完整路径，在IoTDB中所有的时间序列必须以root开始、以传感器作为结尾。一个时间序列也可称为一个全路径。

例如，vehicle种类的device1有名为sensor1的传感器，则它的时间序列可以表示为：`root.vehicle.device1.sensor1`。 

> 注意：当前IoTDB支持的时间序列必须大于等于四层（之后会更改为两层）。

* 前缀路径

前缀路径是指一个时间序列的前缀所在的路径，一个前缀路径包含以该路径为前缀的所有时间序列。例如当前我们有`root.vehicle.device1.sensor1`, `root.vehicle.device1.sensor2`, `root.vehicle.device2.sensor1`三个传感器，则`root.vehicle.device1`前缀路径包含`root.vehicle.device1.sensor1`、`root.vehicle.device1.sensor2`两个时间序列，而不包含`root.vehicle.device2.sensor1`。

* 3.1.7 带`*`路径
为了使得在表达多个时间序列或表达前缀路径的时候更加方便快捷，IoTDB为用户提供带`*`路径。`*`可以出现在路径中的任何层。按照`*`出现的位置，带`*`路径可以分为两种：

`*`出现在路径的结尾；

`*`出现在路径的中间；

当`*`出现在路径的结尾时，其代表的是（`*`）+，即为一层或多层`*`。例如`root.vehicle.device1.*`代表的是`root.vehicle.device1.*`, `root.vehicle.device1.*.*`, `root.vehicle.device1.*.*.*`等所有以`root.vehicle.device1`为前缀路径的大于等于4层的路径。

当`*`出现在路径的中间，其代表的是`*`本身，即为一层。例如`root.vehicle.*.sensor1`代表的是以`root.vehicle`为前缀，以`sensor1`为后缀，层次等于4层的路径。

> 注意：`*`不能放在路径开头。

> 注意：`*`放在末尾时与前缀路径表意相同，例如`root.vehicle.*`与`root.vehicle`为相同含义。

* 时间戳

时间戳是一个数据到来的时间点。IoTDB时间戳分为两种类型，一种为LONG类型，一种为DATETIME类型（包含DATETIME-INPUT, DATETIME-DISPLAY两个小类）。

在用户在输入时间戳时，可以使用LONG类型的时间戳或DATETIME-INPUT类型的时间戳，其中DATETIME-INPUT类型的时间戳支持格式如表所示：

<center>**DATETIME-INPUT类型支持格式**

|format|
|:---|
|yyyy-MM-dd HH:mm:ss|
|yyyy/MM/dd HH:mm:ss|
|yyyy.MM.dd HH:mm:ss|
|yyyy-MM-dd'T'HH:mm:ss|
|yyyy/MM/dd'T'HH:mm:ss|
|yyyy.MM.dd'T'HH:mm:ss|
|yyyy-MM-dd HH:mm:ssZZ|
|yyyy/MM/dd HH:mm:ssZZ|
|yyyy.MM.dd HH:mm:ssZZ|
|yyyy-MM-dd'T'HH:mm:ssZZ|
|yyyy/MM/dd'T'HH:mm:ssZZ|
|yyyy.MM.dd'T'HH:mm:ssZZ|
|yyyy/MM/dd HH:mm:ss.SSS|
|yyyy-MM-dd HH:mm:ss.SSS|
|yyyy.MM.dd HH:mm:ss.SSS|
|yyyy/MM/dd'T'HH:mm:ss.SSS|
|yyyy-MM-dd'T'HH:mm:ss.SSS|
|yyyy.MM.dd'T'HH:mm:ss.SSS|
|yyyy-MM-dd HH:mm:ss.SSSZZ|
|yyyy/MM/dd HH:mm:ss.SSSZZ|
|yyyy.MM.dd HH:mm:ss.SSSZZ|
|yyyy-MM-dd'T'HH:mm:ss.SSSZZ|
|yyyy/MM/dd'T'HH:mm:ss.SSSZZ|
|yyyy.MM.dd'T'HH:mm:ss.SSSZZ|
|ISO8601 standard time format|

</center>

IoTDB在显示时间戳时可以支持LONG类型以及DATETIME-DISPLAY类型，其中DATETIME-DISPLAY类型可以支持用户自定义时间格式。自定义时间格式的语法如表所示：

<center>**DATETIME-DISPLAY自定义时间格式的语法**

|Symbol|Meaning|Presentation|Examples|
|:---:|:---:|:---:|:---:|
|G|era|era|era|
|C|century of era (>=0)|	number|	20|
| Y	|year of era (>=0)|	year|	1996|
|||||
| x	|weekyear|	year|	1996|
| w	|week of weekyear|	number	|27|
| e	|day of week	|number|	2|
| E	|day of week	|text	|Tuesday; Tue|
|||||
| y|	year|	year|	1996|
| D	|day of year	|number|	189|
| M	|month of year	|month|	July; Jul; 07|
| d	|day of month	|number|	10|
|||||
| a	|halfday of day	|text	|PM|
| K	|hour of halfday (0~11)	|number|	0|
| h	|clockhour of halfday (1~12)	|number|	12|
|||||
| H	|hour of day (0~23)|	number|	0|
| k	|clockhour of day (1~24)	|number|	24|
| m	|minute of hour|	number|	30|
| s	|second of minute|	number|	55|
| S	|fraction of second	|millis|	978|
|||||
| z	|time zone	|text	|Pacific Standard Time; PST|
| Z	|time zone offset/id|	zone|	-0800; -08:00; America/Los_Angeles|
|||||
| '|	escape for text	|delimiter|	　|
| ''|	single quote|	literal	|'|

</center>

* 值

一个时间序列的值是由实际中的传感器向IoTDB发送的数值。这个值可以按照数据类型被IoTDB存储，同时用户也可以针对这个值的数据类型选择压缩方式，以及对应的编码方式。数据类型与对应编码的详细信息请参见本文[数据类型](/#/Documents/latest/chap2/sec2)与[编码方式](/#/Documents/latest/chap2/sec3)节。

* 数据点

一个数据点是由一个时间戳-值对(timestamp, value)组成的。

* 数据的列

一个数据的列包含属于一个时间序列的所有值以及这些值相对应的时间戳。当有多个数据的列时，IoTDB会针对时间戳做合并，变为多个<时间戳-多值>对（timestamp, value, value, …）。
