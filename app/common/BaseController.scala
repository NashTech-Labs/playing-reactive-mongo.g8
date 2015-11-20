package common

import play.api.Play._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi}

/**
  * Add comments.
  *
  * author: gong_baiping
  * date: 11/12/15 7:01 PM
  * version: 0.1 (Scala 2.11.7, Play 2.4.2)
  * copyright: TonyGong, Inc.
  */
trait BaseController extends Controller with I18nSupport with MongoController with MongoDao {
  val messagesApi = BaseControllerHelper.messagesApi
  val reactiveMongoApi = BaseControllerHelper.reactiveMongoApi
}

/**
  * Providing some unique objects or some common methods for BaseController.
  */
object BaseControllerHelper {

  /** ReactiveMongoApi which is the interface to MongoDB.
    * And this is just a single object.
    */
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]

  /** Messages and internationalization
    * Instantiating only one messagesApi object, So put this code in an object.
    * References: https://www.playframework.com/documentation/2.4.x/ScalaI18N
    */
  lazy val messagesApi = current.injector.instanceOf[MessagesApi]
}

