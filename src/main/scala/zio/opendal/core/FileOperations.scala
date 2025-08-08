package zio.opendal.core

import zio._
import zio.opendal.error.OpenDALError

/** File management operations */
trait FileOperations {

  /** Copy a file from source to destination */
  def copy(from: String, to: String): ZIO[Any, OpenDALError, Unit]

  /** Move/rename a file from source to destination */
  def rename(from: String, to: String): ZIO[Any, OpenDALError, Unit]

  /** Check if a file exists */
  def exists(path: String): ZIO[Any, OpenDALError, Boolean]
}
