package examples

import zio._
import zio.opendal.OpenDAL
import zio.opendal.config.LayerConfig

/** Example showing working syntax extensions with explicit usage */
object SyntaxExtensionsWorkingExample extends ZIOAppDefault {

  private val layer = OpenDAL.testLayer(LayerConfig.memory())

  def run = example.provide(layer)

  def example = for {
    _ <- Console.printLine("=== ZIO OpenDAL Working Syntax Extensions Example ===")
    
    // Demonstrate manual use of syntax extensions to avoid conflicts
    _ <- Console.printLine("\n1. Manual syntax usage (avoiding conflicts)...")
    
    // Import syntax in local scope to avoid conflicts
    _ <- {
      import zio.opendal.syntax._
      
      for {
        // These work in local import scope
        _ <- OpenDAL.write("test.txt", "Hello World")
        content <- OpenDAL.readString("test.txt") 
        _ <- Console.printLine(s"Content: $content")
        
        // Use byte array extensions  
        bytes = "Binary content".getBytes("UTF-8")
        _ <- OpenDAL.write("binary.bin", bytes)
        readBack <- OpenDAL.read("binary.bin")
        _ <- Console.printLine(s"Binary content: ${readBack.asUtf8String}")
        
        // Clean up
        _ <- OpenDAL.delete("test.txt")
        _ <- OpenDAL.delete("binary.bin")
      } yield ()
    }
    
    // Show the extension methods work on objects directly
    _ <- Console.printLine("\n2. Working with metadata and entries...")
    _ <- for {
      _ <- OpenDAL.write("meta-test.txt", "Metadata test")
      metadata <- OpenDAL.stat("meta-test.txt")
      // Use explicit method calls since extension methods may vary by Scala version
      size <- ZIO.succeed(metadata.getContentLength)
      _ <- Console.printLine(s"File size: $size bytes")
      _ <- Console.printLine(s"Is empty: ${size == 0}")
      
      _ <- OpenDAL.write("dir/entry1.txt", "Entry 1")
      _ <- OpenDAL.write("dir/entry2.txt", "Entry 2")
      entries <- OpenDAL.list("dir/")
      // Use standard Java methods instead of extensions for compatibility
      names = entries.map(_.getPath.split("/").last).mkString(", ")
      _ <- Console.printLine(s"Entry names: $names")
      
      // Clean up
      _ <- OpenDAL.removeAll("dir/")
      _ <- OpenDAL.delete("meta-test.txt")
    } yield ()
    
    _ <- Console.printLine("\n3. Recommended pattern: Use the regular API...")
    _ <- Console.printLine("The regular OpenDAL API is clean and works reliably across all Scala versions.")
    _ <- Console.printLine("Syntax extensions are available but may have import conflicts in some contexts.")
    
    // Show the clean regular API
    _ <- for {
      _ <- OpenDAL.writeText("example.txt", "Regular API example")
      content <- OpenDAL.readString("example.txt")
      size <- OpenDAL.stat("example.txt").map(_.getContentLength)
      exists <- OpenDAL.exists("example.txt")
      
      _ <- Console.printLine(s"Content: $content")
      _ <- Console.printLine(s"Size: $size bytes")  
      _ <- Console.printLine(s"Exists: $exists")
      
      _ <- OpenDAL.delete("example.txt")
    } yield ()
    
    _ <- Console.printLine("\n=== Working syntax extensions example completed! ===")
    
  } yield ()
}
