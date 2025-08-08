package zio.opendal.config

import zio._
import zio.opendal.scheme.Scheme
import zio.opendal.error.{InvalidConfigError, OpenDALError}

/** Configuration for creating OpenDAL layers with validation */
final case class LayerConfig(
  scheme: Scheme,
  config: Map[String, String] = Map.empty,
  retrySchedule: Schedule[Any, OpenDALError, Any] = Schedule.exponential(100.millis) && Schedule.recurs(3),
  enableNativeAccess: Boolean = true,
  platformClassifier: Option[String] = None
) {

  /** Add a configuration key-value pair */
  def withConfig(key: String, value: String): LayerConfig =
    copy(config = config + (key -> value))

  /** Add multiple configuration entries */
  def withConfig(entries: Map[String, String]): LayerConfig =
    copy(config = config ++ entries)

  /** Set custom retry schedule */
  def withRetrySchedule(schedule: Schedule[Any, OpenDALError, Any]): LayerConfig =
    copy(retrySchedule = schedule)

  /** Disable retry */
  def withoutRetry: LayerConfig =
    copy(retrySchedule = Schedule.stop)

  /** Enable/disable native access */
  def withNativeAccess(enabled: Boolean): LayerConfig =
    copy(enableNativeAccess = enabled)

  /** Set platform classifier explicitly */
  def withPlatformClassifier(classifier: String): LayerConfig =
    copy(platformClassifier = Some(classifier))

  /** Validate the configuration */
  def validate: ZIO[Any, InvalidConfigError, Unit] =
    ZIO.attempt {
      // Basic validation logic
      scheme.name match {
        case "s3" =>
          val requiredKeys = Set("bucket", "region")
          val missing      = requiredKeys -- config.keySet
          if (missing.nonEmpty) {
            throw new IllegalArgumentException(s"Missing required S3 config keys: ${missing.mkString(", ")}")
          }
        case "fs" =>
          if (!config.contains("root")) {
            throw new IllegalArgumentException("Missing required 'root' config key for filesystem backend")
          }
        case _    => // No specific validation for other schemes yet
      }
    }.mapError(t => InvalidConfigError(s"Configuration validation failed: ${t.getMessage}", Some(t)))
}

object LayerConfig {

  /** Create configuration for filesystem backend */
  def filesystem(root: String): LayerConfig =
    LayerConfig(
      scheme = zio.opendal.scheme.LocalStorage.Fs,
      config = Map("root" -> root)
    )

  /** Create configuration for in-memory backend */
  def memory(): LayerConfig =
    LayerConfig(scheme = zio.opendal.scheme.LocalStorage.Memory)

  /** Create configuration for S3 backend */
  def s3(bucket: String, region: String, accessKeyId: String, secretAccessKey: String): LayerConfig =
    LayerConfig(
      scheme = zio.opendal.scheme.CloudStorage.S3,
      config = Map(
        "bucket"            -> bucket,
        "region"            -> region,
        "access_key_id"     -> accessKeyId,
        "secret_access_key" -> secretAccessKey
      )
    )

  /** Create configuration for S3-compatible backend with custom endpoint */
  def s3Compatible(
    bucket: String,
    region: String,
    accessKeyId: String,
    secretAccessKey: String,
    endpoint: String
  ): LayerConfig =
    s3(bucket, region, accessKeyId, secretAccessKey).withConfig("endpoint", endpoint)

  /** Create configuration for Azure Blob Storage */
  def azureBlob(container: String, accountName: String, accountKey: String): LayerConfig =
    LayerConfig(
      scheme = zio.opendal.scheme.CloudStorage.Azblob,
      config = Map(
        "container"    -> container,
        "account_name" -> accountName,
        "account_key"  -> accountKey
      )
    )

  /** Create configuration for Google Cloud Storage */
  def gcs(bucket: String, serviceAccount: String, projectId: String): LayerConfig =
    LayerConfig(
      scheme = zio.opendal.scheme.CloudStorage.Gcs,
      config = Map(
        "bucket"          -> bucket,
        "service_account" -> serviceAccount,
        "project_id"      -> projectId
      )
    )

  /** Auto-detect platform classifier */
  def autoDetectPlatform: String = {
    val osName = java.lang.System.getProperty("os.name").toLowerCase
    val osArch = java.lang.System.getProperty("os.arch")

    if (osName.contains("mac")) {
      if (osArch == "aarch64" || osArch == "arm64") "osx-aarch_64"
      else "osx-x86_64"
    } else if (osName.contains("linux")) {
      if (osArch == "aarch64" || osArch == "arm64") "linux-aarch_64"
      else "linux-x86_64"
    } else if (osName.contains("windows")) {
      "windows-x86_64"
    } else {
      "linux-x86_64" // fallback
    }
  }
}
