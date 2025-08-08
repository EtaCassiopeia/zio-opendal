package zio.opendal

import zio._
import zio.opendal.core.OpenDAL
import zio.opendal.error.OpenDALError
import zio.opendal.options._
import org.apache.opendal.{Entry, Metadata}
import java.nio.charset.StandardCharsets

package object syntax {

  /** String extensions for easier content writing */
  implicit class StringOps(private val str: String) extends AnyVal {

    /** Write string content to a path */
    def writeTo(path: String): ZIO[OpenDAL, OpenDALError, Unit] =
      ZIO.serviceWithZIO[OpenDAL](_.write(path, str))

    /** Write string content to a path with options */
    def writeTo(path: String, options: WriteOpts): ZIO[OpenDAL, OpenDALError, Unit] =
      ZIO.serviceWithZIO[OpenDAL](_.write(path, str, options))

    /** Write string as JSON */
    def writeJsonTo(path: String): ZIO[OpenDAL, OpenDALError, Unit] =
      writeTo(path, WriteOpts.json)

    /** Write string as text */
    def writeTextTo(path: String): ZIO[OpenDAL, OpenDALError, Unit] =
      writeTo(path, WriteOpts.text)
  }

  /** Byte array extensions */
  implicit class ByteArrayOps(private val bytes: Array[Byte]) extends AnyVal {

    /** Write bytes to a path */
    def writeTo(path: String): ZIO[OpenDAL, OpenDALError, Unit] =
      ZIO.serviceWithZIO[OpenDAL](_.write(path, bytes))

    /** Write bytes to a path with options */
    def writeTo(path: String, options: WriteOpts): ZIO[OpenDAL, OpenDALError, Unit] =
      ZIO.serviceWithZIO[OpenDAL](_.write(path, bytes, options))

    /** Convert bytes to string using UTF-8 */
    def asUtf8String: String = new String(bytes, StandardCharsets.UTF_8)
  }

  /** Path string extensions for easier operations */
  implicit class PathOps(private val path: String) extends AnyVal {

    /** Read content from path as bytes */
    def readBytes: ZIO[OpenDAL, OpenDALError, Array[Byte]] =
      ZIO.serviceWithZIO[OpenDAL](_.read(path))

    /** Read content from path as string */
    def readString: ZIO[OpenDAL, OpenDALError, String] =
      readBytes.map(new String(_, StandardCharsets.UTF_8))

    /** Read with options */
    def readBytes(options: ReadOpts): ZIO[OpenDAL, OpenDALError, Array[Byte]] =
      ZIO.serviceWithZIO[OpenDAL](_.read(path, options))

    /** Read partial content */
    def readBytes(offset: Long, length: Long): ZIO[OpenDAL, OpenDALError, Array[Byte]] =
      ZIO.serviceWithZIO[OpenDAL](_.read(path, offset, length))

    /** Get file metadata */
    def stat: ZIO[OpenDAL, OpenDALError, Metadata] =
      ZIO.serviceWithZIO[OpenDAL](_.stat(path))

    /** Get file metadata with options */
    def stat(options: StatOpts): ZIO[OpenDAL, OpenDALError, Metadata] =
      ZIO.serviceWithZIO[OpenDAL](_.stat(path, options))

    /** Check if path exists */
    def exists: ZIO[OpenDAL, OpenDALError, Boolean] =
      ZIO.serviceWithZIO[OpenDAL](_.exists(path))

    /** Delete the file */
    def delete: ZIO[OpenDAL, OpenDALError, Unit] =
      ZIO.serviceWithZIO[OpenDAL](_.delete(path))

    /** Copy to another path */
    def copyTo(destination: String): ZIO[OpenDAL, OpenDALError, Unit] =
      ZIO.serviceWithZIO[OpenDAL](_.copy(path, destination))

    /** Rename/move to another path */
    def renameTo(destination: String): ZIO[OpenDAL, OpenDALError, Unit] =
      ZIO.serviceWithZIO[OpenDAL](_.rename(path, destination))

    /** List directory contents */
    def list: ZIO[OpenDAL, OpenDALError, List[Entry]] =
      ZIO.serviceWithZIO[OpenDAL](_.list(path))

    /** List directory contents with options */
    def list(options: ListOpts): ZIO[OpenDAL, OpenDALError, List[Entry]] =
      ZIO.serviceWithZIO[OpenDAL](_.list(path, options))

    /** Create directory */
    def createDir: ZIO[OpenDAL, OpenDALError, Unit] =
      ZIO.serviceWithZIO[OpenDAL](_.createDir(path))

    /** Remove all contents recursively */
    def removeAll: ZIO[OpenDAL, OpenDALError, Unit] =
      ZIO.serviceWithZIO[OpenDAL](_.removeAll(path))
  }

  /** Duration extensions for presigned operations */
  implicit class DurationOps(private val duration: Duration) extends AnyVal {

    /** Generate presigned read URL */
    def presignRead(path: String): ZIO[OpenDAL, OpenDALError, org.apache.opendal.PresignedRequest] =
      ZIO.serviceWithZIO[OpenDAL](_.presignRead(path, duration))

    /** Generate presigned write URL */
    def presignWrite(path: String): ZIO[OpenDAL, OpenDALError, org.apache.opendal.PresignedRequest] =
      ZIO.serviceWithZIO[OpenDAL](_.presignWrite(path, duration))

    /** Generate presigned stat URL */
    def presignStat(path: String): ZIO[OpenDAL, OpenDALError, org.apache.opendal.PresignedRequest] =
      ZIO.serviceWithZIO[OpenDAL](_.presignStat(path, duration))
  }

  /** Metadata extensions */
  implicit class MetadataOps(private val metadata: Metadata) extends AnyVal {

    /** Get content length safely */
    def contentLength: Long = metadata.getContentLength

    /** Get content type safely */
    def contentType: String = Option(metadata.getContentType).getOrElse("application/octet-stream")

    /** Get ETag safely */
    def etag: Option[String] = Option(metadata.getEtag).filter(_.nonEmpty)

    /** Get last modified time safely */
    def lastModified: Option[java.time.Instant] = Option(metadata.getLastModified)

    /** Check if file is empty */
    def isEmpty: Boolean = contentLength == 0

    /** Human readable size */
    def humanReadableSize: String = {
      val size = contentLength
      if (size < 1024) s"${size}B"
      else if (size < 1024 * 1024) f"${size / 1024.0}%.1fKB"
      else if (size < 1024 * 1024 * 1024) f"${size / (1024.0 * 1024)}%.1fMB"
      else f"${size / (1024.0 * 1024 * 1024)}%.1fGB"
    }
  }

  /** Entry extensions */
  implicit class EntryOps(private val entry: Entry) extends AnyVal {

    /** Get entry path */
    def path: String = entry.getPath

    /** Get entry metadata */
    def metadata: Metadata = entry.getMetadata

    /** Get entry name (last part of path) */
    def name: String = {
      val p         = path
      val lastSlash = p.lastIndexOf('/')
      if (lastSlash >= 0) p.substring(lastSlash + 1) else p
    }

    /** Check if entry is a directory */
    def isDirectory: Boolean = path.endsWith("/")

    /** Check if entry is a file */
    def isFile: Boolean = !isDirectory
  }
}
