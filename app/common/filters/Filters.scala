package common.filters

import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter


/**
  * The simplest way to use a filter is to provide an implementation of the HttpFilters trait
  * This code is copied from https://www.playframework.com/documentation/2.4.x/ScalaHttpFilters
  * Injected two filters for http request.
  * The GzipFilter is implemented by Play Framework.
  * The LoggingFilter is customized for the application.
  *
  * author: gong_baiping
  * date: 11/20/15 1:21 PM
  * version: 0.1 (Scala 2.11.7, Play 2.4.2)
  * copyright: TonyGong, Inc.
  */
class Filters @Inject()(gzip: GzipFilter, log: LoggingFilter) extends HttpFilters {
  val filters = Seq(gzip, log)
}
