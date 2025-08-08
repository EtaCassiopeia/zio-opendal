# Examples

This document provides comprehensive examples for common use cases with ZIO OpenDAL.

## Running the Examples

The project includes working examples that you can run:

```bash
# Basic usage example
sbt "project examples" "runMain examples.BasicUsageExample"

# Cloud storage configuration  
sbt "project examples" "runMain examples.CloudStorageExample"

# API usage patterns
sbt "project examples" "runMain examples.SyntaxExtensionsExample"

# Working with extensions
sbt "project examples" "runMain examples.SyntaxExtensionsWorkingExample"
```

## Basic File Operations

### Simple File I/O

```scala
import zio._
import zio.opendal.OpenDAL
import zio.opendal.config.LayerConfig

object BasicFileIO extends ZIOAppDefault {
  
  val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    // Write text file
    _ <- OpenDAL.writeText("greeting.txt", "Hello, ZIO OpenDAL!")
    
    // Read it back
    content <- OpenDAL.readString("greeting.txt")
    _ <- Console.printLine(s"Content: $content")
    
    // Write binary data
    bytes = "Binary data".getBytes("UTF-8")
    _ <- OpenDAL.write("data.bin", bytes)
    
    // Read binary data
    readBytes <- OpenDAL.read("data.bin")
    _ <- Console.printLine(s"Binary: ${new String(readBytes, "UTF-8")}")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

### File Metadata and Operations

```scala
object FileOperations extends ZIOAppDefault {
  
  val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    // Create test file
    _ <- OpenDAL.writeText("test.txt", "Test content for operations")
    
    // Get file metadata
    metadata <- OpenDAL.stat("test.txt")
    _ <- Console.printLine(s"File size: ${metadata.getContentLength} bytes")
    _ <- Console.printLine(s"Content type: ${Option(metadata.getContentType).getOrElse("unknown")}")
    
    // Check if file exists
    exists <- OpenDAL.exists("test.txt")
    _ <- Console.printLine(s"File exists: $exists")
    
    // Copy file
    _ <- OpenDAL.copy("test.txt", "test-copy.txt")
    
    // Verify copy exists
    copyExists <- OpenDAL.exists("test-copy.txt")
    _ <- Console.printLine(s"Copy exists: $copyExists")
    
    // Rename file
    _ <- OpenDAL.rename("test-copy.txt", "test-renamed.txt")
    
    // Clean up
    _ <- OpenDAL.delete("test.txt")
    _ <- OpenDAL.delete("test-renamed.txt")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

## Working with Options

### Read Operations with Options

```scala
object ReadWithOptions extends ZIOAppDefault {
  
  val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    // Create test file with known content
    testData = "0123456789abcdefghijklmnopqrstuvwxyz"
    _ <- OpenDAL.writeText("data.txt", testData)
    
    // Read entire file
    fullContent <- OpenDAL.readString("data.txt")
    _ <- Console.printLine(s"Full content: $fullContent")
    
    // Read with offset
    offsetData <- OpenDAL.read("data.txt", ReadOpts.withOffset(10))
    _ <- Console.printLine(s"From offset 10: ${new String(offsetData, "UTF-8")}")
    
    // Read specific range
    rangeData <- OpenDAL.read("data.txt", ReadOpts.withRange(5, 10))
    _ <- Console.printLine(s"Range 5-15: ${new String(rangeData, "UTF-8")}")
    
    // Read with buffer size
    bufferedData <- OpenDAL.read("data.txt", ReadOpts.empty.withBufferSize(1024))
    _ <- Console.printLine(s"Buffered read: ${new String(bufferedData, "UTF-8")}")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

### Write Operations with Options

```scala
object WriteWithOptions extends ZIOAppDefault {
  
  val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    // Write JSON with content type
    jsonData = """{"name": "ZIO OpenDAL", "version": "1.0.0"}"""
    _ <- OpenDAL.write("config.json", jsonData, WriteOpts.json)
    
    // Write text with content type
    _ <- OpenDAL.write("readme.txt", "README content", WriteOpts.text)
    
    // Write with custom options
    imageBytes = Array[Byte](137, 80, 78, 71) // PNG header
    _ <- OpenDAL.write("image.png", imageBytes, WriteOpts.empty
      .withContentType("image/png")
      .withCacheControl("max-age=3600")
      .withMetadata("author", "ZIO OpenDAL")
    )
    
