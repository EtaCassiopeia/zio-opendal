package zio.opendal.core

import zio._
import zio.opendal.error.OpenDALError
import zio.opendal.options._
import org.apache.opendal.Metadata

/** Basic read/write operations */
trait BasicOperations {

  // Read operations
  def read(path: String): ZIO[Any, OpenDALError, Array[Byte]]
  def read(path: String, options: ReadOpts): ZIO[Any, OpenDALError, Array[Byte]]
  def read(path: String, offset: Long, length: Long): ZIO[Any, OpenDALError, Array[Byte]]

  // Write operations
  def write(path: String, data: Array[Byte]): ZIO[Any, OpenDALError, Unit]
  def write(path: String, content: String): ZIO[Any, OpenDALError, Unit]
  def write(path: String, data: Array[Byte], options: WriteOpts): ZIO[Any, OpenDALError, Unit]
  def write(path: String, content: String, options: WriteOpts): ZIO[Any, OpenDALError, Unit]

  // Metadata operations
  def stat(path: String): ZIO[Any, OpenDALError, Metadata]
  def stat(path: String, options: StatOpts): ZIO[Any, OpenDALError, Metadata]

  // Delete operations
  def delete(path: String): ZIO[Any, OpenDALError, Unit]
}
