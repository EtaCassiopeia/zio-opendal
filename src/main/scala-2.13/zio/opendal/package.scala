package zio

import zio.opendal.scheme._
import zio.opendal.error._

/**
 * ZIO OpenDAL package object providing convenient imports and aliases
 */
package object opendal {

  // Type aliases for better ergonomics
  type OpenDALError = zio.opendal.error.OpenDALError
  type LayerConfig  = zio.opendal.config.LayerConfig

  // Expose commonly used scheme objects
  val S3     = CloudStorage.S3
  val Azblob = CloudStorage.Azblob
  val Gcs    = CloudStorage.Gcs
  val Fs     = LocalStorage.Fs
  val Memory = LocalStorage.Memory

  // Expose option objects for easy access
  val ReadOpts  = zio.opendal.options.ReadOpts
  val WriteOpts = zio.opendal.options.WriteOpts
  val ListOpts  = zio.opendal.options.ListOpts
  val StatOpts  = zio.opendal.options.StatOpts

  // Expose LayerConfig factory methods
  val LayerConfig = zio.opendal.config.LayerConfig

  // Error constructors for common error types
  def notFound(message: String, cause: Option[Throwable] = None): NotFoundError =
    NotFoundError(message, cause)

  def networkError(message: String, cause: Option[Throwable] = None): NetworkError =
    NetworkError(message, cause)

  def unauthorized(message: String, cause: Option[Throwable] = None): UnauthorizedError =
    UnauthorizedError(message, cause)

  def unsupported(message: String, cause: Option[Throwable] = None): UnsupportedOperationError =
    UnsupportedOperationError(message, cause)
}