    // Write with multiple metadata entries
    _ <- OpenDAL.write("document.pdf", "PDF content".getBytes(), WriteOpts.empty
      .withContentType("application/pdf")
      .withMetadata(Map(
        "title" -> "User Manual",
        "author" -> "Documentation Team",
        "version" -> "1.0"
      ))
    )
    
    _ <- Console.printLine("Files written with custom options")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

## Directory Operations

### Directory Management

```scala
object DirectoryOperations extends ZIOAppDefault {
  
  val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    // Create directory structure with files
    _ <- OpenDAL.writeText("docs/readme.md", "# Project Documentation")
    _ <- OpenDAL.writeText("docs/api.md", "# API Documentation")
    _ <- OpenDAL.writeText("docs/examples/basic.md", "# Basic Examples")
    _ <- OpenDAL.writeText("docs/examples/advanced.md", "# Advanced Examples")
    _ <- OpenDAL.writeText("src/main.scala", "object Main extends App")
    
    // List top-level directories
    rootEntries <- OpenDAL.list("")
    _ <- Console.printLine(s"Root entries: ${rootEntries.map(_.getPath).mkString(", ")}")
    
    // List docs directory
    docsEntries <- OpenDAL.list("docs/")
    _ <- Console.printLine(s"Docs entries: ${docsEntries.map(_.getPath).mkString(", ")}")
    
    // List with options - limit results
    limitedEntries <- OpenDAL.list("docs/", ListOpts.withLimit(2))
    _ <- Console.printLine(s"Limited entries: ${limitedEntries.map(_.getPath).mkString(", ")}")
    
    // Recursive listing
    allEntries <- OpenDAL.list("docs/", ListOpts.recursive)
    _ <- Console.printLine(s"All docs files: ${allEntries.map(_.getPath).mkString(", ")}")
    
    // List with prefix filter
    exampleEntries <- OpenDAL.list("docs/", ListOpts.empty
      .recursive
      .withPrefix("docs/examples/")
    )
    _ <- Console.printLine(s"Example files: ${exampleEntries.map(_.getPath).mkString(", ")}")
    
    // Clean up - remove entire directory
    _ <- OpenDAL.removeAll("docs/")
    _ <- OpenDAL.removeAll("src/")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

## Error Handling

### Basic Error Handling

```scala
object ErrorHandling extends ZIOAppDefault {
  
  val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    // Try to read non-existent file
    result <- OpenDAL.readString("non-existent.txt").either
    
    _ <- result match {
      case Left(error) => Console.printLine(s"Expected error: ${error.getMessage}")
      case Right(content) => Console.printLine(s"Unexpected content: $content")
    }
    
    // Handle specific error types
    handled <- (for {
      content <- OpenDAL.readString("missing-file.txt")
      _ <- Console.printLine(s"Content: $content")
    } yield ()).catchSome {
      case NotFoundError(msg, _) =>
        Console.printLine(s"File not found: $msg") *>
        OpenDAL.writeText("missing-file.txt", "Created file") *>
        OpenDAL.readString("missing-file.txt")
      case NetworkError(msg, _) =>
        Console.printLine(s"Network error: $msg") *>
        ZIO.succeed("fallback content")
    }
    
    _ <- Console.printLine(s"Final result: $handled")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

### Retry Logic

```scala
object RetryExample extends ZIOAppDefault {
  
  // Create config with retry policy
  val config = LayerConfig.memory()
    .withRetrySchedule(
      Schedule.exponential(100.millis) && Schedule.recurs(3)
    )
  
  val layer = OpenDAL.live(config)
  
  val program = for {
    // This will retry automatically on retriable errors
    _ <- OpenDAL.writeText("test.txt", "content")
    content <- OpenDAL.readString("test.txt")
    _ <- Console.printLine(s"Content: $content")
    
    // Custom retry for specific operations
    resilientRead <- OpenDAL.readString("might-fail.txt").retry(
      Schedule.recurWhile[OpenDALError](_.isRetriable) &&
      Schedule.exponential(200.millis) &&
      Schedule.recurs(5)
    ).catchAll { error =>
      Console.printLine(s"Failed after retries: ${error.getMessage}") *>
      ZIO.succeed("default content")
    }
    
    _ <- Console.printLine(s"Resilient read: $resilientRead")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

## Cloud Storage Examples

### AWS S3

```scala
object S3Example extends ZIOAppDefault {
  
  val s3Config = LayerConfig.s3(
    bucket = sys.env.getOrElse("S3_BUCKET", "my-test-bucket"),
    region = sys.env.getOrElse("AWS_REGION", "us-east-1"),
    accessKeyId = sys.env("AWS_ACCESS_KEY_ID"),
    secretAccessKey = sys.env("AWS_SECRET_ACCESS_KEY")
  )
  
