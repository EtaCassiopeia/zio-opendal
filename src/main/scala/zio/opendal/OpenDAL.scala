package zio.opendal

import zio._
import zio.opendal.core.{OpenDAL => OpenDALTrait}
import zio.opendal.impl.{NativeOpenDAL, TestOpenDAL}
import zio.opendal.config.LayerConfig
import zio.opendal.error.OpenDALError
import zio.opendal.scheme.Scheme
import zio.opendal.options._
import org.apache.opendal.{AsyncOperator, Capability, Entry, Metadata, NativeLibrary, OperatorInfo, PresignedRequest}
import zio.Duration
import scala.jdk.CollectionConverters._

/**
 * OpenDAL companion object providing layer construction and service accessors
 */
object OpenDAL {

  /**
   * Create a live OpenDAL layer using native libraries with configuration
   */
  def live(config: LayerConfig): ZLayer[Any, OpenDALError, OpenDALTrait] =
    ZLayer.scoped {
      for {
        _        <- config.validate
        _        <- ZIO.attempt(NativeLibrary.loadLibrary()).mapError(OpenDALError.fromThrowable)
        jConfig  <- ZIO
          .attempt(new java.util.HashMap[String, String](config.config.asJava))
          .mapError(OpenDALError.fromThrowable)
        operator <- ZIO
          .acquireRelease(
            ZIO.attemptBlocking(AsyncOperator.of(config.scheme.name, jConfig))
          )(op => ZIO.attemptBlocking(op.close()).ignore)
          .mapError(OpenDALError.fromThrowable)
      } yield new NativeOpenDAL(operator, config.retrySchedule)
    }

  /**
   * Create a live OpenDAL layer using native libraries (legacy API for
   * compatibility)
   */
  def live(
    scheme: Scheme,
    config: Map[String, String],
    retrySchedule: Schedule[Any, OpenDALError, Any] = Schedule.exponential(100.millis) && Schedule.recurs(3)
  ): ZLayer[Any, OpenDALError, OpenDALTrait] =
    live(LayerConfig(scheme, config, retrySchedule))

  /**
   * Create a test layer for testing without native library dependencies
   */
  def testLayer(scheme: Scheme, config: Map[String, String] = Map.empty): ZLayer[Any, Nothing, OpenDALTrait] =
    ZLayer.succeed(new TestOpenDAL(scheme, config))

  /**
   * Create a test layer using configuration
   */
  def testLayer(config: LayerConfig): ZLayer[Any, Nothing, OpenDALTrait] =
    testLayer(config.scheme, config.config)

  // Service accessors for all operations

  // Basic read operations
  def read(path: String): ZIO[OpenDALTrait, OpenDALError, Array[Byte]] =
    ZIO.serviceWithZIO[OpenDALTrait](_.read(path))

  def read(path: String, options: ReadOpts): ZIO[OpenDALTrait, OpenDALError, Array[Byte]] =
    ZIO.serviceWithZIO[OpenDALTrait](_.read(path, options))

  def read(path: String, offset: Long, length: Long): ZIO[OpenDALTrait, OpenDALError, Array[Byte]] =
    ZIO.serviceWithZIO[OpenDALTrait](_.read(path, offset, length))

  def readString(path: String): ZIO[OpenDALTrait, OpenDALError, String] =
    read(path).map(new String(_, "UTF-8"))

  def readString(path: String, options: ReadOpts): ZIO[OpenDALTrait, OpenDALError, String] =
    read(path, options).map(new String(_, "UTF-8"))

  // Basic write operations
  def write(path: String, data: Array[Byte]): ZIO[OpenDALTrait, OpenDALError, Unit] =
    ZIO.serviceWithZIO[OpenDALTrait](_.write(path, data))

