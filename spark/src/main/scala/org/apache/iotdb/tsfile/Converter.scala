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
package org.apache.iotdb.tsfile

import java.util

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileStatus
import org.apache.iotdb.tsfile.common.conf.TSFileDescriptor
import org.apache.iotdb.tsfile.common.constant.QueryConstant
import org.apache.iotdb.tsfile.file.metadata.TsFileMetaData
import org.apache.iotdb.tsfile.file.metadata.enums.{TSDataType, TSEncoding}
import org.apache.iotdb.tsfile.io.HDFSInput
import org.apache.iotdb.tsfile.read.TsFileSequenceReader
import org.apache.iotdb.tsfile.read.common.{Field, Path}
import org.apache.iotdb.tsfile.read.expression.impl.{BinaryExpression, GlobalTimeExpression, SingleSeriesExpression}
import org.apache.iotdb.tsfile.read.expression.{IExpression, QueryExpression}
import org.apache.iotdb.tsfile.read.filter.{TimeFilter, ValueFilter}
import org.apache.iotdb.tsfile.utils.Binary
import org.apache.iotdb.tsfile.write.record.TSRecord
import org.apache.iotdb.tsfile.write.record.datapoint.DataPoint
import org.apache.iotdb.tsfile.write.schema.{FileSchema, MeasurementSchema, SchemaBuilder}
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * A series has a name and a type.
  *
  * @param nameVar name
  * @param typeVar type
  */
class Series(nameVar: String, typeVar: TSDataType) {
  var seriesName: String = nameVar
  var seriesType: TSDataType = typeVar

  def getName = seriesName
  def getType = seriesType

  override def toString = s"[" + seriesName + "," + seriesType + "]"
}

/**
  * This object contains methods that are used to convert schema and data between SparkSQL and TSFile.
  *
  */
object Converter {

  /**
    * Get series from the given tsFileMetaData.
    *
    * @param tsFileMetaData TsFileMetaData
    * @return union series
    */
  def getSeries(tsFileMetaData: TsFileMetaData): util.ArrayList[Series] = {
    val series = new util.ArrayList[Series]()

    val devices = tsFileMetaData.getDeviceMap.keySet()
    val measurements = tsFileMetaData.getMeasurementSchema

    devices.foreach(d => {
      measurements.foreach(m => {
        val fullPath = d + "." + m._1
        series.add(new Series(fullPath, m._2.getType)
        )
      })
    })

    series
  }

  /**
    * Get union series in all tsfiles.
    * e.g. (tsfile1:s1,s2) & (tsfile2:s2,s3) = s1,s2,s3
    *
    * @param files tsfiles
    * @param conf  hadoop configuration
    * @return union series
    */
  def getUnionSeries(files: Seq[FileStatus], conf: Configuration): util.ArrayList[Series] = {
    val unionSeries = new util.ArrayList[Series]()
    var seriesSet: mutable.Set[String] = mutable.Set()

    files.foreach(f => {
      val in = new HDFSInput(f.getPath, conf)
      val reader = new TsFileSequenceReader(in)
      val tsFileMetaData = reader.readFileMetadata
      val devices = tsFileMetaData.getDeviceMap.keySet()
      val measurements = tsFileMetaData.getMeasurementSchema

      devices.foreach(d => {
        measurements.foreach(m => {
          val fullPath = d + "." + m._1
          if (!seriesSet.contains(fullPath)) {
            seriesSet += fullPath
            unionSeries.add(new Series(fullPath, m._2.getType)
            )
          }
        })
      })
      in.close()
    })

    unionSeries
  }

  /**
    * Convert TSFile data to SparkSQL data.
    *
    * @param field one data point in TsFile
    * @return SparkSQL data
    */
  def toSqlValue(field: Field): Any = {
    if (field == null)
      return null
    if (field.isNull)
      null
    else field.getDataType match {
      case TSDataType.BOOLEAN => field.getBoolV
      case TSDataType.INT32 => field.getIntV
      case TSDataType.INT64 => field.getLongV
      case TSDataType.FLOAT => field.getFloatV
      case TSDataType.DOUBLE => field.getDoubleV
      case TSDataType.TEXT => field.getStringValue
      case other => throw new UnsupportedOperationException(s"Unsupported type $other")
    }
  }

