import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.github.tototoshi.csv.CSVWriter
import spray.json._

import scala.language.implicitConversions

/**
 * Created by Marcello Steiner on 21/04/15.
 */
object Main {

  val ERROR_CELL = "####"
  val CSV_HEADER = List("Key", "Priority", "Estimated Time (h)", "Effective Time(h)", "Opened on", "Updated on", "Closed on", "Description")
  val dataFormatter = new SimpleDateFormat("yyyy-MM-DD'T'HH:mm:ss'.000'Z")
  val excelCompatibleDateFormat = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss")


  implicit def toProperDateString(maybeDate: Option[Date]): String = {
    maybeDate.fold(ERROR_CELL)(date => excelCompatibleDateFormat.format(date))
  }

  implicit def toStringIfDefined(maybeString: Option[String]): String = {
    maybeString.getOrElse(ERROR_CELL)
  }


  def main(args: Array[String]) {
    val file = new File(args(0))
    val outputCSV = new File("/tmp/output.csv")
    val csvWriter = CSVWriter.open(outputCSV)

    val rawJson = scala.io.Source.fromFile(file).mkString
    val parsedJson = rawJson.parseJson.asJsObject
    val issues = parsedJson.fields("issues").asInstanceOf[JsArray]

    csvWriter.writeRow(CSV_HEADER)
    issues.elements.foreach(JsonIssue => writeToCSV(csvWriter, JsonIssue.asJsObject.fields))
    csvWriter.close()
  }

  def getString(fieldName: String, fields: Map[String, JsValue]): Option[String] = {
    fields.get(fieldName).flatMap {
      case JsString(s) => Option(s)
      case JsNull => None
      case _ => throw new IllegalArgumentException("Not supposed to end up here")
    }
  }

  def getKey(fields: Map[String, JsValue]): Option[String] =
    getString("key", fields)

  def getPriority(fields: Map[String, JsValue]): Option[String] =
    getString("name", fields("priority").asJsObject().fields)

  def getTimeEstimateInHours(fields: Map[String, JsValue]): Option[String] = {
    fields("timeestimate") match {
      case JsNumber(v) => Some((v.doubleValue() / 3600D).toString)
      case JsNull => None
      case _ => throw new IllegalArgumentException("Not supposed to end up here")
    }
  }

  def getDate(fieldName: String, fieldsMapping: Map[String, JsValue]): Option[Date] = {
    fieldsMapping.get(fieldName).flatMap {
      case JsString(s) => Option(dataFormatter.parse(s))
      case JsNull => None
      case _ => throw new IllegalArgumentException("Not supposed to end up here")
    }
  }

  def calculateEffectiveTime(closed: Option[Date], updated: Option[Date]): Option[String] = {
    closed
      .flatMap(v => updated.map(_.getTime - v.getTime))
      .map(v => Math.abs(v.toDouble / 36000000D))
      .map(_.toString)
  }

  def writeToCSV(writer: CSVWriter, fields: Map[String, JsValue]): Unit = {

    val innerFields = fields("fields").asJsObject.fields

    val key: Option[String] = getKey(fields)
    val priority: Option[String] = getPriority(innerFields)
    val timeEstimate: Option[String] = getTimeEstimateInHours(innerFields)
    val opened: Option[Date] = getDate("created", innerFields)
    val updated: Option[Date] = getDate("updated", innerFields)
    val closed: Option[Date] = getDate("resolutiondate", innerFields)
    val description: Option[String] = getString("description", innerFields)
    val effectiveTime: Option[String] = calculateEffectiveTime(closed, updated)

    val list = List[String](key, priority, timeEstimate, effectiveTime, opened, updated, closed, description)
    writer.writeRow(list)

  }


}