  def write(path: String, content: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    ZIO.serviceWithZIO[OpenDALTrait](_.write(path, content))

  def write(path: String, data: Array[Byte], options: WriteOpts): ZIO[OpenDALTrait, OpenDALError, Unit] =
    ZIO.serviceWithZIO[OpenDALTrait](_.write(path, data, options))

  def write(path: String, content: String, options: WriteOpts): ZIO[OpenDALTrait, OpenDALError, Unit] =
    ZIO.serviceWithZIO[OpenDALTrait](_.write(path, content, options))

  def writeJson(path: String, content: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    write(path, content, WriteOpts.json)

  def writeText(path: String, content: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    write(path, content, WriteOpts.text)

  // Stat operations
  def stat(path: String): ZIO[OpenDALTrait, OpenDALError, Metadata] =
    ZIO.serviceWithZIO[OpenDALTrait](_.stat(path))

  def stat(path: String, options: StatOpts): ZIO[OpenDALTrait, OpenDALError, Metadata] =
    ZIO.serviceWithZIO[OpenDALTrait](_.stat(path, options))

  // File operations
  def delete(path: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    ZIO.serviceWithZIO[OpenDALTrait](_.delete(path))

  def copy(from: String, to: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    ZIO.serviceWithZIO[OpenDALTrait](_.copy(from, to))

  def rename(from: String, to: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    ZIO.serviceWithZIO[OpenDALTrait](_.rename(from, to))

  def exists(path: String): ZIO[OpenDALTrait, OpenDALError, Boolean] =
    ZIO.serviceWithZIO[OpenDALTrait](_.exists(path))

  // Directory operations
  def createDir(path: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    ZIO.serviceWithZIO[OpenDALTrait](_.createDir(path))

  def list(path: String): ZIO[OpenDALTrait, OpenDALError, List[Entry]] =
    ZIO.serviceWithZIO[OpenDALTrait](_.list(path))

  def list(path: String, options: ListOpts): ZIO[OpenDALTrait, OpenDALError, List[Entry]] =
    ZIO.serviceWithZIO[OpenDALTrait](_.list(path, options))

  def removeAll(path: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    ZIO.serviceWithZIO[OpenDALTrait](_.removeAll(path))

  // Presigned operations
  def presignRead(path: String, duration: Duration): ZIO[OpenDALTrait, OpenDALError, PresignedRequest] =
    ZIO.serviceWithZIO[OpenDALTrait](_.presignRead(path, duration))

  def presignWrite(path: String, duration: Duration): ZIO[OpenDALTrait, OpenDALError, PresignedRequest] =
    ZIO.serviceWithZIO[OpenDALTrait](_.presignWrite(path, duration))

  def presignStat(path: String, duration: Duration): ZIO[OpenDALTrait, OpenDALError, PresignedRequest] =
    ZIO.serviceWithZIO[OpenDALTrait](_.presignStat(path, duration))

  // Info operations
  def info: ZIO[OpenDALTrait, OpenDALError, OperatorInfo] =
    ZIO.serviceWithZIO[OpenDALTrait](_.info)

  def capabilities: ZIO[OpenDALTrait, OpenDALError, Capability] =
    ZIO.serviceWithZIO[OpenDALTrait](_.capabilities)

  // Convenience methods for common patterns

  /** Upload text file with appropriate content type */
  def uploadText(path: String, content: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    writeText(path, content)

  /** Upload JSON file with appropriate content type */
  def uploadJson(path: String, content: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    writeJson(path, content)

  /** Download and parse as string */
  def downloadText(path: String): ZIO[OpenDALTrait, OpenDALError, String] =
    readString(path)

  /** Check if file exists and is not empty */
  def existsAndNotEmpty(path: String): ZIO[OpenDALTrait, OpenDALError, Boolean] =
    for {
      exists <- exists(path)
      result <-
        if (exists) {
          stat(path).map(_.getContentLength > 0)
        } else {
          ZIO.succeed(false)
        }
    } yield result

  /** Get file size */
  def size(path: String): ZIO[OpenDALTrait, OpenDALError, Long] =
    stat(path).map(_.getContentLength)

  /** List all files recursively */
  def listRecursive(path: String): ZIO[OpenDALTrait, OpenDALError, List[Entry]] =
    list(path, ListOpts.recursive)

  /** Copy file and verify it exists */
  def copyAndVerify(from: String, to: String): ZIO[OpenDALTrait, OpenDALError, Unit] =
    for {
      _ <- copy(from, to)
      _ <- exists(to).filterOrFail(identity)(
        OpenDALError.fromThrowable(new RuntimeException(s"Copy verification failed: $to does not exist"))
      )
    } yield ()
}
