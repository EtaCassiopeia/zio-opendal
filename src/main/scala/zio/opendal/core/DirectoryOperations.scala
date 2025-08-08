package zio.opendal.core

import zio._
import zio.opendal.error.OpenDALError
import zio.opendal.options._
import org.apache.opendal.Entry

/** Directory management operations */
trait DirectoryOperations {

  /** Create a directory */
  def createDir(path: String): ZIO[Any, OpenDALError, Unit]

  /** List entries in a directory */
  def list(path: String): ZIO[Any, OpenDALError, List[Entry]]

  /** List entries with options */
  def list(path: String, options: ListOpts): ZIO[Any, OpenDALError, List[Entry]]

  /** Remove all files and directories under a path */
  def removeAll(path: String): ZIO[Any, OpenDALError, Unit]
}