  /**
    * Construct fields with the TSFile data type converted to the SparkSQL data type.
    *
    * @param tsfileSchema tsfileSchema
    * @param addTimeField  true to add a time field; false to not
    * @return the converted list of fields
    */
  def toSqlField(tsfileSchema: util.ArrayList[Series], addTimeField: Boolean): ListBuffer[StructField] = {
    val fields = new ListBuffer[StructField]()

    if (addTimeField) {
      fields += StructField(QueryConstant.RESERVED_TIME, LongType, nullable = false)
    }

    tsfileSchema.foreach((series: Series) => {
      fields += StructField(series.getName, series.getType match {
        case TSDataType.BOOLEAN => BooleanType
        case TSDataType.INT32 => IntegerType
        case TSDataType.INT64 => LongType
        case TSDataType.FLOAT => FloatType
        case TSDataType.DOUBLE => DoubleType
        case TSDataType.TEXT => StringType
        case other => throw new UnsupportedOperationException(s"Unsupported type $other")
      }, nullable = true)
    })

    fields
  }

  /**
    * Convert TSFile columns to SparkSQL schema.
    *
    * @param tsfileSchema all time series information in TSFile
    * @return SparkSQL table schema with the time field added
    */
  def toSqlSchema(tsfileSchema: util.ArrayList[Series]): Option[StructType] = {
    val fields = toSqlField(tsfileSchema, true) // true to add the time field

    SchemaType(StructType(fields.toList), nullable = false).dataType match {
      case t: StructType => Some(t)
      case _ => throw new RuntimeException(
        s"""TSFile schema cannot be converted to a Spark SQL StructType:
           |${tsfileSchema.toString}
           |""".stripMargin)
    }
  }

  /**
    * Prepare queriedSchema from requiredSchema.
    *
    * @param requiredSchema requiredSchema
    * @param tsFileMetaData tsFileMetaData
    * @return
    */
  def prepSchema(requiredSchema: StructType, tsFileMetaData: TsFileMetaData): StructType = {
    var queriedSchema: StructType = new StructType()

    if (requiredSchema.isEmpty
      || (requiredSchema.size == 1 && requiredSchema.iterator.next().name == QueryConstant.RESERVED_TIME)) {
      // for example, (i) select count(*) from table; (ii) select time from table

      val fileSchema = Converter.getSeries(tsFileMetaData)
      queriedSchema = StructType(toSqlField(fileSchema, false).toList)

    } else { // Remove nonexistent schema according to the current file's metadata.
      // This may happen when queried TsFiles in the same folder do not have the same schema.

      val devices = tsFileMetaData.getDeviceMap.keySet()
      val measurementIds = tsFileMetaData.getMeasurementSchema.keySet()
      requiredSchema.foreach(f => {
        if (!QueryConstant.RESERVED_TIME.equals(f.name)) {
          val path = new org.apache.iotdb.tsfile.read.common.Path(f.name)
          if (devices.contains(path.getDevice) && measurementIds.contains(path.getMeasurement)) {
            queriedSchema = queriedSchema.add(f)
          }
        }
      })

    }

    queriedSchema
  }


  /**
    * Return the TsFile data type of given SparkSQL data type.
    *
    * @param dataType SparkSQL data type
    * @return TsFile data type
    */
  def getTsDataType(dataType: DataType): TSDataType = {
    dataType match {
      case BooleanType => TSDataType.BOOLEAN
      case IntegerType => TSDataType.INT32
      case LongType => TSDataType.INT64
      case FloatType => TSDataType.FLOAT
      case DoubleType => TSDataType.DOUBLE
      case StringType => TSDataType.TEXT
      case other => throw new UnsupportedOperationException(s"Unsupported type $other")
    }
  }

