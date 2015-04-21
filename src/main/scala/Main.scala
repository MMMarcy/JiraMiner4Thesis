import java.io.File
import java.text.SimpleDateFormat
import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import java.time.{ZoneOffset, LocalDateTime}
import java.util.{GregorianCalendar, Date}
import com.github.tototoshi.csv.CSVWriter
import spray.json._
import DefaultJsonProtocol._ // if you don't supply your own Protocol (see below)

/**
 * Created by marcello on 21/04/15.
 */
object Main {

  val CSV_HEADER = List("Key", "Priority",  "Estimated Time (h)", "Effective Time(h)", "Opened on", "Updated on", "Closed on", "Description")
  val dataFormatter = new SimpleDateFormat("yyyy-MM-DD'T'HH:mm:ss'.000'Z")




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

  def writeToCSV(writer: CSVWriter, issueElements: Map[String, JsValue]): Unit = {

    val innerFields = issueElements("fields").asJsObject.fields

    val key = issueElements("key").asInstanceOf[JsString].value
    val priority = innerFields("priority").asJsObject().fields("name").asInstanceOf[JsString].value
    val timeEstimate = innerFields("timeestimate") match {
      case JsNumber(v) => (v.intValue()/3600).toString
      case JsNull      => "-"
    }
    val opened = dataFormatter.parse(innerFields("created").asInstanceOf[JsString].value)
    val updated = innerFields.get("updated").flatMap{
      case JsString(s) => Option(dataFormatter.parse(s))
      case JsNull      => None
    }
    val closed = innerFields.get("resolutiondate").flatMap{
      case JsString(s) => Option(dataFormatter.parse(s))
      case JsNull      => None
    }
    val description = innerFields.get("description").flatMap{
      case JsString(s)  => Option(s)
      case JsNull       => None
    }
    val effectiveTime = closed
                            .flatMap(v => updated.map(_.getTime - v.getTime ))
                            .map(v => Math.abs(v/36000000))
                            .getOrElse("-").toString


    val list = List(key, priority, timeEstimate, effectiveTime, opened, updated.getOrElse("-").toString, closed.getOrElse("-").toString, description)
    writer.writeRow(list)

  }

}
