# API Reference

This is the complete API reference for ZIO OpenDAL. All operations are effect-based and integrate seamlessly with ZIO.

## Core Concepts

### OpenDAL Service

The main interface for all storage operations:

```scala
trait OpenDAL extends BasicOperations 
                with FileOperations 
                with DirectoryOperations 
                with PresignedOperations 
                with InfoOperations
```

### Layer Creation

```scala
object OpenDAL {
  // Create a live layer with real storage backend
  def live(config: LayerConfig): ZLayer[Any, OpenDALError, OpenDAL]
  
  // Create a test layer for development/testing  
  def testLayer(config: LayerConfig): ZLayer[Any, Nothing, OpenDAL]
  def testLayer(scheme: Scheme, config: Map[String, String] = Map.empty): ZLayer[Any, Nothing, OpenDAL]
}
```

## Basic Operations

### Reading Data

**Read entire file as bytes:**
```scala
def read(path: String): ZIO[OpenDAL, OpenDALError, Array[Byte]]
```

**Read with options:**
```scala
def read(path: String, options: ReadOpts): ZIO[OpenDAL, OpenDALError, Array[Byte]]
```

**Read byte range:**
```scala
def read(path: String, offset: Long, length: Long): ZIO[OpenDAL, OpenDALError, Array[Byte]]
```

**Read as string:**
```scala
def readString(path: String): ZIO[OpenDAL, OpenDALError, String]
def readString(path: String, options: ReadOpts): ZIO[OpenDAL, OpenDALError, String]
```

**Example:**
```scala
// Read entire file
content <- OpenDAL.readString("config.json")

// Read with range
partial <- OpenDAL.read("large-file.txt", ReadOpts.withRange(100, 50))

// Read as string with encoding
text <- OpenDAL.readString("document.txt", ReadOpts.withBufferSize(8192))
```

### Writing Data

**Write bytes:**
```scala
def write(path: String, data: Array[Byte]): ZIO[OpenDAL, OpenDALError, Unit]
```

**Write string:**
```scala
def write(path: String, content: String): ZIO[OpenDAL, OpenDALError, Unit]
```

**Write with options:**
```scala
def write(path: String, data: Array[Byte], options: WriteOpts): ZIO[OpenDAL, OpenDALError, Unit]
def write(path: String, content: String, options: WriteOpts): ZIO[OpenDAL, OpenDALError, Unit]
```

**Convenience methods:**
```scala
def writeText(path: String, content: String): ZIO[OpenDAL, OpenDALError, Unit]
def writeJson(path: String, content: String): ZIO[OpenDAL, OpenDALError, Unit]
```

**Example:**
```scala
// Write text
_ <- OpenDAL.writeText("greeting.txt", "Hello, World!")

// Write JSON with content type
_ <- OpenDAL.writeJson("config.json", """{"debug": true}""")

// Write with custom options
_ <- OpenDAL.write("image.jpg", imageBytes, WriteOpts.empty
  .withContentType("image/jpeg")
  .withCacheControl("max-age=3600")
)
```

### File Metadata

**Get metadata:**
```scala
def stat(path: String): ZIO[OpenDAL, OpenDALError, Metadata]
def stat(path: String, options: StatOpts): ZIO[OpenDAL, OpenDALError, Metadata]
```

**Check existence:**
```scala
def exists(path: String): ZIO[OpenDAL, OpenDALError, Boolean]
```

**Convenience methods:**
```scala
def size(path: String): ZIO[OpenDAL, OpenDALError, Long]
def existsAndNotEmpty(path: String): ZIO[OpenDAL, OpenDALError, Boolean]
```

**Example:**
```scala
// Get file metadata
metadata <- OpenDAL.stat("document.pdf")
size = metadata.getContentLength
contentType = metadata.getContentType
lastModified = metadata.getLastModified

// Check existence
exists <- OpenDAL.exists("backup.zip")

// Get size directly
fileSize <- OpenDAL.size("large-dataset.csv")
```

## File Operations

### Copy and Move

**Copy file:**
```scala
def copy(from: String, to: String): ZIO[OpenDAL, OpenDALError, Unit]
def copyAndVerify(from: String, to: String): ZIO[OpenDAL, OpenDALError, Unit]
```

**Rename/move file:**
```scala
def rename(from: String, to: String): ZIO[OpenDAL, OpenDALError, Unit]
```

**Example:**
```scala
// Copy file
_ <- OpenDAL.copy("original.txt", "backup.txt")

// Copy with verification
_ <- OpenDAL.copyAndVerify("important.doc", "important-backup.doc")

// Rename file
_ <- OpenDAL.rename("temp-file.txt", "final-file.txt")
```

