package zio.opendal.test

import zio._
import zio.opendal.OpenDAL
import zio.opendal.config.LayerConfig

// Simple test to verify basic integration
object MinimalTest extends ZIOAppDefault {
  
  // Test with the test layer first
  val testLayer = OpenDAL.testLayer(LayerConfig.memory())
  
  def run = {
    val program = for {
      _ <- Console.printLine("=== Testing ZIO OpenDAL ===")
      
      // Test basic write/read
      _ <- Console.printLine("1. Testing basic write/read...")
      _ <- OpenDAL.writeText("test.txt", "Hello, OpenDAL!")
      content <- OpenDAL.readString("test.txt")
      _ <- Console.printLine(s"Read content: $content")
      
      // Test file operations
      _ <- Console.printLine("2. Testing file operations...")
      _ <- OpenDAL.copy("test.txt", "test-copy.txt")
      copyContent <- OpenDAL.readString("test-copy.txt")
      _ <- Console.printLine(s"Copy content: $copyContent")
      
      // Test metadata
      _ <- Console.printLine("3. Testing metadata...")
      metadata <- OpenDAL.stat("test.txt")
      _ <- Console.printLine(s"File size: ${metadata.getContentLength}")
      
      // Test existence check
      _ <- Console.printLine("4. Testing existence check...")
      exists <- OpenDAL.exists("test.txt")
      notExists <- OpenDAL.exists("does-not-exist.txt")
      _ <- Console.printLine(s"test.txt exists: $exists, non-existent file: $notExists")
      
      // Test directory operations
      _ <- Console.printLine("5. Testing directory operations...")
      _ <- OpenDAL.writeText("dir/file1.txt", "File 1")
      _ <- OpenDAL.writeText("dir/file2.txt", "File 2")
      entries <- OpenDAL.list("dir/")
      _ <- Console.printLine(s"Directory entries: ${entries.map(_.getPath).mkString(", ")}")
      
      _ <- Console.printLine("=== All tests completed successfully! ===")
    } yield ()
    
    program.provide(testLayer)
  }
}