  // Use test layer for demo (replace with OpenDAL.live(s3Config) for real S3)
  val layer = OpenDAL.testLayer(s3Config)
  
  val program = for {
    _ <- Console.printLine("Working with S3...")
    
    // Upload user data
    userData = """{"id": 123, "name": "John Doe", "email": "john@example.com"}"""
    _ <- OpenDAL.writeJson("users/123/profile.json", userData)
    
    // Upload with metadata
    reportData = "Q4 2023 Financial Report..."
    _ <- OpenDAL.write("reports/2023/q4.txt", reportData, WriteOpts.empty
      .withContentType("text/plain")
      .withMetadata(Map(
        "department" -> "finance",
        "quarter" -> "Q4",
        "year" -> "2023"
      ))
    )
    
    // List user files
    userFiles <- OpenDAL.list("users/123/")
    _ <- Console.printLine(s"User files: ${userFiles.map(_.getPath).mkString(", ")}")
    
    // Download and process
    profile <- OpenDAL.readString("users/123/profile.json")
    _ <- Console.printLine(s"User profile: $profile")
    
    // Batch operations
    reports = List("q1", "q2", "q3", "q4")
    _ <- ZIO.foreachPar(reports) { quarter =>
      OpenDAL.writeText(s"reports/2023/$quarter.txt", s"$quarter 2023 report")
    }
    
    allReports <- OpenDAL.list("reports/2023/")
    _ <- Console.printLine(s"All reports: ${allReports.length}")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

### Azure Blob Storage

```scala
object AzureExample extends ZIOAppDefault {
  
  val azureConfig = LayerConfig.azureBlob(
    container = sys.env.getOrElse("AZURE_CONTAINER", "my-container"),
    accountName = sys.env.getOrElse("AZURE_ACCOUNT_NAME", "mystorageaccount"),
    accountKey = sys.env("AZURE_STORAGE_KEY")
  )
  
  // Use test layer for demo
  val layer = OpenDAL.testLayer(azureConfig)
  
  val program = for {
    _ <- Console.printLine("Working with Azure Blob Storage...")
    
    // Upload documents with proper content types
    _ <- OpenDAL.write("documents/contract.pdf", "PDF content".getBytes(), 
      WriteOpts.empty.withContentType("application/pdf"))
    
    _ <- OpenDAL.write("documents/spreadsheet.xlsx", "Excel content".getBytes(),
      WriteOpts.empty.withContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    
    // Upload images with cache headers
    imageData = "fake image data".getBytes()
    _ <- OpenDAL.write("images/logo.png", imageData, WriteOpts.empty
      .withContentType("image/png")
      .withCacheControl("max-age=31536000") // 1 year
    )
    
    // List all documents
    documents <- OpenDAL.list("documents/")
    _ <- Console.printLine(s"Documents: ${documents.map(_.getPath).mkString(", ")}")
    
    // Get file sizes
    sizes <- ZIO.foreach(documents) { entry =>
      OpenDAL.stat(entry.getPath).map(_.getContentLength)
    }
    totalSize = sizes.sum
    _ <- Console.printLine(s"Total document size: $totalSize bytes")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

## Advanced Patterns

### Batch Processing

```scala
object BatchProcessing extends ZIOAppDefault {
  
  val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    _ <- Console.printLine("Batch processing example...")
    
    // Generate test data
    testFiles = (1 to 100).map(i => s"data-$i.txt" -> s"Content for file $i")
    
    // Parallel batch upload
    _ <- Console.printLine("Uploading files in parallel...")
    start <- Clock.nanoTime
    _ <- ZIO.foreachPar(testFiles) { case (filename, content) =>
      OpenDAL.writeText(filename, content)
    }
    end <- Clock.nanoTime
    uploadTime = (end - start) / 1_000_000 // Convert to milliseconds
    _ <- Console.printLine(s"Uploaded ${testFiles.length} files in ${uploadTime}ms")
    
    // Batch download with processing
    _ <- Console.printLine("Processing files in batches...")
    batchSize = 10
    batches = testFiles.map(_._1).grouped(batchSize).toList
    
    results <- ZIO.foreach(batches) { batch =>
      ZIO.foreachPar(batch) { filename =>
        for {
          content <- OpenDAL.readString(filename)
          size <- OpenDAL.size(filename)
        } yield (filename, content.length, size)
      }
    }
    
    allResults = results.flatten
    totalChars = allResults.map(_._2).sum
    totalBytes = allResults.map(_._3).sum
    
    _ <- Console.printLine(s"Processed ${allResults.length} files")
    _ <- Console.printLine(s"Total characters: $totalChars")
    _ <- Console.printLine(s"Total bytes: $totalBytes")
    
    // Cleanup in parallel
    _ <- ZIO.foreachPar(testFiles.map(_._1))(OpenDAL.delete)
    
  } yield ()
  
  def run = program.provide(layer)
}
```

### Stream Processing

```scala
object StreamProcessing extends ZIOAppDefault {
  
