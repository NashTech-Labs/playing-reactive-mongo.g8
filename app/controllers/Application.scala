package controllers

import java.util.concurrent.TimeoutException

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{ Action, Controller }
import play.api.data.Form
import play.api.data.Forms.{ date, ignored, mapping, nonEmptyText }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json, Json.toJsFieldJsValueWrapper

import play.modules.reactivemongo.{
  MongoController, ReactiveMongoApi, ReactiveMongoComponents
}
import reactivemongo.play.json._, collection.JSONCollection

import reactivemongo.bson.BSONObjectID

import models.{ Employee, JsonFormats, Page }, JsonFormats.employeeFormat
import views.html

/*
 * Example using ReactiveMongo + Play JSON library.
 *
 * There are two approaches demonstrated in this controller:
 * - using JsObjects directly
 * - using case classes that can be turned into Json using Reads and Writes.
 *
 * This controller uses case classes and their associated Reads/Writes
 * to read or write JSON structures.
 *
 * Instead of using the default Collection implementation (which interacts with
 * BSON structures + BSONReader/BSONWriter), we use a specialized
 * implementation that works with JsObject + Reads/Writes.
 *
 * Of course, you can still use the default Collection implementation
 * (BSONCollection.) See ReactiveMongo examples to learn how to use it.
 */
class Application @Inject() (
  val reactiveMongoApi: ReactiveMongoApi,
  val messagesApi: MessagesApi)
    extends Controller with MongoController with ReactiveMongoComponents {

  implicit val timeout = 10.seconds

  /**
   * Describe the employee form (used in both edit and create screens).
   */
  val employeeForm = Form(
    mapping(
      "id" -> ignored(BSONObjectID.generate: BSONObjectID),
      "name" -> nonEmptyText,
      "address" -> nonEmptyText,
      "dob" -> date("yyyy-MM-dd"),
      "joiningDate" -> date("yyyy-MM-dd"),
      "designation" -> nonEmptyText)(Employee.apply)(Employee.unapply))

  /*
   * Get a JSONCollection (a Collection implementation that is designed to work
   * with JsObject, Reads and Writes.)
   * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
   * the collection reference to avoid potential problems in development with
   * Play hot-reloading.
   */
  def collection: Future[JSONCollection] = connection.database(db.name).
    map(_.collection[JSONCollection]("employees"))

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //
  import play.api.data.Form
  import models._
  import models.JsonFormats._

  /**
   * Handle default path requests, redirect to employee list
   */
  def index = Action { Home }

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Application.list())

  /**
   * Display the paginated list of employees.
   *
   * @param page Current page number (starts from 0)
   * @param orderBy Column to be sorted
   * @param filter Filter applied on employee names
   */
  def list(page: Int, orderBy: Int, filter: String) = Action.async { implicit request =>
    val mongoFilter = {
      if (filter.length > 0) Json.obj("name" -> filter)
      else Json.obj()
    }
    val filtered = collection.flatMap(
      _.find(mongoFilter).cursor[Employee]().collect[List]())

    filtered.map({ employees =>
      implicit val msg = messagesApi.preferred(request)

      Ok(html.list(Page(employees, 0, 10, 20), orderBy, filter))
    }).recover {
      case t: TimeoutException =>
        Logger.error("Problem found in employee list process")
        InternalServerError(t.getMessage)
    }
  }

  /**
   * Display the 'edit form' of a existing Employee.
   *
   * @param id Id of the employee to edit
   */
  def edit(id: String) = Action.async { request =>
    val futureEmp = collection.flatMap(_.find(Json.obj("_id" -> Json.obj("$oid" -> id))).cursor[Employee]().collect[List]())

    futureEmp.map { emps: List[Employee] =>
      implicit val msg = messagesApi.preferred(request)

      Ok(html.editForm(id, employeeForm.fill(emps.head)))
    }.recover {
      case t: TimeoutException =>
        Logger.error("Problem found in employee edit process")
        InternalServerError(t.getMessage)
    }
  }

  /**
   * Handle the 'edit form' submission
   *
   * @param id Id of the employee to edit
   */
  def update(id: String) = Action.async { implicit request =>
    employeeForm.bindFromRequest.fold(
      { formWithErrors =>
        implicit val msg = messagesApi.preferred(request)
        Future.successful(BadRequest(html.editForm(id, formWithErrors)))
      },
      employee => {
        val futureUpdateEmp = collection.flatMap(_.update(Json.obj("_id" -> Json.obj("$oid" -> id)), employee.copy(_id = BSONObjectID(id))))

        futureUpdateEmp.map { result =>
          Home.flashing("success" -> s"Employee ${employee.name} has been updated")
        }.recover {
          case t: TimeoutException =>
            Logger.error("Problem found in employee update process")
            InternalServerError(t.getMessage)
        }
      })
  }

  /**
   * Display the 'new employee form'.
   */
  def create = Action { request =>
    implicit val msg = messagesApi.preferred(request)
    Ok(html.createForm(employeeForm))
  }

  /**
   * Handle the 'new employee form' submission.
   */
  def save = Action.async { implicit request =>
    employeeForm.bindFromRequest.fold(
      { formWithErrors =>
        implicit val msg = messagesApi.preferred(request)
        Future.successful(BadRequest(html.createForm(formWithErrors)))
      },
      employee => {
        val futureUpdateEmp = collection.flatMap(_.insert(employee.copy(_id = BSONObjectID.generate)))

        futureUpdateEmp.map { result =>
          Home.flashing("success" -> s"Employee ${employee.name} has been created")
        }.recover {
          case t: TimeoutException =>
            Logger.error("Problem found in employee update process")
            InternalServerError(t.getMessage)
        }
      })
  }

  /**
   * Handle employee deletion.
   */
  def delete(id: String) = Action.async {
    val futureInt = collection.flatMap(_.remove(Json.obj("_id" -> Json.obj("$oid" -> id)), firstMatchOnly = true))

    futureInt.map(i => Home.flashing("success" -> "Employee has been deleted")).recover {
      case t: TimeoutException =>
        Logger.error("Problem deleting employee")
        InternalServerError(t.getMessage)
    }
  }

}
