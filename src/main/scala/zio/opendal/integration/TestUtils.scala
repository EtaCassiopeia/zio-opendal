package zio.opendal.integration

import zio._
import zio.opendal.OpenDAL
import zio.opendal.core.{OpenDAL => OpenDALTrait}
import zio.opendal.config.LayerConfig
import zio.opendal.error.OpenDALError
import scala.util.Random

/** Simple testing utilities for ZIO OpenDAL */
object TestUtils {

  /** Create a test layer with in-memory storage */
  def memoryTestLayer: ZLayer[Any, Nothing, OpenDALTrait] =
    OpenDAL.testLayer(LayerConfig.memory())

  /** Generate random test data */
  def randomBytes(size: Int): Array[Byte] = {
    val bytes = new Array[Byte](size)
    Random.nextBytes(bytes)
    bytes
  }

  /** Generate random text content */
  def randomText(size: Int): String =
    Random.alphanumeric.take(size).mkString

  /** Create test file with random content */
  def createTestFile(path: String, size: Int = 1024): ZIO[OpenDALTrait, OpenDALError, String] =
    for {
      content <- ZIO.succeed(randomText(size))
      _       <- OpenDAL.writeText(path, content)
    } yield content

  /** Measure execution time */
  def timed[R, E, A](effect: ZIO[R, E, A]): ZIO[R, E, (Duration, A)] =
    for {
      start  <- Clock.nanoTime
      result <- effect
      end    <- Clock.nanoTime
      duration = Duration.fromNanos(end - start)
    } yield (duration, result)
}