### Delete Operations

**Delete single file:**
```scala
def delete(path: String): ZIO[OpenDAL, OpenDALError, Unit]
```

**Example:**
```scala
// Delete file
_ <- OpenDAL.delete("unwanted-file.txt")
```

## Directory Operations

### Listing Contents

**List directory:**
```scala
def list(path: String): ZIO[OpenDAL, OpenDALError, List[Entry]]
def list(path: String, options: ListOpts): ZIO[OpenDAL, OpenDALError, List[Entry]]
```

**Convenience methods:**
```scala
def listRecursive(path: String): ZIO[OpenDAL, OpenDALError, List[Entry]]
```

**Example:**
```scala
// List directory contents
files <- OpenDAL.list("documents/")

// List with options
limitedFiles <- OpenDAL.list("logs/", ListOpts.empty
  .withLimit(10)
  .withPrefix("error-")
)

// Recursive listing
allFiles <- OpenDAL.listRecursive("project/")
```

### Directory Management

**Create directory:**
```scala
def createDir(path: String): ZIO[OpenDAL, OpenDALError, Unit]
```

**Remove directory and contents:**
```scala
def removeAll(path: String): ZIO[OpenDAL, OpenDALError, Unit]
```

**Example:**
```scala
// Create directory structure
_ <- OpenDAL.createDir("uploads/images/")

// Remove entire directory tree
_ <- OpenDAL.removeAll("temp-data/")
```

## Presigned Operations

For storage backends that support presigned URLs:

**Generate presigned read URL:**
```scala
def presignRead(path: String, duration: Duration): ZIO[OpenDAL, OpenDALError, PresignedRequest]
```

**Generate presigned write URL:**
```scala  
def presignWrite(path: String, duration: Duration): ZIO[OpenDAL, OpenDALError, PresignedRequest]
```

**Generate presigned stat URL:**
```scala
def presignStat(path: String, duration: Duration): ZIO[OpenDAL, OpenDALError, PresignedRequest]
```

**Example:**
```scala
// Generate presigned download URL (valid for 1 hour)
downloadUrl <- OpenDAL.presignRead("private-document.pdf", 1.hour)

// Generate presigned upload URL
uploadUrl <- OpenDAL.presignWrite("user-uploads/image.jpg", 30.minutes)
```

## Info Operations

**Get operator information:**
```scala
def info: ZIO[OpenDAL, OpenDALError, OperatorInfo]
```

**Get backend capabilities:**
```scala
def capabilities: ZIO[OpenDAL, OpenDALError, Capability]
```

**Example:**
```scala
// Get storage backend info
operatorInfo <- OpenDAL.info
scheme = operatorInfo.getScheme
name = operatorInfo.getName

// Check capabilities
caps <- OpenDAL.capabilities
canWrite = caps.write()
canList = caps.list()
```

## Options Classes

### ReadOpts

Configure read operations:

```scala
case class ReadOpts(
  offset: Option[Long] = None,
  length: Option[Long] = None, 
  bufferSize: Option[Int] = None
)
```

**Builder methods:**
```scala
def withOffset(offset: Long): ReadOpts
def withLength(length: Long): ReadOpts
def withBufferSize(size: Int): ReadOpts
def withRange(offset: Long, length: Long): ReadOpts
```

**Factory methods:**
```scala
object ReadOpts {
  val empty: ReadOpts
  def withOffset(offset: Long): ReadOpts
  def withLength(length: Long): ReadOpts  
  def withRange(offset: Long, length: Long): ReadOpts
}
```

### WriteOpts

Configure write operations:

```scala
case class WriteOpts(
  contentType: Option[String] = None,
  cacheControl: Option[String] = None,
  contentDisposition: Option[String] = None,
  contentEncoding: Option[String] = None,
  metadata: Map[String, String] = Map.empty
)
```

**Builder methods:**
```scala
def withContentType(contentType: String): WriteOpts
def withCacheControl(cacheControl: String): WriteOpts
def withContentDisposition(disposition: String): WriteOpts
def withContentEncoding(encoding: String): WriteOpts
def withMetadata(metadata: Map[String, String]): WriteOpts
def withMetadata(key: String, value: String): WriteOpts
```

**Factory methods:**
```scala
object WriteOpts {
  val empty: WriteOpts
  val json: WriteOpts  // Content-Type: application/json
  val text: WriteOpts  // Content-Type: text/plain
  val binary: WriteOpts // Content-Type: application/octet-stream
}
```

### ListOpts

Configure listing operations:

