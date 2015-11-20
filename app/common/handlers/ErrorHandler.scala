package common.handlers

import play.api.http.HttpErrorHandler
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

  def onServerError(request: RequestHeader, exception: Throwable) = {
    Future.successful(
      InternalServerError("A server error occurred: " + exception.getMessage)
    )
  }
}
