package zio.opendal.options

/** Options for read operations with builder pattern support */
final case class ReadOpts(
  offset: Option[Long] = None,
  length: Option[Long] = None,
  bufferSize: Option[Int] = None
) {

  /** Set the offset to start reading from */
  def withOffset(offset: Long): ReadOpts = copy(offset = Some(offset))

  /** Set the maximum number of bytes to read */
  def withLength(length: Long): ReadOpts = copy(length = Some(length))

  /** Set the buffer size for reading */
  def withBufferSize(size: Int): ReadOpts = copy(bufferSize = Some(size))

  /** Set offset and length together for convenience */
  def withRange(offset: Long, length: Long): ReadOpts =
    copy(offset = Some(offset), length = Some(length))
}

object ReadOpts {

  /** Empty read options */
  val empty: ReadOpts = ReadOpts()

  /** Create read options with offset */
  def withOffset(offset: Long): ReadOpts = ReadOpts(offset = Some(offset))

  /** Create read options with length */
  def withLength(length: Long): ReadOpts = ReadOpts(length = Some(length))

  /** Create read options with range */
  def withRange(offset: Long, length: Long): ReadOpts =
    ReadOpts(offset = Some(offset), length = Some(length))
}