```scala
case class ListOpts(
  recursive: Boolean = false,
  limit: Option[Long] = None,
  prefix: Option[String] = None
)
```

**Builder methods:**
```scala
def recursive: ListOpts
def withLimit(limit: Long): ListOpts
def withPrefix(prefix: String): ListOpts
```

**Factory methods:**
```scala
object ListOpts {
  val empty: ListOpts
  val recursive: ListOpts
  def withLimit(limit: Long): ListOpts
  def withPrefix(prefix: String): ListOpts
}
```

### StatOpts

Configure stat operations:

```scala
case class StatOpts(
  checksum: Boolean = false,
  version: Boolean = false
)
```

**Builder methods:**
```scala
def withChecksum(enabled: Boolean = true): StatOpts
def withVersion(enabled: Boolean = true): StatOpts
```

## Error Types

ZIO OpenDAL provides comprehensive error types:

### Base Error Type

```scala
sealed abstract class OpenDALError(message: String, cause: Option[Throwable] = None)
  extends Exception(message, cause.getOrElse(null))
```

**Properties:**
```scala
def isRetriable: Boolean  // Whether error should be retried
def category: ErrorCategory  // Error category for handling
```

### Specific Error Types

**NotFoundError:**
```scala
final case class NotFoundError(message: String, cause: Option[Throwable] = None) 
  extends OpenDALError(message, cause)
```

**NetworkError:**
```scala
final case class NetworkError(message: String, cause: Option[Throwable] = None) 
  extends OpenDALError(message, cause)
```

**TemporaryError:**
```scala
final case class TemporaryError(message: String, cause: Option[Throwable] = None) 
  extends OpenDALError(message, cause)
```

**UnauthorizedError:**
```scala
final case class UnauthorizedError(message: String, cause: Option[Throwable] = None) 
  extends OpenDALError(message, cause)
```

**InvalidConfigError:**
```scala
final case class InvalidConfigError(message: String, cause: Option[Throwable] = None) 
  extends OpenDALError(message, cause)
```

**UnsupportedOperationError:**
```scala
final case class UnsupportedOperationError(message: String, cause: Option[Throwable] = None) 
  extends OpenDALError(message, cause)
```

**UnknownError:**
```scala
final case class UnknownError(message: String, cause: Option[Throwable] = None) 
  extends OpenDALError(message, cause)
```

### Error Categories

```scala
sealed trait ErrorCategory
object ErrorCategory {
  case object NotFound extends ErrorCategory
  case object Authentication extends ErrorCategory  
  case object Configuration extends ErrorCategory
  case object Unsupported extends ErrorCategory
  case object Network extends ErrorCategory
  case object Temporary extends ErrorCategory
  case object Unknown extends ErrorCategory
}
```

### Error Handling Examples

```scala
val program = for {
  content <- OpenDAL.readString("file.txt")
  _ <- Console.printLine(s"Content: $content")
} yield ()

// Handle specific errors
val safeProgram = program.catchSome {
  case NotFoundError(msg, _) =>
    Console.printLine(s"File not found: $msg") *>
    ZIO.succeed("default content")
  case NetworkError(msg, _) =>
    Console.printLine(s"Network issue: $msg") *>
    ZIO.sleep(1.second) *> program // Retry
  case UnauthorizedError(msg, _) =>
    ZIO.fail(new SecurityException(s"Access denied: $msg"))
}

// Handle by category
val categoricalHandling = program.catchAll { error =>
  error.category match {
    case ErrorCategory.Network | ErrorCategory.Temporary =>
      Console.printLine("Temporary issue, retrying...") *> 
      ZIO.sleep(5.seconds) *> program
    case ErrorCategory.Authentication =>
      Console.printLine("Authentication required") *>
      ZIO.fail(error)
    case _ =>
      Console.printLine(s"Unhandled error: ${error.getMessage}") *>
      ZIO.fail(error)
  }
}

// Retry retriable errors automatically
val resilientProgram = program.retry(
  Schedule.recurWhile[OpenDALError](_.isRetriable) &&
  Schedule.exponential(100.millis) && 
  Schedule.recurs(3)
)
```

## Syntax Extensions

ZIO OpenDAL provides syntax extensions for more ergonomic usage:

### String Extensions

```scala
import zio.opendal.syntax._

// Write string to file
_ <- "Hello, World!".writeTo("greeting.txt")
_ <- "Content".writeTextTo("file.txt")
_ <- """{"key": "value"}""".writeJsonTo("config.json")
```

### Path Extensions

