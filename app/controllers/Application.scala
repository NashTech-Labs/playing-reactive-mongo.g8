package controllers

import java.util.concurrent.TimeoutException

import common.BaseController
import models.Employee
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.{date, ignored, mapping, nonEmptyText}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.bson.BSONObjectID
import views.html

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

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
 *
 * ReactiveMongoApi which is the interface to MongoDB.
 */
class Application extends BaseController {

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
  def collection: JSONCollection = db.collection[JSONCollection]("employees")

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //
  import models.JsonFormats._
  import models._

  /**
    * Handle default path requests, redirect to employee list
    */
  def index = Action {
    Home
  }

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
  def list(page: Int, orderBy: Int, filter: String) = AsyncAction { implicit request =>
    // searching by query and pagination
    search[Employee](Json.obj("name" -> filter), page) map {
      page =>
        Logger.debug(s"[List Results]: ${Json.toJson(page)}")
        Ok(html.list(page, orderBy, filter))
      //        Ok(Json.toJson(page))
    }
  }

  /**
    * Display the 'edit form' of a existing Employee.
    *
    * @param id Id of the employee to edit
    */
  def edit(id: String) = AsyncAction { request =>
    val futureEmp = get[Employee](id)
    futureEmp.map { emps =>
      Ok(html.editForm(id, employeeForm.fill(emps.head)))
    }
  }

  /**
    * Handle the 'edit form' submission
    *
    * @param id Id of the employee to edit
    */
  def update(id: String) = AsyncAction { implicit request =>
    employeeForm.bindFromRequest.fold(
      { formWithErrors =>
        Future.successful(BadRequest(html.editForm(id, formWithErrors)))
      },
      employee => {
        val futureUpdateEmp = saving(employee.copy(_id = BSONObjectID(id)))
        futureUpdateEmp.map { result =>
          Home.flashing("success" -> s"Employee ${employee.name} has been updated")
        }
      })
  }

  /**
    * Display the 'new employee form'.
    */
  def create = Action { request =>
    Ok(html.createForm(employeeForm))
  }

  /**
    * Handle the 'new employee form' submission.
    */
  def save = AsyncAction { implicit request =>
    employeeForm.bindFromRequest.fold(
      { formWithErrors =>
        Future.successful(BadRequest(html.createForm(formWithErrors)))
      },
      employee => {
        val futureUpdateEmp = saving(employee.copy(_id = BSONObjectID.generate))
        futureUpdateEmp.map { result =>
          Home.flashing("success" -> s"Employee ${employee.name} has been created")
        }
      })
  }

  /**
    * Handle employee deletion.
    */
  def delete(id: String) = AsyncAction { request =>
    val futureInt = deleting[Employee](id)
    futureInt.map(i => Home.flashing("success" -> "Employee has been deleted")).recover {
      case t: TimeoutException =>
        Logger.error("Problem deleting employee")
        InternalServerError(t.getMessage)
    }
  }


}
