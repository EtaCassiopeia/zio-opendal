package zio.opendal.impl

import zio._
import zio.opendal.core.OpenDAL
import zio.opendal.error._
import zio.opendal.options._
import org.apache.opendal._

import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters._

/** Native implementation using OpenDAL Java bindings */
final class NativeOpenDAL(
  operator: AsyncOperator,
  retrySchedule: Schedule[Any, OpenDALError, Any]
) extends OpenDAL {

  // Helper methods
  private def wrap[R](f: => CompletableFuture[R]): ZIO[Any, OpenDALError, R] =
    ZIO
      .fromCompletableFuture(f)
      .mapError(OpenDALError.fromThrowable)
      .retryOrElse(
        retrySchedule.whileInput(_.isRetriable),
        (e: OpenDALError, _: Any) => ZIO.fail(e)
      )

  // Basic operations
  def read(path: String): ZIO[Any, OpenDALError, Array[Byte]] =
    wrap(operator.read(path))

  def read(path: String, options: ReadOpts): ZIO[Any, OpenDALError, Array[Byte]] =
    // For now, simulate offset/length handling manually since options classes don't exist in this version
    for {
      fullData <- read(path)
      sliced = {
        val start = options.offset.getOrElse(0L).toInt
        val end   = options.length.map(len => (start + len).toInt).getOrElse(fullData.length)
        fullData.slice(start, end)
      }
    } yield sliced

  def read(path: String, offset: Long, length: Long): ZIO[Any, OpenDALError, Array[Byte]] =
    read(path, ReadOpts(Some(offset), Some(length)))

  def write(path: String, data: Array[Byte]): ZIO[Any, OpenDALError, Unit] =
    wrap(operator.write(path, data)).unit

  def write(path: String, content: String): ZIO[Any, OpenDALError, Unit] =
    write(path, content.getBytes("UTF-8"))

  def write(path: String, data: Array[Byte], options: WriteOpts): ZIO[Any, OpenDALError, Unit] =
    // Options are ignored for now in the native layer since the Java API doesn't support them in this version
    write(path, data)

  def write(path: String, content: String, options: WriteOpts): ZIO[Any, OpenDALError, Unit] =
    write(path, content.getBytes("UTF-8"), options)

  def stat(path: String): ZIO[Any, OpenDALError, Metadata] =
    wrap(operator.stat(path))

  def stat(path: String, options: StatOpts): ZIO[Any, OpenDALError, Metadata] =
    // Options are ignored for now in the native layer
    stat(path)

  // File operations
  def delete(path: String): ZIO[Any, OpenDALError, Unit] =
    wrap(operator.delete(path)).unit

  def copy(from: String, to: String): ZIO[Any, OpenDALError, Unit] =
    // Try to support copy through read+write if direct copy is not available
    for {
      data <- read(from)
      _    <- write(to, data)
    } yield ()

  def rename(from: String, to: String): ZIO[Any, OpenDALError, Unit] =
    copy(from, to) *> delete(from)

  def exists(path: String): ZIO[Any, OpenDALError, Boolean] =
    stat(path).as(true).catchSome { case _: NotFoundError =>
      ZIO.succeed(false)
    }

  // Directory operations
  def createDir(path: String): ZIO[Any, OpenDALError, Unit] =
    ZIO.unit // May not be supported in current version

  def list(path: String): ZIO[Any, OpenDALError, List[Entry]] =
    wrap(operator.list(path)).map(_.asScala.toList)

  def list(path: String, options: ListOpts): ZIO[Any, OpenDALError, List[Entry]] =
    // Simulate list options by filtering results
    list(path).map { entries =>
      val filtered = if (options.recursive) entries else entries.filter(!_.getPath.drop(path.length).contains("/"))
      options.limit.map(limit => filtered.take(limit.toInt)).getOrElse(filtered)
    }

  def removeAll(path: String): ZIO[Any, OpenDALError, Unit] =
    // Simulate removeAll by listing and deleting each item
    for {
      entries <- list(path)
      _       <- ZIO.foreachDiscard(entries)(entry => delete(entry.getPath))
    } yield ()

  // Presigned operations - may not be available in current version
  def presignRead(path: String, duration: Duration): ZIO[Any, OpenDALError, PresignedRequest] =
    ZIO.fail(UnsupportedOperationError("Presigned URLs not supported in this OpenDAL version"))

  def presignWrite(path: String, duration: Duration): ZIO[Any, OpenDALError, PresignedRequest] =
    ZIO.fail(UnsupportedOperationError("Presigned URLs not supported in this OpenDAL version"))

  def presignStat(path: String, duration: Duration): ZIO[Any, OpenDALError, PresignedRequest] =
    ZIO.fail(UnsupportedOperationError("Presigned URLs not supported in this OpenDAL version"))

  // Info operations
  def info: ZIO[Any, OpenDALError, OperatorInfo] =
    ZIO.attempt(operator.info).mapError(OpenDALError.fromThrowable)

  def capabilities: ZIO[Any, OpenDALError, Capability] =
    info.map(_.fullCapability)
}