  val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    _ <- Console.printLine("Stream processing example...")
    
    // Create large dataset
    largeData = (1 to 1000).map(i => s"Record $i: ${Random.alphanumeric.take(100).mkString}").mkString("\\n")
    _ <- OpenDAL.writeText("large-dataset.txt", largeData)
    
    // Process file in chunks
    fileSize <- OpenDAL.size("large-dataset.txt")
    _ <- Console.printLine(s"Processing file of size: $fileSize bytes")
    
    chunkSize = 1024
    chunks = (0L until fileSize by chunkSize).toList
    
    // Process chunks and collect statistics
    stats <- ZIO.foreach(chunks) { offset =>
      val length = math.min(chunkSize, fileSize - offset)
      for {
        chunk <- OpenDAL.read("large-dataset.txt", offset, length)
        lines = new String(chunk, "UTF-8").split("\\n").length
      } yield (offset, length, lines)
    }
    
    totalLines = stats.map(_._3).sum
    _ <- Console.printLine(s"Processed ${chunks.length} chunks, total lines: $totalLines")
    
    // Streaming transformation - read, transform, write
    transformedData <- for {
      original <- OpenDAL.readString("large-dataset.txt")
      transformed = original.split("\\n").map(line => s"PROCESSED: $line").mkString("\\n")
      _ <- OpenDAL.writeText("processed-dataset.txt", transformed)
      processedSize <- OpenDAL.size("processed-dataset.txt")
    } yield processedSize
    
