package zio.opendal.error

/** Base class for all OpenDAL-related errors */
sealed abstract class OpenDALError(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.getOrElse(null)) {

  /** Whether this error should be retried */
  def isRetriable: Boolean = this match {
    case _: NotFoundError             => false
    case _: UnauthorizedError         => false
    case _: InvalidConfigError        => false
    case _: UnsupportedOperationError => false
    case _: NetworkError              => true
    case _: TemporaryError            => true
    case _: UnknownError              => true
  }

  /** Error category for better error handling */
  def category: ErrorCategory = this match {
    case _: NotFoundError             => ErrorCategory.NotFound
    case _: UnauthorizedError         => ErrorCategory.Authentication
    case _: InvalidConfigError        => ErrorCategory.Configuration
    case _: UnsupportedOperationError => ErrorCategory.Unsupported
    case _: NetworkError              => ErrorCategory.Network
    case _: TemporaryError            => ErrorCategory.Temporary
    case _: UnknownError              => ErrorCategory.Unknown
  }
}

object OpenDALError {

  /** Create an OpenDAL error from a throwable */
  def fromThrowable(throwable: Throwable): OpenDALError = {
    val message = Option(throwable.getMessage).getOrElse(throwable.getClass.getSimpleName)

    // Categorize based on message content
    if (isNotFoundError(message)) {
      NotFoundError(message, Some(throwable))
    } else if (isNetworkError(message)) {
      NetworkError(message, Some(throwable))
    } else if (isTemporaryError(message)) {
      TemporaryError(message, Some(throwable))
    } else if (isUnauthorizedError(message)) {
      UnauthorizedError(message, Some(throwable))
    } else {
      UnknownError(message, Some(throwable))
    }
  }

  private def isNotFoundError(message: String): Boolean = {
    val lowerMessage = message.toLowerCase
    lowerMessage.contains("not found") ||
    lowerMessage.contains("does not exist") ||
    lowerMessage.contains("nosuchfile") ||
    lowerMessage.contains("notfound") ||
    lowerMessage.contains("404")
  }

  private def isNetworkError(message: String): Boolean = {
    val lowerMessage = message.toLowerCase
    lowerMessage.contains("connection") ||
    lowerMessage.contains("network") ||
    lowerMessage.contains("timeout") ||
    lowerMessage.contains("unreachable")
  }

  private def isTemporaryError(message: String): Boolean = {
    val lowerMessage = message.toLowerCase
    lowerMessage.contains("503") ||
    lowerMessage.contains("502") ||
    lowerMessage.contains("500") ||
    lowerMessage.contains("temporary")
  }

  private def isUnauthorizedError(message: String): Boolean = {
    val lowerMessage = message.toLowerCase
    lowerMessage.contains("unauthorized") ||
    lowerMessage.contains("forbidden") ||
    lowerMessage.contains("401") ||
    lowerMessage.contains("403") ||
    lowerMessage.contains("access denied")
  }
}

/** File or resource not found */
final case class NotFoundError(message: String, cause: Option[Throwable] = None) extends OpenDALError(message, cause)

/** Network-related errors */
final case class NetworkError(message: String, cause: Option[Throwable] = None) extends OpenDALError(message, cause)

/** Temporary/transient errors that can be retried */
final case class TemporaryError(message: String, cause: Option[Throwable] = None) extends OpenDALError(message, cause)

/** Authentication or authorization errors */
final case class UnauthorizedError(message: String, cause: Option[Throwable] = None)
    extends OpenDALError(message, cause)

/** Invalid configuration errors */
final case class InvalidConfigError(message: String, cause: Option[Throwable] = None)
    extends OpenDALError(message, cause)

/** Operation not supported by the backend */
final case class UnsupportedOperationError(message: String, cause: Option[Throwable] = None)
    extends OpenDALError(message, cause)

/** Unknown or uncategorized errors */
final case class UnknownError(message: String, cause: Option[Throwable] = None) extends OpenDALError(message, cause)

/** Error categories for better error handling */
sealed trait ErrorCategory extends Product with Serializable

object ErrorCategory {
  case object NotFound       extends ErrorCategory
  case object Authentication extends ErrorCategory
  case object Configuration  extends ErrorCategory
  case object Unsupported    extends ErrorCategory
  case object Network        extends ErrorCategory
  case object Temporary      extends ErrorCategory
  case object Unknown        extends ErrorCategory
}
