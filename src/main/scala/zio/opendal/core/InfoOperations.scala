package zio.opendal.core

import zio._
import zio.opendal.error.OpenDALError
import org.apache.opendal.{Capability, OperatorInfo}

/** Backend information operations */
trait InfoOperations {

  /** Get operator information */
  def info: ZIO[Any, OpenDALError, OperatorInfo]

  /** Get backend capabilities */
  def capabilities: ZIO[Any, OpenDALError, Capability]
}
