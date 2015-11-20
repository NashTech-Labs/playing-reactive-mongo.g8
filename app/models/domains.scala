package models

import java.util.Date

import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

case class Employee(_id: BSONObjectID, name: String, address: String, dob: Date, joiningDate: Date, designation: String)

/**
  * Helper for pagination.
  */
case class PageResults[T](items: Seq[T], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object JsonFormats {

  import play.api.libs.json.Json

  // implicit format for reactivemongo.bson.BSONObjectID
  import play.modules.reactivemongo.json.BSONFormats._

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val employeeFormat = Json.format[Employee]

  // Generics results to Json responses
  implicit def pageResultsWrites[T](implicit fmt: Writes[T]): Writes[PageResults[T]] = new Writes[PageResults[T]] {
    def writes(ts: PageResults[T]) = JsObject(Seq(
      "page" -> JsNumber(ts.page),
      "offset" -> JsNumber(ts.offset),
      "total" -> JsNumber(ts.total),
      "items" -> JsArray(ts.items.map(Json.toJson(_)))
    ))
  }

}