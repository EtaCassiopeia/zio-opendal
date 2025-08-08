package zio.opendal.options

/** Options for stat operations with builder pattern support */
final case class StatOpts(
  checksum: Boolean = false,
  includeMetadata: Boolean = true,
  version: Boolean = false
) {

  /** Enable checksum calculation */
  def withChecksum: StatOpts = copy(checksum = true)

  /** Disable checksum calculation */
  def withoutChecksum: StatOpts = copy(checksum = false)

  /** Include metadata in stat result */
  def withMetadata: StatOpts = copy(includeMetadata = true)

  /** Exclude metadata from stat result */
  def withoutMetadata: StatOpts = copy(includeMetadata = false)

  /** Include version information */
  def withVersion: StatOpts = copy(version = true)

  /** Exclude version information */
  def withoutVersion: StatOpts = copy(version = false)
}

object StatOpts {

  /** Empty stat options */
  val empty: StatOpts = StatOpts()

  /** Stat options with checksum */
  val withChecksum: StatOpts = StatOpts(checksum = true)

  /** Minimal stat options (no checksum, no metadata) */
  val minimal: StatOpts = StatOpts(checksum = false, includeMetadata = false)

  /** Full stat options (checksum, metadata, version) */
  val full: StatOpts = StatOpts(checksum = true, includeMetadata = true, version = true)
}
