# Getting Started with ZIO OpenDAL

Welcome to ZIO OpenDAL! This guide will help you get up and running with unified storage access using ZIO effects.

## What is ZIO OpenDAL?

ZIO OpenDAL is a Scala wrapper for [Apache OpenDAL](https://opendal.apache.org/), providing:

- **Unified API**: Access 40+ storage services through a single interface
- **ZIO Integration**: Native ZIO effects with proper error handling and resource management
- **Type Safety**: Comprehensive error types and configuration validation
- **Testing Support**: Built-in mock implementations for development and testing

## What is Apache OpenDAL?

Apache OpenDAL (Open Data Access Layer) is a data access layer that allows users to easily and efficiently retrieve data from various storage services in a unified way. It supports:

- **Cloud Storage**: AWS S3, Azure Blob Storage, Google Cloud Storage, and many more
- **Local Storage**: Filesystem and in-memory storage
- **Databases**: Redis, PostgreSQL, MySQL, MongoDB, and others
- **Specialized Services**: GitHub, HDFS, WebDAV, IPFS, and more

## Installation

Add ZIO OpenDAL to your `build.sbt`:

```scala
libraryDependencies += "io.github.etacassiopeia" %% "zio-opendal" % "0.1.0"
```

### Scala Version Support

ZIO OpenDAL supports multiple Scala versions:
- **Scala 2.13.16** - Full compatibility
- **Scala 3.3.4** - LTS version 
- **Scala 3.4.3** - Latest stable
- **Scala 3.5.2** - Cutting edge

## Your First Program

Let's start with a simple example using in-memory storage:

```scala
import zio._
import zio.opendal.OpenDAL
import zio.opendal.config.LayerConfig

object HelloOpenDAL extends ZIOAppDefault {
  
  // Use in-memory storage for this example
  private val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    // Write some data
    _ <- OpenDAL.writeText("greeting.txt", "Hello, ZIO OpenDAL!")
    
    // Read it back
    content <- OpenDAL.readString("greeting.txt")
    
    // Print the result
    _ <- Console.printLine(s"Content: $content")
    
    // Check if file exists
    exists <- OpenDAL.exists("greeting.txt")
    _ <- Console.printLine(s"File exists: $exists")
    
    // Get file metadata
    metadata <- OpenDAL.stat("greeting.txt")
    _ <- Console.printLine(s"File size: ${metadata.getContentLength} bytes")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

Run this program with:
```bash
sbt run
```

## Understanding the Basics

### 1. Configuration

Every OpenDAL instance needs a configuration that specifies the storage backend:

```scala
import zio.opendal.config.LayerConfig

// In-memory storage (great for testing)
val memoryConfig = LayerConfig.memory()

// Local filesystem
val fsConfig = LayerConfig.filesystem("/tmp/my-app-data")

// AWS S3
val s3Config = LayerConfig.s3(
  bucket = "my-bucket",
  region = "us-east-1",
  accessKeyId = sys.env("AWS_ACCESS_KEY_ID"),
  secretAccessKey = sys.env("AWS_SECRET_ACCESS_KEY")
)
```

### 2. Layers

ZIO OpenDAL uses ZIO's layer system for dependency injection:

```scala
// Test layer (no native dependencies)
val testLayer = OpenDAL.testLayer(LayerConfig.memory())

// Live layer (uses native OpenDAL libraries)
val liveLayer = OpenDAL.live(s3Config)

// Use in your program
val program = for {
  _ <- OpenDAL.writeText("file.txt", "content")
  content <- OpenDAL.readString("file.txt")
  _ <- Console.printLine(content)
} yield ()

// Provide the layer
program.provide(testLayer) // or liveLayer
```

### 3. Basic Operations

ZIO OpenDAL provides a comprehensive API for storage operations:

```scala
// Writing data
_ <- OpenDAL.write("file.txt", "Hello, World!".getBytes("UTF-8"))
_ <- OpenDAL.writeText("greeting.txt", "Hello, World!")
_ <- OpenDAL.writeJson("config.json", """{"key": "value"}""")

// Reading data  
bytes <- OpenDAL.read("file.txt")
text <- OpenDAL.readString("greeting.txt")
partial <- OpenDAL.read("large-file.txt", ReadOpts.withRange(100, 50))

// File operations
_ <- OpenDAL.copy("source.txt", "destination.txt")  
_ <- OpenDAL.rename("old-name.txt", "new-name.txt")
_ <- OpenDAL.delete("unwanted-file.txt")
exists <- OpenDAL.exists("some-file.txt")

// Metadata
metadata <- OpenDAL.stat("file.txt")
size = metadata.getContentLength
contentType = metadata.getContentType

// Directory operations
_ <- OpenDAL.createDir("my-directory/")
files <- OpenDAL.list("my-directory/")
_ <- OpenDAL.removeAll("my-directory/")
```

## Working with Different Storage Backends

### Local Filesystem

Perfect for development and local applications:

```scala
val fsConfig = LayerConfig.filesystem("/tmp/my-app")
val fsLayer = OpenDAL.live(fsConfig)

val program = for {
  _ <- OpenDAL.writeText("logs/app.log", "Application started")
  _ <- OpenDAL.writeText("config/settings.json", """{"debug": true}""")
  
  logs <- OpenDAL.list("logs/")
  _ <- Console.printLine(s"Log files: ${logs.map(_.getPath).mkString(", ")}")
} yield ()

program.provide(fsLayer)
```

### AWS S3

For cloud storage with AWS:

```scala
val s3Config = LayerConfig.s3(
  bucket = "my-application-data",
  region = "us-west-2",
  accessKeyId = sys.env("AWS_ACCESS_KEY_ID"),
  secretAccessKey = sys.env("AWS_SECRET_ACCESS_KEY")
)

val s3Layer = OpenDAL.live(s3Config)

val program = for {
  // Upload user data
  _ <- OpenDAL.writeText("users/123/profile.json", """{"name": "John Doe"}""")
  
  // Upload with metadata
  _ <- OpenDAL.write(
    "images/avatar.jpg", 
    imageBytes,
    WriteOpts.empty.withContentType("image/jpeg")
  )
  
  // List user files
  userFiles <- OpenDAL.list("users/123/")
  _ <- Console.printLine(s"User files: ${userFiles.length}")
  
} yield ()

program.provide(s3Layer)
```

### Azure Blob Storage

For Microsoft Azure:

```scala
val azureConfig = LayerConfig.azureBlob(
  container = "my-container",
  accountName = "mystorageaccount", 
  accountKey = sys.env("AZURE_STORAGE_KEY")
)

val azureLayer = OpenDAL.live(azureConfig)

val program = for {
  _ <- OpenDAL.writeText("documents/report.txt", "Quarterly report...")
  content <- OpenDAL.readString("documents/report.txt")
  _ <- Console.printLine(s"Report length: ${content.length} characters")
} yield ()

program.provide(azureLayer)
```

## Error Handling

ZIO OpenDAL provides typed errors for better error handling:

```scala
import zio.opendal.error._

val program = for {
  content <- OpenDAL.readString("might-not-exist.txt")
  _ <- Console.printLine(s"Content: $content")
} yield ()

val safeProgram = program.catchSome {
  case NotFoundError(message, _) =>
    Console.printLine(s"File not found: $message")
  case NetworkError(message, _) =>
    Console.printLine(s"Network issue: $message") *>
    ZIO.sleep(1.second) *> 
    program // Retry
  case UnauthorizedError(message, _) =>
    Console.printLine(s"Access denied: $message") *>
    ZIO.fail(new SecurityException(message))
}
```

## Testing Your Code

Use the test layer for unit tests:

```scala
import zio.test._
import zio.opendal.OpenDAL
import zio.opendal.config.LayerConfig

object MyAppSpec extends ZIOSpecDefault {
  
  private val testLayer = OpenDAL.testLayer(LayerConfig.memory())
  
  def spec = suite("MyApp")(
    test("should store and retrieve data") {
      for {
        _ <- OpenDAL.writeText("test-file.txt", "test content")
        content <- OpenDAL.readString("test-file.txt")
      } yield assertTrue(content == "test content")
    },
    
    test("should handle file operations") {
      for {
        _ <- OpenDAL.writeText("source.txt", "original content")
        _ <- OpenDAL.copy("source.txt", "copy.txt")
        
        original <- OpenDAL.readString("source.txt")
        copied <- OpenDAL.readString("copy.txt")
        
      } yield assertTrue(original == copied)
    }
  ).provideLayer(testLayer)
}
```

## Advanced Configuration

### Retry Policies

Configure retry behavior for resilient applications:

```scala
import zio.Schedule

val resilientConfig = LayerConfig.s3(
  bucket = "my-bucket",
  region = "us-east-1",
  accessKeyId = "...",
  secretAccessKey = "..."
).withRetrySchedule(
  Schedule.exponential(100.millis) && 
  Schedule.recurs(5) &&
  Schedule.whileOutput(_ < 30.seconds)
)
```

### Custom Configuration

For advanced use cases, you can provide custom configuration:

```scala
import zio.opendal.scheme.CloudStorage

val customConfig = LayerConfig(
  scheme = CloudStorage.S3,
  config = Map(
    "bucket" -> "my-bucket",
    "region" -> "us-west-2",
    "access_key_id" -> "...",
    "secret_access_key" -> "...",
    "endpoint" -> "https://custom-s3-endpoint.com"
  )
)
```

## Next Steps

Now that you have the basics:

1. **[Configuration Guide](configuration.md)** - Learn about all supported storage backends
2. **[API Reference](api-reference.md)** - Explore the complete API
3. **[Examples](examples.md)** - See working examples for common patterns

## Common Patterns

### Batch Operations

Process multiple files efficiently:

```scala
val files = List("file1.txt", "file2.txt", "file3.txt")

// Parallel uploads
_ <- ZIO.foreachPar(files) { filename =>
  OpenDAL.writeText(filename, s"Content of $filename")
}

// Batch downloads  
contents <- ZIO.foreachPar(files)(OpenDAL.readString)
```

### Resource Cleanup

Ensure proper cleanup with ZIO's resource management:

```scala
val program = for {
  _ <- OpenDAL.writeText("temp-file.txt", "temporary data")
  
  // Use the file
  content <- OpenDAL.readString("temp-file.txt")
  _ <- Console.printLine(s"Processing: $content")
  
  // Cleanup
  _ <- OpenDAL.delete("temp-file.txt")
} yield ()

// Even better: use ensuring for guaranteed cleanup
val safeProgram = program.ensuring(
  OpenDAL.delete("temp-file.txt").ignore
)
```

### Configuration from Environment

Load configuration from environment variables:

```scala
val configFromEnv = for {
  bucket <- System.env("S3_BUCKET")
  region <- System.env("AWS_REGION") 
  accessKey <- System.env("AWS_ACCESS_KEY_ID")
  secretKey <- System.env("AWS_SECRET_ACCESS_KEY")
} yield LayerConfig.s3(
  bucket = bucket.getOrElse("default-bucket"),
  region = region.getOrElse("us-east-1"),
  accessKeyId = accessKey.getOrElse(""),
  secretAccessKey = secretKey.getOrElse("")
)

val dynamicLayer = ZLayer.fromZIO(configFromEnv.map(OpenDAL.live))
```

You're now ready to build applications with ZIO OpenDAL! Check out the other documentation sections for more advanced topics.
