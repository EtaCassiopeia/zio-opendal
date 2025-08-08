package examples

import zio._
import zio.opendal.OpenDAL
import zio.opendal.config.LayerConfig
import zio.opendal.options.{ReadOpts, ListOpts}

/** Basic usage example showing common operations */
object BasicUsageExample extends ZIOAppDefault {

  // Create configuration for in-memory storage (perfect for examples)
  private val config = LayerConfig.memory()
  
  // Use test layer to avoid native library dependencies in examples
  private val layer = OpenDAL.testLayer(config)

  def run = example.provide(layer)

  def example = for {
    _ <- Console.printLine("=== ZIO OpenDAL Basic Usage Example ===")
    
    // Basic write and read
    _ <- Console.printLine("\n1. Writing and reading files...")
    _ <- OpenDAL.write("hello.txt", "Hello, World!")
    content <- OpenDAL.readString("hello.txt")
    _ <- Console.printLine(s"Read content: $content")
    
    // Write with options
    _ <- Console.printLine("\n2. Writing with content type...")
    _ <- OpenDAL.writeJson("config.json", """{"name": "zio-opendal", "version": "0.1.0"}""")
    jsonContent <- OpenDAL.readString("config.json")
    _ <- Console.printLine(s"JSON content: $jsonContent")
    
    // Partial reads
    _ <- Console.printLine("\n3. Partial reads...")
    _ <- OpenDAL.write("data.txt", "0123456789abcdef")
    partial <- OpenDAL.read("data.txt", ReadOpts.withRange(5, 5))
    partialStr = new String(partial, "UTF-8")
    _ <- Console.printLine(s"Partial read (offset=5, length=5): $partialStr")
    
    // File operations
    _ <- Console.printLine("\n4. File operations...")
    _ <- OpenDAL.copy("hello.txt", "hello-copy.txt")
    _ <- OpenDAL.rename("hello-copy.txt", "hello-renamed.txt")
    exists <- OpenDAL.exists("hello-renamed.txt")
    _ <- Console.printLine(s"Renamed file exists: $exists")
    
    // Directory operations
    _ <- Console.printLine("\n5. Directory operations...")
    _ <- OpenDAL.write("dir/file1.txt", "File 1 content")
    _ <- OpenDAL.write("dir/file2.txt", "File 2 content")
    _ <- OpenDAL.write("dir/subdir/file3.txt", "File 3 content")
    
    // List files
    files <- OpenDAL.list("dir/")
    _ <- Console.printLine(s"Files in dir/: ${files.map(_.getPath).mkString(", ")}")
    
    // Recursive list
    allFiles <- OpenDAL.list("dir/", ListOpts.recursive)
    _ <- Console.printLine(s"All files recursively: ${allFiles.map(_.getPath).mkString(", ")}")
    
    // Get file metadata
    _ <- Console.printLine("\n6. File metadata...")
    metadata <- OpenDAL.stat("hello.txt")
    _ <- Console.printLine(s"File size: ${metadata.getContentLength} bytes")
    _ <- Console.printLine(s"Content type: ${metadata.getContentType}")
    
    // Clean up
    _ <- Console.printLine("\n7. Cleanup...")
    _ <- OpenDAL.removeAll("dir/")
    _ <- OpenDAL.delete("hello.txt")
    _ <- OpenDAL.delete("hello-renamed.txt") 
    _ <- OpenDAL.delete("config.json")
    _ <- OpenDAL.delete("data.txt")
    
    _ <- Console.printLine("\n=== Example completed successfully! ===")
    
  } yield ()
}
