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

# Chapter 3: Operation Manual

## Data Query
### Time Slice Query

This chapter mainly introduces the relevant examples of time slice query using IoTDB SELECT statements. Detailed SQL syntax and usage specifications can be found in [SQL Documentation](/#/Documents/latest/chap5/sec1). You can also use the [Java JDBC](/#/Documents/latest/chap6/sec1) standard interface to execute related queries.

#### Select a Column of Data Based on a Time Interval

The SQL statement is:

```
select temperature from root.ln.wf01.wt01 where time < 2017-11-01T00:08:00.000
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is the temperature sensor (temperature). The SQL statement requires that all temperature sensor values before the time point of "2017-11-01T00:08:00.000" be selected.

The execution result of this SQL statement is as follows:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/23614968/61280074-da1c0a00-a7e9-11e9-8eb8-3809428043a8.png"></center>

#### Select Multiple Columns of Data Based on a Time Interval

The SQL statement is:

```
select status, temperature from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000;
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is "status" and "temperature". The SQL statement requires that the status and temperature sensor values between the time point of "2017-11-01T00:05:00.000" and "2017-11-01T00:12:00.000" be selected.

The execution result of this SQL statement is as follows:
<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/23614968/61280328-40a12800-a7ea-11e9-85b9-3b8db67673a3.png"></center>

#### Select Multiple Columns of Data for the Same Device According to Multiple Time Intervals
IoTDB supports specifying multiple time interval conditions in a query. Users can combine time interval conditions at will according to their needs. For example, the SQL statement is:

```
select status,temperature from root.ln.wf01.wt01 where (time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000) or (time >= 2017-11-01T16:35:00.000 and time <= 2017-11-01T16:37:00.000);
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is "status" and "temperature"; the statement specifies two different time intervals, namely "2017-11-01T00:05:00.000 to 2017-11-01T00:12:00.000" and "2017-11-01T16:35:00.000 to 2017-11-01T16:37:00.000". The SQL statement requires that the values of selected timeseries satisfying any time interval be selected.

The execution result of this SQL statement is as follows:
<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/23614968/61280449-780fd480-a7ea-11e9-8ed0-70fa9dfda80f.png"></center>


#### Choose Multiple Columns of Data for Different Devices According to Multiple Time Intervals
The system supports the selection of data in any column in a query, i.e., the selected columns can come from different devices. For example, the SQL statement is:

```
select wf01.wt01.status,wf02.wt02.hardware from root.ln where (time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000) or (time >= 2017-11-01T16:35:00.000 and time <= 2017-11-01T16:37:00.000);
```
which means:

The selected timeseries are "the power supply status of ln group wf01 plant wt01 device" and "the hardware version of ln group wf02 plant wt02 device"; the statement specifies two different time intervals, namely "2017-11-01T00:05:00.000 to 2017-11-01T00:12:00.000" and "2017-11-01T16:35:00.000 to 2017-11-01T16:37:00.000". The SQL statement requires that the values of selected timeseries satisfying any time interval be selected.

The execution result of this SQL statement is as follows:
<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577450-dcfe0800-1ef4-11e9-9399-4ba2b2b7fb73.jpg"></center>

### Down-Frequency Aggregate Query
This section mainly introduces the related examples of down-frequency aggregation query, using the [GROUP BY clause](/#/Documents/latest/chap5/sec1), which is used to partition the result set according to the user's given partitioning conditions and aggregate the partitioned result set. IoTDB supports partitioning result sets according to time intervals, and by default results are sorted by time in ascending order. You can also use the [Java JDBC](/#/Documents/latest/chap6/sec1) standard interface to execute related queries.

The GROUP BY statement provides users with three types of specified parameters:

* Parameter 1: Time interval for dividing the time axis
* Parameter 2: Time axis origin position (optional)
* Parameter 3: The display window(s) (one or more) on the time axis

The actual meanings of the three types of parameters are shown in Figure 3.2 below. Among them, the paramter 2 is optional. Next we will give three typical examples of frequency reduction aggregation: parameter 2 specified, parameter 2 not specified, and time filtering conditions specified.

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577465-e8513380-1ef4-11e9-84c6-d0690f2a8113.jpg">

**Figure 3.2 The actual meanings of the three types of parameters**</center>

#### Down-Frequency Aggregate Query without Specifying the Time Axis Origin Position
The SQL statement is:

```
select count(status), max_value(temperature) from root.ln.wf01.wt01 group by (1d, [2017-11-01T00:00:00, 2017-11-07T23:00:00]);
```
which means:

Since the user does not specify the time axis origin position, the GROUP BY statement will by default set the origin at 0 (+0 time zone) on January 1, 1970.

The first parameter of the GROUP BY statement above is the time interval for dividing the time axis. Taking this parameter (1d) as time interval and the default origin as the dividing origin, the time axis is divided into several continuous intervals, which are [0,1d], [1d, 2d], [2d, 3d], etc.

The second parameter of the GROUP BY statement above is the display window paramter, which determines the final display range is [2017-11-01T00:00:00, 2017-11-07T23:00:00].

Then the system will use the time and value filtering condition in the WHERE clause and the second parameter of the GROUP BY statement as the data filtering condition to obtain the data satisfying the filtering condition (which in this case is the data in the range of [2017-11-01T00:00:00, 2017-11-07 T23:00:00]), and map these data to the previously segmented time axis (in this case there are mapped data in every 1-day period from 2017-11-01T00:00:00 to 2017-11-07T23:00:00:00).

Since there is data for each time period in the result range to be displayed, the execution result of the SQL statement is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577537-277f8480-1ef5-11e9-9b0f-c477f3b71acb.jpg"></center>

#### Down-Frequency Aggregate Query Specifying the Time Axis Origin Position
The SQL statement is:

```
select count(status), max_value(temperature) from root.ln.wf01.wt01 group by (1d, 2017-11-03 00:00:00, [2017-11-01 00:00:00, 2017-11-07 23:00:00]);
```

which means:

Since the user specifies the time axis origin position parameter as 2017-11-03 00:00:00, the GROUP BY statement will set the origin at 0 (system default time zone) on November 3, 2017.

The first parameter of the GROUP BY statement above is the time interval for dividing the time axis. Taking this parameter (1d) as time interval and the speicified origin as the dividing origin, the time axis is divided into several continuous intervals, which are [2017-11-02T00:00:00, 2017-11-03T00:00:00], [2017-11-03T00:00:00, 2017-11-04T00:00:00], etc.

The third parameter of the GROUP BY statement above is the display window paramter, which determines the final display range is [2017-11-01T00:00:00, 2017-11-07T23:00:00].

hen the system will use the time and value filtering condition in the WHERE clause and the second parameter of the GROUP BY statement as the data filtering condition to obtain the data satisfying the filtering condition (which in this case is the data in the range of [2017-11-01T00:00:00, 2017-11-07T23:00:00]), and map these data to the previously segmented time axis (in this case there are mapped data in every 1-day period from 2017-11-01T00:00:00 to 2017-11-07T23:00:00:00).

Since there is data for each time period in the result range to be displayed, the execution result of the SQL statement is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577563-3a925480-1ef5-11e9-88da-2d7e3eb4c951.jpg"></center>

#### Down-Frequency Aggregate Query Specifying the Time Filtering Conditions
The SQL statement is:

```
select count(status), max_value(temperature) from root.ln.wf01.wt01 where time > 2017-11-03T06:00:00 and temperature > 20 group by(1h, [2017-11-03T00:00:00, 2017-11-03T23:00:00]);
```
which means:

Since the user does not specify the time axis origin position, the GROUP BY statement will by default set the origin at 0 (+0 time zone) on January 1, 1970.

The first parameter of the GROUP BY statement above is the time interval for dividing the time axis. Taking this parameter (1d) as time interval and the default origin as the dividing origin, the time axis is divided into several continuous intervals, which are [0,1d], [1d, 2d], [2d, 3d], etc.

The second parameter of the GROUP BY statement above is the display window paramter, which determines the final display range is [2017-11-03T00:00:00, 2017-11-03T23:00:00].

Then the system will use the time and value filtering condition in the WHERE clause and the second parameter of the GROUP BY statement as the data filtering condition to obtain the data satisfying the filtering condition (which in this case is the data in the range of (2017-11-03T06:00:00, 2017-11-03T23:00:00] and satisfying root.ln.wf01.wt01.temperature > 20), and map these data to the previously segmented time axis (in this case there are mapped data in every 1-day period from 2017-11-03T00:06:00 to 2017-11-03T23:00:00).

Since there is  no data in the result range [2017-11-03T00:00:00, 2017-11-03T00:06:00], the aggregation results of this segment will be null. There is data in all other time periods in the result range to be displayed. The execution result of the SQL statement is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577582-441bbc80-1ef5-11e9-8b54-3ad1f586bbc4.jpg"></center>

It is worth noting that the path after SELECT in GROUP BY statement must be aggregate function, otherwise the system will give the corresponding error prompt, as shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/19167280/61517091-fbbf0080-aa38-11e9-8623-cdadf1ccf5d6.png"></center>

### Automated Fill
In the actual use of IoTDB, when doing the query operation of timeseries, situations where the value is null at some time points may appear, which will obstruct the further analysis by users. In order to better reflect the degree of data change, users expect missing values to be automatically filled. Therefore, the IoTDB system introduces the function of Automated Fill.

Automated fill function refers to filling empty values according to the user's specified method and effective time range when performing timeseries queries for single or multiple columns. If the queried point's value is not null, the fill function will not work.

> Note: In the current version 0.7.0, IoTDB provides users with two methods: Previous and Linear. The previous method fills blanks with previous value. The linear method fills blanks through linear fitting. And the fill function can only be used when performing point-in-time queries.

#### Fill Function
* Previous Function

When the value of the queried timestamp is null, the value of the previous timestamp is used to fill the blank. The formalized previous method is as follows (see Section 7.1.3.6 for detailed syntax):

```
select <path> from <prefixPath> where time = <T> fill(<data_type>[previous, <before_range>], …)
```

Detailed descriptions of all parameters are given in Table 3-4.

<center>**Table 3-4 Previous fill paramter list**

|Parameter name (case insensitive)|Interpretation|
|:---|:---|
|path, prefixPath|query path; mandatory field|
|T|query timestamp (only one can be specified); mandatory field|
|data\_type|the type of data used by the fill method. Optional values are int32, int64, float, double, boolean, text; optional field|
|before\_range|represents the valid time range of the previous method. The previous method works when there are values in the [T-before\_range, T] range. When before\_range is not specified, before\_range takes the default value T; optional field|
</center>

Here we give an example of filling null values using the previous method. The SQL statement is as follows:

```
select temperature from root.sgcc.wf03.wt01 where time = 2017-11-01T16:37:50.000 fill(float[previous, 1m]) 
```
which means:

Because the timeseries root.sgcc.wf03.wt01.temperature is null at 2017-11-01T16:37:50.000, the system uses the previous timestamp of 2017-11-01T16:37:50.000 (and the timestamp is in the [2017-11-01T16:36:50.000, 2017-11-01T16:37:50.000] time range) for fill and display.

On the [sample data](/#/Documents/latest/chap3/sec1), the execution result of this statement is shown below:
<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577616-67df0280-1ef5-11e9-9dff-2eb8342074eb.jpg"></center>

It is worth noting that if there is no value in the specified valid time range, the system will not fill the null value, as shown below:
<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577679-9f4daf00-1ef5-11e9-8d8b-06a58de6efc1.jpg"></center>

* Linear Method

When the value of the queried timestamp is null, the value of the previous and the next timestamp is used to fill the blank. The formalized linear method is as follows:

```
select <path> from <prefixPath> where time = <T> fill(<data_type>[linear, <before_range>, <after_range>]…)
```
Detailed descriptions of all parameters are given in Table 3-5.

<center>**Table 3-5 Linear fill paramter list**

|Parameter name (case insensitive)|Interpretation|
|:---|:---|
|path, prefixPath|query path; mandatory field|
|T|query timestamp (only one can be specified); mandatory field|
|data_type|the type of data used by the fill method. Optional values are int32, int64, float, double, boolean, text; optional field|
|before\_range, after\_range|represents the valid time range of the linear method. The previous method works when there are values in the [T-before\_range, T+after\_range] range. When before\_range and after\_range are not explicitly specified, both before\_range and after\_range default to infinity; optional field|
</center>

Here we give an example of filling null values using the linear method. The SQL statement is as follows:

```
select temperature from root.sgcc.wf03.wt01 where time = 2017-11-01T16:37:50.000 fill(float [linear, 1m, 1m])
```
which means:

Because the timeseries root.sgcc.wf03.wt01.temperature is null at 2017-11-01T16:37:50.000, the system uses the previous timestamp 2017-11-01T16:37:00.000 (and the timestamp is in the [2017-11-01T16:36:50.000, 2017-11-01T16:37:50.000] time range) and its value 21.927326, the next timestamp 2017-11-01T16:39:00.000 (and the timestamp is in the [2017-11-01T16:36:50.000, 2017-11-01T16:37:50.000] time range) and its value 25.311783 to perform linear fitting calculation: 21.927326 + (25.311783-21.927326)/60s*50s = 24.747707

On the [sample data](/#/Documents/latest/chap3/sec1), the execution result of this statement is shown below:
<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577727-d4f29800-1ef5-11e9-8ff3-3bb519da3993.jpg"></center>

#### Correspondence between Data Type and Fill Method
Data types and the supported fill methods are shown in Table 3-6.

<center>**Table 3-6 Data types and the supported fill methods**

|Data Type|Supported Fill Methods|
|:---|:---|
|boolean|previous|
|int32|previous, linear|
|int64|previous, linear|
|float|previous, linear|
|double|previous, linear|
|text|previous|
</center>

It is worth noting that IoTDB will give error prompts for fill methods that are not supported by data types, as shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577741-e340b400-1ef5-11e9-9238-a4eaf498ab84.jpg"></center>

When the fill method is not specified, each data type bears its own default fill methods and parameters. The corresponding relationship is shown in Table 3-7.

<center>**Table 3-7 Default fill methods and parameters for various data types**

|Data Type|Default Fill Methods and Parameters|
|:---|:---|
|boolean|previous, 0|
|int32|linear, 0, 0|
|int64|linear, 0, 0|
|float|linear, 0, 0|
|double|linear, 0, 0|
|text|previous, 0|
</center>

> Note: In version 0.7.0, at least one fill method should be specified in the Fill statement.

### Row and Column Control over Query Results

IoTDB provides [LIMIT/SLIMIT](/#/Documents/latest/chap5/sec1) clause and [OFFSET/SOFFSET](/#/Documents/latest/chap5/sec1) clause in order to make users have more control over query results. The use of LIMIT and SLIMIT clauses allows users to control the number of rows and columns of query results, and the use of OFFSET and SOFSET clauses allows users to set the starting position of the results for display.

This chapter mainly introduces related examples of row and column control of query results. You can also use the [Java JDBC](/#/Documents/latest/chap6/sec1) standard interface to execute queries.

#### Row Control over Query Results
By using LIMIT and OFFSET clauses, users can control the query results in a row-related manner. We will demonstrate how to use LIMIT and OFFSET clauses through the following examples.

* Example 1: basic LIMIT clause

The SQL statement is:

```
select status, temperature from root.ln.wf01.wt01 limit 10
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is "status" and "temperature". The SQL statement requires the first 10 rows of the query result be returned.

The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577752-efc50c80-1ef5-11e9-9071-da2bbd8b9bdd.jpg"></center>


* Example 2: LIMIT clause with OFFSET

The SQL statement is:

```
select status, temperature from root.ln.wf01.wt01 limit 5 offset 3
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is "status" and "temperature". The SQL statement requires rows 3 to 7 of the query result be returned (with the first row numbered as row 0).

The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577773-08352700-1ef6-11e9-883f-8d353bef2bdc.jpg"></center>

* Example 3: LIMIT clause combined with WHERE clause

The SQL statement is:

```
select status,temperature from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time< 2017-11-01T00:12:00.000 limit 2 offset 3
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is "status" and "temperature". The SQL statement requires rows 3 to 4 of  the status and temperature sensor values between the time point of "2017-11-01T00:05:00.000" and "2017-11-01T00:12:00.000" be returned (with the first row numbered as row 0).

The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577789-15521600-1ef6-11e9-86ca-d7b2c947367f.jpg"></center>

* Example 4: LIMIT clause combined with GROUP BY clause

The SQL statement is:

```
select count(status), max_value(temperature) from root.ln.wf01.wt01 group by (1d,[2017-11-01T00:00:00, 2017-11-07T23:00:00]) limit 5 offset 3
```
which means:

The SQL statement clause requires rows 3 to 7 of the query result be returned (with the first row numbered as row 0).

The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577796-1e42e780-1ef6-11e9-8987-be443000a77e.jpg"></center>

It is worth noting that because the current FILL clause can only fill in the missing value of timeseries at a certain time point, that is to say, the execution result of FILL clause is exactly one line, so LIMIT and OFFSET are not expected to be used in combination with FILL clause, otherwise errors will be prompted. For example, executing the following SQL statement:

```
select temperature from root.sgcc.wf03.wt01 where time = 2017-11-01T16:37:50.000 fill(float[previous, 1m]) limit 10
```

The SQL statement will not be executed and the corresponding error prompt is given as follows:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/19167280/61517266-6e2fe080-aa39-11e9-8015-154a8e8ace30.png"></center>

#### Column Control over Query Results

By using SLIMIT and SOFFSET clauses, users can control the query results in a column-related manner. We will demonstrate how to use SLIMIT and SOFFSET clauses through the following examples.

* Example 1: basic SLIMIT clause

The SQL statement is:

```
select * from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000 slimit 1
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is the first column under this device, i.e., the power supply status. The SQL statement requires the status sensor values between the time point of "2017-11-01T00:05:00.000" and "2017-11-01T00:12:00.000" be selected.

The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577813-30bd2100-1ef6-11e9-94ef-dbeb450cf319.jpg"></center>

* Example 2: SLIMIT clause with SOFFSET

The SQL statement is:

```
select * from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000 slimit 1 soffset 1
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is the second column under this device, i.e., the temperature. The SQL statement requires the temperature sensor values between the time point of "2017-11-01T00:05:00.000" and "2017-11-01T00:12:00.000" be selected.

The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577827-39adf280-1ef6-11e9-81b5-876769607cd2.jpg"></center>

* Example 3: SLIMIT clause combined with GROUP BY clause

The SQL statement is:

```
select max_value(*) from root.ln.wf01.wt01 group by (1d, [2017-11-01T00:00:00, 2017-11-07T23:00:00]) slimit 1 soffset 1
```

The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577840-44688780-1ef6-11e9-8abc-04ae78efa85b.jpg"></center>

* Example 4: SLIMIT clause combined with FILL clause

The SQL statement is:

```
select * from root.sgcc.wf03.wt01 where time = 2017-11-01T16:37:50.000 fill(float[previous, 1m]) slimit 1 soffset 1
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is the second column under this device, i.e., the temperature.

The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577855-4d595900-1ef6-11e9-8541-a4accd714b75.jpg"></center>

It is worth noting that SLIMIT clause is expected to be used in conjunction with star path or prefix path, and the system will prompt errors when SLIMIT clause is used in conjunction with complete path query. For example, executing the following SQL statement:

```
select status,temperature from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000 slimit 1
```

The SQL statement will not be executed and the corresponding error prompt is given as follows:
<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577867-577b5780-1ef6-11e9-978c-e02c1294bcc5.jpg"></center>

#### Row and Column Control over Query Results

In addition to row or column control over query results, IoTDB allows users to control both rows and columns of query results. Here is a complete example with both LIMIT clauses and SLIMIT clauses.

The SQL statement is:

```
select * from root.ln.wf01.wt01 limit 10 offset 100 slimit 2 soffset 0
```
which means:

The selected device is ln group wf01 plant wt01 device; the selected timeseries is columns 0 to 1 under this device (with the first column numbered as column 0). The SQL statement clause requires rows 100 to 109 of the query result be returned (with the first row numbered as row 0).

The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51577879-64984680-1ef6-11e9-9d7b-57dd60fab60e.jpg"></center>

####  Error Handling

When the parameter N/SN of LIMIT/SLIMIT exceeds the size of the result set, IoTDB will return all the results as expected. For example, the query result of the original SQL statement consists of six rows, and we select the first 100 rows through the LIMIT clause:

```
select status,temperature from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000 limit 100
```
The result is shown below:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51578187-ad9cca80-1ef7-11e9-897a-83e66a0f3d94.jpg"></center>

When the parameter N/SN of LIMIT/SLIMIT clause exceeds the allowable maximum value (N/SN is of type int32), the system will prompt errors. For example, executing the following SQL statement:

```
select status,temperature from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000 limit 1234567890123456789
```
The SQL statement will not be executed and the corresponding error prompt is given as follows:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/19167280/61517469-e696a180-aa39-11e9-8ca5-42ea991d520e.png"></center>

When the parameter N/SN of LIMIT/SLIMIT clause is not a positive intege, the system will prompt errors. For example, executing the following SQL statement:

```
select status,temperature from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000 limit 13.1
```

The SQL statement will not be executed and the corresponding error prompt is given as follows:

<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/19167280/61518094-68d39580-aa3b-11e9-993c-fc73c27540f7.png"></center>

When the parameter OFFSET of LIMIT clause exceeds the size of the result set, IoTDB will return an empty result set. For example, executing the following SQL statement:

```
select status,temperature from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000 limit 2 offset 6
```
The result is shown below:
<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51578227-c60ce500-1ef7-11e9-98eb-175beb8d4086.jpg"></center>

When the parameter SOFFSET of SLIMIT clause is not smaller than the number of available timeseries, the system will prompt errors. For example, executing the following SQL statement:

```
select * from root.ln.wf01.wt01 where time > 2017-11-01T00:05:00.000 and time < 2017-11-01T00:12:00.000 slimit 1 soffset 2
```
The SQL statement will not be executed and the corresponding error prompt is given as follows:
<center><img style="width:100%; max-width:800px; max-height:600px; margin-left:auto; margin-right:auto; display:block;" src="https://user-images.githubusercontent.com/13203019/51578237-cd33f300-1ef7-11e9-9aef-2a717c56ab54.jpg"></center>