    _ <- Console.printLine(s"Transformed file size: $transformedData bytes")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

### Resource Management

```scala
object ResourceManagement extends ZIOAppDefault {
  
  val layer = OpenDAL.testLayer(LayerConfig.memory())
  
  val program = for {
    _ <- Console.printLine("Resource management example...")
    
    // Automatic cleanup with ensuring
    result <- (for {
      _ <- OpenDAL.writeText("temp-file.txt", "temporary data")
      content <- OpenDAL.readString("temp-file.txt")
      processed = content.toUpperCase
      _ <- OpenDAL.writeText("processed-temp.txt", processed)
    } yield processed).ensuring(
      // Cleanup guaranteed even if operation fails
      OpenDAL.delete("temp-file.txt").ignore *>
      OpenDAL.delete("processed-temp.txt").ignore
    )
    
    _ <- Console.printLine(s"Processed result: $result")
    
    // Scoped resource management
    scopedResult <- ZIO.scoped {
      for {
        // Acquire resources
        _ <- ZIO.acquireRelease(
          OpenDAL.writeText("scoped-resource.txt", "scoped data")
        )(_ => OpenDAL.delete("scoped-resource.txt").ignore)
        
        // Use resources
        content <- OpenDAL.readString("scoped-resource.txt")
      } yield content
    } // Resources automatically released here
    
    _ <- Console.printLine(s"Scoped result: $scopedResult")
    
    // Batch resource management
    tempFiles = List("temp1.txt", "temp2.txt", "temp3.txt")
    batchResult <- (for {
      // Create temp files
      _ <- ZIO.foreach(tempFiles.zipWithIndex) { case (filename, index) =>
        OpenDAL.writeText(filename, s"temp data $index")
      }
      
      // Process them
      contents <- ZIO.foreach(tempFiles)(OpenDAL.readString)
      combined = contents.mkString(" | ")
      
    } yield combined).ensuring(
      // Clean up all temp files
      ZIO.foreach(tempFiles)(filename => 
        OpenDAL.delete(filename).ignore
      ).ignore
    )
    
    _ <- Console.printLine(s"Batch result: $batchResult")
    
  } yield ()
  
  def run = program.provide(layer)
}
```

## Testing Patterns

### Unit Testing

```scala
import zio.test._

object OpenDALServiceSpec extends ZIOSpecDefault {
  
  val testLayer = OpenDAL.testLayer(LayerConfig.memory())
  
  def spec = suite("OpenDAL Service")(
    
    test("should write and read files") {
      for {
        _ <- OpenDAL.writeText("test.txt", "test content")
        content <- OpenDAL.readString("test.txt")
      } yield assertTrue(content == "test content")
    },
    
    test("should handle file operations") {
      for {
        _ <- OpenDAL.writeText("original.txt", "original content")
        _ <- OpenDAL.copy("original.txt", "copy.txt")
        originalContent <- OpenDAL.readString("original.txt")
        copyContent <- OpenDAL.readString("copy.txt")
        originalExists <- OpenDAL.exists("original.txt")
        copyExists <- OpenDAL.exists("copy.txt")
      } yield assertTrue(
        originalContent == "original content" &&
        copyContent == "original content" &&
        originalExists && copyExists
      )
    },
    
    test("should handle directory operations") {
      for {
        _ <- OpenDAL.writeText("dir/file1.txt", "content 1")
        _ <- OpenDAL.writeText("dir/file2.txt", "content 2")
        entries <- OpenDAL.list("dir/")
        _ <- OpenDAL.removeAll("dir/")
        entriesAfterRemoval <- OpenDAL.list("dir/").either
      } yield assertTrue(
        entries.length == 2 &&
        entriesAfterRemoval.isLeft
      )
    },
    
    test("should handle errors appropriately") {
      for {
        result <- OpenDAL.readString("non-existent.txt").either
      } yield assertTrue(result.isLeft)
    }
    
  ).provide(testLayer)
}
```

### Integration Testing

```scala
object IntegrationTestSpec extends ZIOSpecDefault {
  
  // Test with filesystem to simulate real storage
  val fsLayer = OpenDAL.testLayer(LayerConfig.filesystem("/tmp/opendal-test"))
  
  def spec = suite("Integration Tests")(
    
    test("should work with filesystem-like operations") {
      for {
        // Test realistic file sizes and operations
        largeContent = "x" * 10000
        _ <- OpenDAL.writeText("large-file.txt", largeContent)
        
        // Test partial reads
        partial <- OpenDAL.read("large-file.txt", ReadOpts.withRange(5000, 1000))
        
        // Test metadata
        metadata <- OpenDAL.stat("large-file.txt")
        
        // Test batch operations
        files = (1 to 10).map(i => s"batch-file-$i.txt")
        _ <- ZIO.foreachPar(files)(f => OpenDAL.writeText(f, f))
        
        // Test listing
        entries <- OpenDAL.list("")
        
        // Cleanup
        _ <- ZIO.foreach(files)(OpenDAL.delete)
        _ <- OpenDAL.delete("large-file.txt")
        
      } yield assertTrue(
        partial.length == 1000 &&
        metadata.getContentLength == 10000 &&
        entries.length >= 10
      )
    }
    
  ).provide(fsLayer)
}
```

## Configuration Patterns

### Environment-Based Configuration

```scala
object EnvironmentConfig extends ZIOAppDefault {
  
  val configLayer = ZLayer.fromZIO {
    for {
      provider <- System.env("STORAGE_PROVIDER")
      bucket <- System.env("STORAGE_BUCKET") 
      region <- System.env("STORAGE_REGION")
      accessKey <- System.env("AWS_ACCESS_KEY_ID")
      secretKey <- System.env("AWS_SECRET_ACCESS_KEY")
    } yield {
      provider.getOrElse("memory") match {
        case "s3" => LayerConfig.s3(
          bucket.getOrElse("default-bucket"),
          region.getOrElse("us-east-1"),
          accessKey.getOrElse(""),
          secretKey.getOrElse("")
        )
        case "filesystem" => LayerConfig.filesystem(
          bucket.getOrElse("/tmp/storage")
        )
        case _ => LayerConfig.memory()
      }
    }
  }
  
  val openDALLayer = configLayer >>> ZLayer.fromFunction(OpenDAL.testLayer)
  
  val program = for {
    _ <- Console.printLine("Using environment-based configuration...")
    _ <- OpenDAL.writeText("config-test.txt", "Environment config works!")
    content <- OpenDAL.readString("config-test.txt")
    _ <- Console.printLine(s"Content: $content")
  } yield ()
  
  def run = program.provide(openDALLayer)
}
```

These examples demonstrate the key patterns and use cases for ZIO OpenDAL. You can adapt them to your specific needs and storage backends.

For more examples, check out the working examples in the [examples/](../../examples/) directory of the project.
