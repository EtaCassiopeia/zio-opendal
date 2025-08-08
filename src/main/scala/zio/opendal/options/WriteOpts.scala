package zio.opendal.options

/** Options for write operations with builder pattern support */
final case class WriteOpts(
  contentType: Option[String] = None,
  contentDisposition: Option[String] = None,
  cacheControl: Option[String] = None,
  contentEncoding: Option[String] = None,
  metadata: Map[String, String] = Map.empty,
  overwrite: Boolean = true
) {

  /** Set the content type */
  def withContentType(contentType: String): WriteOpts = copy(contentType = Some(contentType))

  /** Set the content disposition */
  def withContentDisposition(disposition: String): WriteOpts = copy(contentDisposition = Some(disposition))

  /** Set the cache control header */
  def withCacheControl(cacheControl: String): WriteOpts = copy(cacheControl = Some(cacheControl))

  /** Set the content encoding */
  def withContentEncoding(encoding: String): WriteOpts = copy(contentEncoding = Some(encoding))

  /** Add metadata key-value pair */
  def withMetadata(key: String, value: String): WriteOpts =
    copy(metadata = metadata + (key -> value))

  /** Set all metadata at once */
  def withMetadata(metadata: Map[String, String]): WriteOpts = copy(metadata = metadata)

  /** Set whether to overwrite existing files */
  def withOverwrite(overwrite: Boolean): WriteOpts = copy(overwrite = overwrite)

  /** Convenience method for common content types */
  def asJson: WriteOpts   = withContentType("application/json")
  def asText: WriteOpts   = withContentType("text/plain")
  def asHtml: WriteOpts   = withContentType("text/html")
  def asBinary: WriteOpts = withContentType("application/octet-stream")
}

object WriteOpts {

  /** Empty write options */
  val empty: WriteOpts = WriteOpts()

  /** Write options for JSON content */
  val json: WriteOpts = WriteOpts(contentType = Some("application/json"))

  /** Write options for text content */
  val text: WriteOpts = WriteOpts(contentType = Some("text/plain"))

  /** Write options for HTML content */
  val html: WriteOpts = WriteOpts(contentType = Some("text/html"))

  /** Write options for binary content */
  val binary: WriteOpts = WriteOpts(contentType = Some("application/octet-stream"))
}
