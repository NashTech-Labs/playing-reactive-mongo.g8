package common

import controllers.routes
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

/**
  * This trait is used for authentication.
  * Provide security features.
  *
  * author: gong_baiping
  * date: 11/20/15 11:22 AM
  * version: 0.1 (Scala 2.11.7, Play 2.4.2)
  * copyright: TonyGong, Inc.
  */
trait Secured {
  self: Controller =>

  val UserIdentifier = "uuid"
  val UserName = "uname"
  val CurrentURLIdentifier = "crt_url"

  /**
    * Retrieve the connected user's uuid.
    */
  def getUUID(request: RequestHeader): Option[String] = request.session.get(UserIdentifier)

  /**
    * Redirect to login if the use in not authorized, and then, redirect to access link before login.
    */
  def onUnauthorized(request: RequestHeader): Result =
    Redirect(routes.Application.list())
      .withSession(request.session - CurrentURLIdentifier)
      .withSession(CurrentURLIdentifier -> request.uri)

  /**
    * Synchronous Authentication by uuid.
    * @param f
    * @return
    */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result): EssentialAction =
    Security.Authenticated(getUUID, onUnauthorized) { uuid =>
      Action(request => f(uuid)(request))
    }

  /**
    * Synchronous Authentication by uuid.
    * It is used for posting form data
    * @param f
    * @return
    */
  def IsAuthenticated2[A](bodyParser: BodyParser[A])(f: => String => Request[A] => Result): EssentialAction =
    Security.Authenticated(getUUID, onUnauthorized) { uuid =>
      Action(bodyParser)(request => f(uuid)(request))
    }

  /**
    * Asynchronous Authentication by uuid.
    * This is used for get request
    * @param f
    * @return
    */
  def IsAuthenticatedAsync(f: => String => Request[AnyContent] => Future[Result]): EssentialAction =
    Security.Authenticated(getUUID, onUnauthorized) { uuid =>
      Action.async { request =>
        f(uuid)(request).recover {
          case t: Exception =>
            Logger.error(s"Problem found in ${request.uri} process\n${t.getMessage}")
            InternalServerError(t.getMessage)
        }
      }
    }

  /**
    * Asynchronous Authentication by uuid and with specific body parser.
    * It is used for posting form data
    * @param f
    * @return
    */
  def IsAuthenticatedAsync2[A](bodyParser: BodyParser[A])(f: => String => Request[A] => Future[Result]): EssentialAction =
    Security.Authenticated(getUUID, onUnauthorized) { uuid =>
      Action.async(bodyParser) { request =>
        f(uuid)(request).recover {
          case t: Exception =>
            Logger.error(s"Problem found in ${request.uri} process\n${t.getMessage}")
            InternalServerError(t.getMessage)
        }
      }
    }

  /**
    * This async action is used for portal pages.
    * @param f
    * @return
    */
  def AsyncAction(f: => Request[AnyContent] => Future[Result]): EssentialAction =
    Action.async { request =>
      f(request).recover {
        case t: Exception =>
          Logger.error(s"Problem found in ${request.uri} process\n${t.getMessage}")
          InternalServerError(t.getMessage)
      }
    }

}