  /**
    * Construct MeasurementSchema from the given field.
    *
    * @param field   field
    * @param options encoding options
    * @return MeasurementSchema
    */
  def getSeriesSchema(field: StructField, options: Map[String, String]): MeasurementSchema = {
    val conf = TSFileDescriptor.getInstance.getConfig
    val dataType = getTsDataType(field.dataType)
    val encodingStr = dataType match {
      case TSDataType.BOOLEAN => options.getOrElse(QueryConstant.BOOLEAN, TSEncoding.PLAIN.toString)
      case TSDataType.INT32 => options.getOrElse(QueryConstant.INT32, TSEncoding.RLE.toString)
      case TSDataType.INT64 => options.getOrElse(QueryConstant.INT64, TSEncoding.RLE.toString)
      case TSDataType.FLOAT => options.getOrElse(QueryConstant.FLOAT, TSEncoding.RLE.toString)
      case TSDataType.DOUBLE => options.getOrElse(QueryConstant.DOUBLE, TSEncoding.RLE.toString)
      case TSDataType.TEXT => options.getOrElse(QueryConstant.BYTE_ARRAY, TSEncoding.PLAIN.toString)
      case other => throw new UnsupportedOperationException(s"Unsupported type $other")
    }
    val encoding = TSEncoding.valueOf(encodingStr)
    val fullPath = new Path(field.name)
    val measurement = fullPath.getMeasurement
    new MeasurementSchema(measurement, dataType, encoding)
  }

  /**
    * Given a SparkSQL struct type, generate the TsFile schema.
    * Note: Measurements of the same name should have the same schema.
    *
    * @param structType given sql schema
    * @return TsFile schema
    */
  def toTsFileSchema(structType: StructType, options: Map[String, String]): FileSchema = {
    val schemaBuilder = new SchemaBuilder()
    structType.fields.filter(f => {
      !QueryConstant.RESERVED_TIME.equals(f.name)
    }).foreach(f => {
      val seriesSchema = getSeriesSchema(f, options)
      schemaBuilder.addSeries(seriesSchema)
    })
    schemaBuilder.build()
  }

  /**
    * Convert a row in the spark table to a list of TSRecord.
    *
    * @param row given spark sql row
    * @return TSRecord
    */
  def toTsRecord(row: InternalRow, dataSchema: StructType): List[TSRecord] = {
    val time = row.getLong(0)
    val deviceToRecord = scala.collection.mutable.Map[String, TSRecord]()
    var index = 1

    dataSchema.fields.filter(f => {
      !QueryConstant.RESERVED_TIME.equals(f.name)
    }).foreach(f => {
      val name = f.name
      val fullPath = new Path(name)
      val device = fullPath.getDevice
      val measurement = fullPath.getMeasurement
      if (!deviceToRecord.contains(device)) {
        deviceToRecord.put(device, new TSRecord(time, device))
      }
      val tsRecord: TSRecord = deviceToRecord.getOrElse(device, new TSRecord(time, device))

      val dataType = getTsDataType(f.dataType)
      if (!row.isNullAt(index)) {
        val value = f.dataType match {
          case BooleanType => row.getBoolean(index)
          case IntegerType => row.getInt(index)
          case LongType => row.getLong(index)
          case FloatType => row.getFloat(index)
          case DoubleType => row.getDouble(index)
          case StringType => row.getString(index)
          case other => throw new UnsupportedOperationException(s"Unsupported type $other")
        }
        val dataPoint = DataPoint.getDataPoint(dataType, measurement, value.toString)
        tsRecord.addTuple(dataPoint)
      }
      index += 1
    })
    deviceToRecord.values.toList
  }


