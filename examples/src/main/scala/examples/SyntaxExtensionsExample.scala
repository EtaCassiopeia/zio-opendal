package examples

import zio._
import zio.opendal.OpenDAL
import zio.opendal.config.LayerConfig

/** Example showing the convenient API usage (syntax extensions available separately) */
object SyntaxExtensionsExample extends ZIOAppDefault {

  private val layer = OpenDAL.testLayer(LayerConfig.memory())

  def run = example.provide(layer)

  def example = for {
    _ <- Console.printLine("=== ZIO OpenDAL Syntax Extensions Example ===")
    
    // Instead of syntax extensions, demonstrate the regular API for now
    _ <- Console.printLine("\n1. Writing and reading files...")
    _ <- OpenDAL.write("greeting.txt", "Hello, Syntax!")
    _ <- OpenDAL.writeJson("greeting.json", """{"message": "Hello JSON!"}""")
    _ <- OpenDAL.writeText("plain.txt", "Plain text content")
    
    // Reading files
    _ <- Console.printLine("\n2. Reading files...")
    content <- OpenDAL.readString("greeting.txt")
    _ <- Console.printLine(s"Greeting: $content")
    
    jsonContent <- OpenDAL.readString("greeting.json")
    _ <- Console.printLine(s"JSON: $jsonContent")
    
    // File metadata
    _ <- Console.printLine("\n3. File metadata...")
    metadata <- OpenDAL.stat("greeting.txt")
    _ <- Console.printLine(s"File size: ${metadata.getContentLength} bytes")
    _ <- Console.printLine(s"Content type: ${Option(metadata.getContentType).getOrElse("unknown")}")
    
    exists <- OpenDAL.exists("greeting.txt")
    _ <- Console.printLine(s"File exists: $exists")
    
    // File operations
    _ <- Console.printLine("\n4. File operations...")
    _ <- OpenDAL.copy("greeting.txt", "greeting-copy.txt")
    _ <- OpenDAL.rename("greeting-copy.txt", "greeting-renamed.txt")
    
    // Byte array operations
    _ <- Console.printLine("\n5. Byte array operations...")
    bytes = "Binary data".getBytes("UTF-8")
    _ <- OpenDAL.write("binary.dat", bytes)
    readBytes <- OpenDAL.read("binary.dat")
    _ <- Console.printLine(s"Binary content as string: ${new String(readBytes, "UTF-8")}")
    
    // Directory operations
    _ <- Console.printLine("\n6. Directory operations...")  
    _ <- OpenDAL.write("data/file1.txt", "File 1")
    _ <- OpenDAL.write("data/file2.txt", "File 2")
    
    entries <- OpenDAL.list("data/")
    _ <- Console.printLine(s"Files: ${entries.map(_.getPath).mkString(", ")}")
    
    // Clean up
    _ <- Console.printLine("\n7. Cleanup...")
    _ <- OpenDAL.removeAll("data/")
    _ <- OpenDAL.delete("greeting.txt")
    _ <- OpenDAL.delete("greeting.json")
    _ <- OpenDAL.delete("greeting-renamed.txt")
    _ <- OpenDAL.delete("plain.txt")
    _ <- OpenDAL.delete("binary.dat")
    
    _ <- Console.printLine("\n=== API usage example completed! ===")
    _ <- Console.printLine("\nNote: Syntax extensions are available but may conflict with Scala standard library.")
    _ <- Console.printLine("For syntax extensions, use explicit imports or method calls to avoid conflicts.")
    
  } yield ()
}