```scala
import zio.opendal.syntax._

// Read from path
content <- "greeting.txt".readString
bytes <- "file.dat".readBytes
partial <- "large.txt".readBytes(ReadOpts.withRange(100, 50))

// File operations
exists <- "file.txt".exists
metadata <- "file.txt".stat
_ <- "source.txt".copyTo("dest.txt")
_ <- "old.txt".renameTo("new.txt")
_ <- "unwanted.txt".delete

// Directory operations
files <- "directory/".list
_ <- "directory/".removeAll
```

### Byte Array Extensions

```scala
import zio.opendal.syntax._

val bytes = "content".getBytes("UTF-8")
_ <- bytes.writeTo("file.bin")
text = bytes.asUtf8String
```

### Metadata Extensions

```scala
import zio.opendal.syntax._

metadata <- OpenDAL.stat("file.txt")
size = metadata.contentLength
readable = metadata.humanReadableSize  // "1.5MB"
empty = metadata.isEmpty
```

### Entry Extensions

```scala
import zio.opendal.syntax._

entries <- OpenDAL.list("directory/")
names = entries.map(_.name)  // Extract filename from path
dirs = entries.filter(_.isDirectory)
files = entries.filter(_.isFile)
```

## Configuration API

### LayerConfig

Main configuration class:

```scala
case class LayerConfig(
  scheme: Scheme,
  config: Map[String, String] = Map.empty,
  retrySchedule: Schedule[Any, OpenDALError, Any] = defaultRetrySchedule,
  enableNativeAccess: Boolean = true,
  platformClassifier: Option[String] = None
)
```

**Builder methods:**
```scala
def withConfig(key: String, value: String): LayerConfig
def withConfig(entries: Map[String, String]): LayerConfig  
def withRetrySchedule(schedule: Schedule[Any, OpenDALError, Any]): LayerConfig
def withoutRetry: LayerConfig
def withNativeAccess(enabled: Boolean): LayerConfig
def withPlatformClassifier(classifier: String): LayerConfig
def validate: ZIO[Any, InvalidConfigError, Unit]
```

**Factory methods:**
```scala
object LayerConfig {
  def filesystem(root: String): LayerConfig
  def memory(): LayerConfig
  def s3(bucket: String, region: String, accessKeyId: String, secretAccessKey: String): LayerConfig
  def s3Compatible(bucket: String, region: String, accessKeyId: String, secretAccessKey: String, endpoint: String): LayerConfig
  def azureBlob(container: String, accountName: String, accountKey: String): LayerConfig
  def gcs(bucket: String, serviceAccount: String, projectId: String): LayerConfig
  def autoDetectPlatform: String
}
```

## Integration Patterns

### ZIO Layers

**Simple layer:**
```scala
val openDALLayer = OpenDAL.live(LayerConfig.memory())

val program = for {
  _ <- OpenDAL.writeText("file.txt", "content")
  content <- OpenDAL.readString("file.txt")
  _ <- Console.printLine(content)
} yield ()

program.provide(openDALLayer)
```

**Composed layers:**
```scala
val configLayer = ZLayer.fromZIO(
  for {
    bucket <- System.env("S3_BUCKET")
    region <- System.env("AWS_REGION")
  } yield LayerConfig.s3(bucket.get, region.get, "...", "...")
)

val openDALLayer = configLayer >>> ZLayer.fromFunction(OpenDAL.live)

val appLayer = openDALLayer ++ Console.live

program.provide(appLayer)
```

### Testing

**Test specifications:**
```scala
import zio.test._

object MyServiceSpec extends ZIOSpecDefault {
  
  val testLayer = OpenDAL.testLayer(LayerConfig.memory())
  
  def spec = suite("MyService")(
    test("should handle file operations") {
      for {
        _ <- OpenDAL.writeText("test.txt", "test content")
        content <- OpenDAL.readString("test.txt")  
        exists <- OpenDAL.exists("test.txt")
      } yield assertTrue(
        content == "test content" &&
        exists == true
      )
    }
  ).provide(testLayer)
}
```

### Resource Management

**Automatic resource management:**
```scala
// Resources are managed by ZIO's layer system
val program = ZIO.scoped {
  for {
    _ <- OpenDAL.writeText("temp.txt", "temporary")
    content <- OpenDAL.readString("temp.txt")
    // File operations...
  } yield content
} // Resources automatically cleaned up
```

**Manual resource management:**
```scala
val safeProgram = (for {
  _ <- OpenDAL.writeText("temp.txt", "data")
  result <- processFile("temp.txt")
} yield result).ensuring(
  OpenDAL.delete("temp.txt").ignore // Cleanup guaranteed
)
```

This completes the API reference. For working examples and patterns, see the [examples documentation](examples.md).
