package common

import org.sedis.Pool
import play.api.Play._
import play.api.cache.Cache
import play.api.libs.Codecs

/**
  * Add comments.
  *
  * author: gong_baiping
  * date: 11/20/15 4:41 PM
  * version: 0.1 (Scala 2.11.7, Play 2.4.2)
  * copyright: TonyGong, Inc.
  */
trait RedisCache {

  object CacheTime {
    /** caching duration seconds */
    val OneMinutes = 60
    val FiveMinutes = 300
    val TenMinutes = 600
    val HalfAnHour = 1800
    val AnHour = 3600
    val OneDay = 86400
    val HalfDay = 43200
    val OneWeek = 604800
    val OneYear = 31536000
    val Forever = 0
  }

  object CacheKey {
    def key(parameter: String) = Codecs.sha1(parameter)
  }

  /**
    * Because the underlying Sedis Pool was injected for the cache module to use,
    * you can just inject the sedis Pool yourself, something like this:
    */
  lazy val sedisPool = current.injector.instanceOf[Pool]

  /**
   * Remove data from the cache by specified key.
   * @param key
   */
  def removeCached(key: String): Unit = Cache.remove(key)

  /**
   * Save the data to the cache, for the specified number of seconds
   * The default duration is 10 minutes.
   * @param key cached key
   * @param value cached data
   * @param duration expired duration, default 10 minutes.
   */
  def save2Cache(key: String, value: Any, duration: Int = CacheTime.TenMinutes): Unit =
    Cache.set(key, value, duration)

  /**
   * Get data from the cache by specified key.
   * @param key cached key
   * @return cached option value
   */
  def getFromCached[T](key: String): Option[T] = Cache.getAs[T](key)

}
