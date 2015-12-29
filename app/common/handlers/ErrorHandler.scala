package common.handlers

import play.api.Logger
import play.api.http.HttpErrorHandler
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent._


/**
  * A custom error handler can be supplied by creating a class called ErrorHandler that implements HttpErrorHandler
  *
  * author: gong_baiping
  * date: 11/20/15 1:55 PM
  * version: 0.1 (Scala 2.11.7, Play 2.4.2)
  * copyright: TonyGong, Inc.
  */
class ErrorHandler extends HttpErrorHandler {

  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    Future.successful(
      Status(statusCode)("A client error occurred: " + message)
    )
  }

  /**
    * This error handler hook is used for global context
    * Printing the request uri and error message.
    * {{{
    *   2015-12-10 17:41:24,622 [error] application - Problem found in GET /employees process and returned 500
        Error message, such as: throw a run time exception by hand!
    * }}}
    *
    * @param request
    * @param exception
    * @return
    */
  def onServerError(request: RequestHeader, exception: Throwable) = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage) // TODO: To redirect the 500 error page
    ).map { result =>
      // request uri
      val uri = request.uri
      // filtering the assets and webjars uri
      if (!uri.contains("assets") && !uri.contains("webjars")) {
        Logger.error(s"Problem found in ${request.method} ${request.uri} process " +
          s"and returned ${result.header.status}\n${exception.getMessage}")
      }
      result
    }
  }

}
