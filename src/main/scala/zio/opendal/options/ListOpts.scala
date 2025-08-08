package zio.opendal.options

/** Options for list operations with builder pattern support */
final case class ListOpts(
  recursive: Boolean = false,
  limit: Option[Long] = None,
  delimiter: Option[String] = None,
  prefix: Option[String] = None,
  startAfter: Option[String] = None
) {

  /** Enable recursive listing */
  def withRecursive: ListOpts = copy(recursive = true)

  /** Disable recursive listing */
  def nonRecursive: ListOpts = copy(recursive = false)

  /** Set maximum number of entries to return */
  def withLimit(limit: Long): ListOpts = copy(limit = Some(limit))

  /** Set delimiter for grouping */
  def withDelimiter(delimiter: String): ListOpts = copy(delimiter = Some(delimiter))

  /** Set prefix filter */
  def withPrefix(prefix: String): ListOpts = copy(prefix = Some(prefix))

  /** Set start after marker for pagination */
  def withStartAfter(marker: String): ListOpts = copy(startAfter = Some(marker))
}

object ListOpts {

  /** Empty list options (non-recursive, no limits) */
  val empty: ListOpts = ListOpts()

  /** Recursive listing */
  val recursive: ListOpts = ListOpts(recursive = true)

  /** Create list options with limit */
  def withLimit(limit: Long): ListOpts = ListOpts(limit = Some(limit))

  /** Create list options with prefix */
  def withPrefix(prefix: String): ListOpts = ListOpts(prefix = Some(prefix))

  /** Create recursive list options with limit */
  def recursiveWithLimit(limit: Long): ListOpts =
    ListOpts(recursive = true, limit = Some(limit))
}
