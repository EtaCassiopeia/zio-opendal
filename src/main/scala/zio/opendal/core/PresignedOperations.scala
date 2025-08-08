package zio.opendal.core

import zio._
import zio.opendal.error.OpenDALError
import zio.Duration
import org.apache.opendal.PresignedRequest

/** Presigned URL operations (for supported backends) */
trait PresignedOperations {

  /** Generate presigned URL for reading a file */
  def presignRead(path: String, duration: Duration): ZIO[Any, OpenDALError, PresignedRequest]

  /** Generate presigned URL for writing a file */
  def presignWrite(path: String, duration: Duration): ZIO[Any, OpenDALError, PresignedRequest]

  /** Generate presigned URL for getting file metadata */
  def presignStat(path: String, duration: Duration): ZIO[Any, OpenDALError, PresignedRequest]
}