  /**
    * Construct queryExpression based on queriedSchema and filters.
    *
    * @param schema  selected columns
    * @param filters filters
    * @return query expression
    */
  def toQueryExpression(schema: StructType, filters: Seq[Filter]): QueryExpression = {
    //get paths from schema
    val paths = new util.ArrayList[org.apache.iotdb.tsfile.read.common.Path]
    schema.foreach(f => {
      if (!QueryConstant.RESERVED_TIME.equals(f.name)) { // the time field is excluded
        paths.add(new org.apache.iotdb.tsfile.read.common.Path(f.name))
      }
    })

    //remove invalid filters
    val validFilters = new ListBuffer[Filter]()
    filters.foreach { f => {
      if (isValidFilter(f))
        validFilters.add(f)
    }
    }
    if (validFilters.isEmpty) {
      val queryExpression = QueryExpression.create(paths, null)
      queryExpression
    } else {
      //construct filters to a binary tree
      var filterTree = validFilters.get(0)
      for (i <- 1 until validFilters.length) {
        filterTree = And(filterTree, validFilters.get(i))
      }

      //convert filterTree to FilterOperator
      val finalFilter = transformFilter(schema, filterTree)

      // create query expression
      val queryExpression = QueryExpression.create(paths, finalFilter)

      queryExpression
    }
  }

  private def isValidFilter(filter: Filter): Boolean = {
    filter match {
      case f: EqualTo => true
      case f: GreaterThan => true
      case f: GreaterThanOrEqual => true
      case f: LessThan => true
      case f: LessThanOrEqual => true
      case f: Or => isValidFilter(f.left) && isValidFilter(f.right)
      case f: And => isValidFilter(f.left) && isValidFilter(f.right)
      case f: Not => isValidFilter(f.child)
      case _ => false
    }
  }

  /**
    * Transform SparkSQL's filter binary tree to TsFile's filter expression.
    *
    * @param schema to get relative columns' dataType information
    * @param node   filter tree's node
    * @return TSFile filter expression
    */
  private def transformFilter(schema: StructType, node: Filter): IExpression = {
    var filter: IExpression = null
    node match {
      case node: Not =>
        throw new Exception("NOT filter is not supported now")

      case node: And =>
        filter = BinaryExpression.and(transformFilter(schema, node.left), transformFilter(schema, node.right))
        filter

      case node: Or =>
        filter = BinaryExpression.or(transformFilter(schema, node.left), transformFilter(schema, node.right))
        filter

      case node: EqualTo =>
        if (QueryConstant.RESERVED_TIME.equals(node.attribute.toLowerCase())) {
          filter = new GlobalTimeExpression(TimeFilter.eq(node.value.asInstanceOf[java.lang.Long]))
        } else {
          filter = constructFilter(schema, node.attribute, node.value, FilterTypes.Eq)
        }
        filter

      case node: LessThan =>
        if (QueryConstant.RESERVED_TIME.equals(node.attribute.toLowerCase())) {
          filter = new GlobalTimeExpression(TimeFilter.lt(node.value.asInstanceOf[java.lang.Long]))
        } else {
          filter = constructFilter(schema, node.attribute, node.value, FilterTypes.Lt)
        }
        filter

      case node: LessThanOrEqual =>
        if (QueryConstant.RESERVED_TIME.equals(node.attribute.toLowerCase())) {
          filter = new GlobalTimeExpression(TimeFilter.ltEq(node.value.asInstanceOf[java.lang.Long]))
        } else {
          filter = constructFilter(schema, node.attribute, node.value, FilterTypes.LtEq)
        }
        filter

      case node: GreaterThan =>
        if (QueryConstant.RESERVED_TIME.equals(node.attribute.toLowerCase())) {
          filter = new GlobalTimeExpression(TimeFilter.gt(node.value.asInstanceOf[java.lang.Long]))
        } else {
          filter = constructFilter(schema, node.attribute, node.value, FilterTypes.Gt)
        }
        filter

      case node: GreaterThanOrEqual =>
        if (QueryConstant.RESERVED_TIME.equals(node.attribute.toLowerCase())) {
          filter = new GlobalTimeExpression(TimeFilter.gtEq(node.value.asInstanceOf[java.lang.Long]))
        } else {
          filter = constructFilter(schema, node.attribute, node.value, FilterTypes.GtEq)
        }
        filter

      case other =>
        throw new Exception(s"Unsupported filter $other")
    }
  }

