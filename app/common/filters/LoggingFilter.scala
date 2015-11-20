package common.filters

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

/**
  * The following is a simple filter that times and logs how long a request takes to execute in Play framework
  * This code is copied from https://www.playframework.com/documentation/2.4.x/ScalaHttpFilters
  *
  * author: gong_baiping
  * date: 11/20/15 1:18 PM
  * version: 0.1 (Scala 2.11.7, Play 2.4.2)
  * copyright: TonyGong, Inc.
  */
class LoggingFilter extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    // request start time
    val startTime = System.currentTimeMillis
    nextFilter(requestHeader).map { result =>
      // request uri
      val uri = requestHeader.uri
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      // filtering the assets and webjars uri
      if (!uri.contains("assets") && !uri.contains("webjars")) {
        Logger.info(s"${requestHeader.method} ${requestHeader.uri} " +
          s"took ${requestTime}ms and returned ${result.header.status}")
      }
      result.withHeaders("Request-Time" -> s"${requestTime}ms")
    }
  }
}
