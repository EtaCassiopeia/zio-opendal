package zio.opendal.scheme

import CloudStorage._
import LocalStorage._

/** Configuration keys and helpers for different storage schemes */
object SchemeConfig {

  /** Get common configuration keys for a given scheme */
  def configKeysFor(scheme: Scheme): Seq[String] = scheme match {
    // Cloud storage
    case S3     => Seq("bucket", "region", "access_key_id", "secret_access_key", "endpoint", "root")
    case Azblob => Seq("container", "account_name", "account_key", "endpoint", "root")
    case Gcs    => Seq("bucket", "service_account", "project_id", "root")
    case Cos    => Seq("bucket", "region", "secret_id", "secret_key", "endpoint", "root")
    case Oss    => Seq("bucket", "region", "access_key_id", "access_key_secret", "endpoint", "root")

    // Local storage
    case Fs     => Seq("root")
    case Memory => Seq.empty

    // Default for other schemes
    case _ => Seq.empty
  }

  /** Check if a scheme requires authentication */
  def requiresAuth(scheme: Scheme): Boolean = scheme match {
    case Memory | Fs => false
    case _           => true
  }

  /** Check if a scheme supports presigned URLs */
  def supportsPresignedUrls(scheme: Scheme): Boolean = scheme match {
    case S3 | Azblob | Gcs | Cos | Oss => true
    case _                             => false
  }

  /** Get scheme category for better organization */
  def categoryOf(scheme: Scheme): String = scheme match {
    case S3 | Azblob | Azdls | Azfile | Gcs | Cos | Oss | Obs | Swift | B2 => "cloud"
    case Fs | Memory | Dashmap | MiniMoka | Moka                           => "local"
    case scheme
        if Database.getClass.getDeclaredFields.exists(_.getName == scheme.getClass.getSimpleName.replace("$", "")) =>
      "database"
    case _                                                                 => "specialized"
  }
}