  def constructFilter(schema: StructType, nodeName: String, nodeValue: Any, filterType: FilterTypes.Value): IExpression = {
    val fieldNames = schema.fieldNames
    val index = fieldNames.indexOf(nodeName)
    if (index == -1) {
      // placeholder for an invalid filter in the current TsFile
      val filter = new SingleSeriesExpression(new Path(nodeName), null)
      filter
    } else {
      val dataType = schema.get(index).dataType

      filterType match {
        case FilterTypes.Eq =>
          dataType match {
            case BooleanType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.eq(nodeValue.asInstanceOf[java.lang.Boolean]))
              filter
            case IntegerType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.eq(nodeValue.asInstanceOf[java.lang.Integer]))
              filter
            case LongType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.eq(nodeValue.asInstanceOf[java.lang.Long]))
              filter
            case FloatType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.eq(nodeValue.asInstanceOf[java.lang.Float]))
              filter
            case DoubleType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.eq(nodeValue.asInstanceOf[java.lang.Double]))
              filter
            case StringType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.eq(new Binary(nodeValue.toString)))
              filter
            case other => throw new UnsupportedOperationException(s"Unsupported type $other")
          }
        case FilterTypes.Gt =>
          dataType match {
            case IntegerType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.gt(nodeValue.asInstanceOf[java.lang.Integer]))
              filter
            case LongType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.gt(nodeValue.asInstanceOf[java.lang.Long]))
              filter
            case FloatType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.gt(nodeValue.asInstanceOf[java.lang.Float]))
              filter
            case DoubleType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.gt(nodeValue.asInstanceOf[java.lang.Double]))
              filter
            case other => throw new UnsupportedOperationException(s"Unsupported type $other")
          }
        case FilterTypes.GtEq =>
          dataType match {
            case IntegerType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.gtEq(nodeValue.asInstanceOf[java.lang.Integer]))
              filter
            case LongType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.gtEq(nodeValue.asInstanceOf[java.lang.Long]))
              filter
            case FloatType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.gtEq(nodeValue.asInstanceOf[java.lang.Float]))
              filter
            case DoubleType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.gtEq(nodeValue.asInstanceOf[java.lang.Double]))
              filter
            case other => throw new UnsupportedOperationException(s"Unsupported type $other")
          }
        case FilterTypes.Lt =>
          dataType match {
            case IntegerType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.lt(nodeValue.asInstanceOf[java.lang.Integer]))
              filter
            case LongType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.lt(nodeValue.asInstanceOf[java.lang.Long]))
              filter
            case FloatType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.lt(nodeValue.asInstanceOf[java.lang.Float]))
              filter
            case DoubleType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.lt(nodeValue.asInstanceOf[java.lang.Double]))
              filter
            case other => throw new UnsupportedOperationException(s"Unsupported type $other")
          }
        case FilterTypes.LtEq =>
          dataType match {
            case IntegerType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.ltEq(nodeValue.asInstanceOf[java.lang.Integer]))
              filter
            case LongType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.ltEq(nodeValue.asInstanceOf[java.lang.Long]))
              filter
            case FloatType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.ltEq(nodeValue.asInstanceOf[java.lang.Float]))
              filter
            case DoubleType =>
              val filter = new SingleSeriesExpression(new Path(nodeName),
                ValueFilter.ltEq(nodeValue.asInstanceOf[java.lang.Double]))
              filter
            case other => throw new UnsupportedOperationException(s"Unsupported type $other")
          }
      }
    }
  }

  object FilterTypes extends Enumeration {
    val Eq, Gt, GtEq, Lt, LtEq = Value
  }

  class SparkSqlFilterException(message: String, cause: Throwable)
    extends Exception(message, cause) {
    def this(message: String) = this(message, null)
  }

  case class SchemaType(dataType: DataType, nullable: Boolean)

}
