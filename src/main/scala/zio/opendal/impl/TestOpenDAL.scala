package zio.opendal.impl

import zio._
import zio.opendal.core.OpenDAL
import zio.opendal.error._
import zio.opendal.options._
import zio.opendal.scheme.Scheme
import org.apache.opendal._

import scala.collection.concurrent.TrieMap
import java.time.Instant

/** Test/mock implementation for testing without native library dependencies */
final class TestOpenDAL(
  scheme: Scheme,
  @annotation.unused config: Map[String, String] = Map.empty
) extends OpenDAL {

  // Thread-safe in-memory storage
  private val storage = TrieMap[String, Array[Byte]]()

  // Basic operations
  def read(path: String): ZIO[Any, OpenDALError, Array[Byte]] =
    ZIO
      .fromOption(storage.get(path))
      .orElseFail(NotFoundError(s"File not found: $path"))

  def read(path: String, options: ReadOpts): ZIO[Any, OpenDALError, Array[Byte]] =
    read(path).map { data =>
      val start = options.offset.getOrElse(0L).toInt
      val end   = options.length.map(len => (start + len).toInt).getOrElse(data.length)
      data.slice(start, end)
    }

  def read(path: String, offset: Long, length: Long): ZIO[Any, OpenDALError, Array[Byte]] =
    read(path, ReadOpts(Some(offset), Some(length)))

  def write(path: String, data: Array[Byte]): ZIO[Any, OpenDALError, Unit] =
    ZIO.succeed(storage.put(path, data.clone())).unit

  def write(path: String, content: String): ZIO[Any, OpenDALError, Unit] =
    write(path, content.getBytes("UTF-8"))

  def write(path: String, data: Array[Byte], options: WriteOpts): ZIO[Any, OpenDALError, Unit] =
    write(path, data) // Ignore options in mock

  def write(path: String, content: String, options: WriteOpts): ZIO[Any, OpenDALError, Unit] =
    write(path, content.getBytes("UTF-8"), options)

  def stat(path: String): ZIO[Any, OpenDALError, Metadata] =
    ZIO
      .fromOption(storage.get(path))
      .map { data =>
        new Metadata(
          0,                          // mode
          data.length.toLong,         // content_length
          "application/octet-stream", // content_type
          path.hashCode.toString,     // etag
          "",                         // content_disposition
          "",                         // cache_control
          "",                         // content_encoding
          Instant.now(),              // last_modified
          ""                          // version
        )
      }
      .orElseFail(NotFoundError(s"File not found: $path"))

  def stat(path: String, options: StatOpts): ZIO[Any, OpenDALError, Metadata] =
    stat(path) // Ignore options in mock

  // File operations
  def delete(path: String): ZIO[Any, OpenDALError, Unit] =
    ZIO.succeed(storage.remove(path)).unit

  def copy(from: String, to: String): ZIO[Any, OpenDALError, Unit] =
    ZIO
      .fromOption(storage.get(from))
      .map(data => storage.put(to, data.clone()))
      .unit
      .orElseFail(NotFoundError(s"Source file not found: $from"))

  def rename(from: String, to: String): ZIO[Any, OpenDALError, Unit] =
    copy(from, to) *> delete(from)

  def exists(path: String): ZIO[Any, OpenDALError, Boolean] =
    ZIO.succeed(storage.contains(path))

  // Directory operations
  def createDir(path: String): ZIO[Any, OpenDALError, Unit] =
    ZIO.unit // No-op for mock

  def list(path: String): ZIO[Any, OpenDALError, List[Entry]] = {
    val mockMetadata = new Metadata(0, 0L, "application/octet-stream", "", "", "", "", Instant.now(), "")
    ZIO.succeed(
      storage.keys
        .filter(_.startsWith(path))
        .map(key => new Entry(key, mockMetadata))
        .toList
    )
  }

  def list(path: String, options: ListOpts): ZIO[Any, OpenDALError, List[Entry]] =
    list(path).map { entries =>
      val filtered = if (options.recursive) entries else entries.filter(!_.getPath.drop(path.length).contains("/"))
      options.limit.map(limit => filtered.take(limit.toInt)).getOrElse(filtered)
    }

  def removeAll(path: String): ZIO[Any, OpenDALError, Unit] =
    ZIO.succeed {
      storage.keys.filter(_.startsWith(path)).foreach(storage.remove)
    }.unit

  // Presigned operations (mock implementations)
  def presignRead(path: String, duration: Duration): ZIO[Any, OpenDALError, PresignedRequest] =
    ZIO.fail(UnsupportedOperationError("Presigned URLs not supported in test layer"))

  def presignWrite(path: String, duration: Duration): ZIO[Any, OpenDALError, PresignedRequest] =
    ZIO.fail(UnsupportedOperationError("Presigned URLs not supported in test layer"))

  def presignStat(path: String, duration: Duration): ZIO[Any, OpenDALError, PresignedRequest] =
    ZIO.fail(UnsupportedOperationError("Presigned URLs not supported in test layer"))

  // Info operations
  def info: ZIO[Any, OpenDALError, OperatorInfo] = {
    val mockCapability = new Capability(
      false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, 0L, 0L,
      0L, false, false, false, false, false, false, false, false, false, false, false, false, false, false, 0L, false
    )
    ZIO.succeed(new OperatorInfo(scheme.name, "", "", mockCapability, mockCapability))
  }

  def capabilities: ZIO[Any, OpenDALError, Capability] =
    ZIO.succeed(
      new Capability(
        false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, 0L, 0L,
        0L, false, false, false, false, false, false, false, false, false, false, false, false, false, false, 0L, false
      )
    )
}
