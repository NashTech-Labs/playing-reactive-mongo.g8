package common

import models.PageResults
import org.apache.commons.lang3.StringUtils
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponents
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.QueryOpts
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

/**
  * The MongoDao trait provides some basic helper method to operate mongodb.
  * Such as the common CRUD operations.
  *
  * author: gong_baiping
  * date: 11/13/15 11:33 AM
  * version: 0.1 (Scala 2.11.7, Play 2.4.2)
  * copyright: TonyGong, Inc.
  */
trait MongoDao extends ReactiveMongoComponents {

  /**
    * Abstraction method implemented by subtype.
    * collection likes a table from mysql.
    * {{{
    *   // collection matching employees table
    *   def collection: JSONCollection = db.collection[JSONCollection]("employees")
    * }}}
    *
    * @return
    */
  def collection: JSONCollection

  /**
    * Inserts the document, or updates it if it already exists in the collection.
    * @param entity, entity object
    * @param writer, Json serializer: write an implicit to define a serializer for any type
    * @tparam T, entity type
    * @return Future[WriteResult]
    */
  def saving[T](entity: T)(implicit writer: Writes[T]): Future[WriteResult] = collection.save(entity)

  /**
    * To find one document by id from mongodb
    * @param id, document id
    * @param reader, Json deserializer
    * @tparam T, entity type
    * @return Future[Option[T]]
    */
  def get[T](id: String)(implicit reader: Reads[T]): Future[Option[T]] =
    collection.find(Json.obj("_id" -> Json.obj("$oid" -> id))).cursor[T]().headOption

  /**
    * According to the query condition, to find matching documents from mongodb.
    * @param query, filter condition
    * @param reader, Json deserializer
    * @tparam T, entity type
    * @return Future[Seq[T]]
    */
  def find[T](query: JsObject)(implicit reader: Reads[T]): Future[Seq[T]] =
    collection.find(query).cursor[T]().collect[Seq]()

  /**
    * To find all documents from mongodb without any query condition.
    * @param reader, Json deserializer
    * @tparam T, entity type
    * @return Future[Seq[T]]
    */
  def findAll[T](implicit reader: Reads[T]): Future[Seq[T]] =
    collection.genericQueryBuilder.cursor[T]().collect[Seq](10)

  /**
    * To page all documents from mongodb.
    * @param reader, Json deserializer
    * @tparam T, entity type
    * @return Future[Page[T]]
    */
  def page[T](page: Int, pageSize: Int)(implicit reader: Reads[T]): Future[PageResults[T]] = {
    val futureDocuments = collection
      .genericQueryBuilder
      .options(QueryOpts(page * pageSize, pageSize))
      .cursor[T]()
      .collect[Seq](pageSize)
    val count = counting

    for {
      documents <- futureDocuments
      totals <- count
    } yield PageResults(documents, page, pageSize, totals)
  }

  /**
    * According to the query condition, to page matching documents from mongodb.
    * @param query, filter condition
    * @param reader, Json deserializer
    * @tparam T, entity type
    * @return Future[Page[T]]
    */
  def search[T](query: JsObject, page: Int, pageSize: Int = 10)(implicit reader: Reads[T]): Future[PageResults[T]] = {

    // to check the query is empty or not
    val queryFlag = query.fieldSet.map(_._2.as[String]).forall(StringUtils.isBlank(_))
    val (futureDocuments, futureTotals) = if (queryFlag) {
      (collection
        .genericQueryBuilder
        .options(QueryOpts(page * pageSize, pageSize))
        .cursor[T]()
        .collect[Seq](pageSize), counting)
    } else {
      (collection
        .find(query)
        .options(QueryOpts(page * pageSize, pageSize))
        .cursor[T]()
        .collect[Seq](pageSize), countByQuery(query))
    }

    for {
      documents <- futureDocuments
      totals <- futureTotals
    } yield PageResults(documents, page, pageSize * page, totals)
  }

  /**
    * To delete the document physically by id from db
    * @param id, document id
    * @param reader, Json deserializer
    * @tparam T, entity type
    * @return Future[WriteResult]
    */
  def deleting[T](id: String)(implicit reader: Reads[T]): Future[WriteResult] =
    collection.remove(Json.obj("_id" -> Json.obj("$oid" -> id)), firstMatchOnly = true)

  /**
    * To count the all documents number of the current collection
    * @return Future[Int]
    */
  def counting: Future[Int] = collection.count()

  /**
    * To count the all documents number of the current collection
    * @return Future[Int]
    */
  def countByQuery(query: JsObject): Future[Int] = collection.count(Some(query))

}
