package zio.opendal

import zio.test._
import zio._
import zio.opendal.core.{OpenDAL => OpenDALTrait}
import zio.opendal.config.LayerConfig
import zio.opendal.options._
import java.nio.file.Files
import java.nio.charset.StandardCharsets

object OpenDALSpec extends ZIOSpecDefault {
  private val tempDir = Files.createTempDirectory("zio-opendal-test").toString

  // Use test layers that don't load native libraries to avoid JNI crashes
  private val fsLayer     = OpenDAL.testLayer(LayerConfig.filesystem(tempDir))
  private val memoryLayer = OpenDAL.testLayer(LayerConfig.memory())

  private def writeReadTest(layer: ZLayer[Any, Nothing, OpenDALTrait]) = test("write and read") {
    val data = "hello".getBytes(StandardCharsets.UTF_8)
    for {
      _        <- OpenDAL.write("test.txt", data)
      readData <- OpenDAL.read("test.txt")
    } yield assertTrue(new String(readData, StandardCharsets.UTF_8) == "hello")
  }.provideLayer(layer)

  def spec = suite("OpenDALSpec")(
    suite("Local File System")(
      writeReadTest(fsLayer),
      test("stat") {
        val data = "hello".getBytes(StandardCharsets.UTF_8)
        for {
          _    <- OpenDAL.write("test.txt", data)
          meta <- OpenDAL.stat("test.txt")
        } yield assertTrue(meta.getContentLength == 5L)
      }.provideLayer(fsLayer)
    ),
    suite("In-Memory Storage")(
      writeReadTest(memoryLayer),
      test("string write operations") {
        for {
          _       <- OpenDAL.write("string-test.txt", "Hello, String!")
          content <- OpenDAL.read("string-test.txt")
          result = new String(content, StandardCharsets.UTF_8)
        } yield assertTrue(result == "Hello, String!")
      }.provideLayer(memoryLayer),
      test("partial read operations") {
        val testData = "0123456789abcdef"
        for {
          _       <- OpenDAL.write("partial.txt", testData)
          partial <- OpenDAL.read("partial.txt", 5, 5) // Read 5 bytes starting from offset 5
          result = new String(partial, StandardCharsets.UTF_8)
        } yield assertTrue(result == "56789")
      }.provideLayer(memoryLayer),
      test("read operations with options") {
        val testData = "0123456789abcdef"
        for {
          _       <- OpenDAL.write("opts.txt", testData)
          partial <- OpenDAL.read("opts.txt", ReadOpts(offset = Some(10), length = Some(4)))
          result = new String(partial, StandardCharsets.UTF_8)
        } yield assertTrue(result == "abcd")
      }.provideLayer(memoryLayer),
      test("write operations with options") {
        for {
          _       <- OpenDAL.write("content-type.txt", "Hello!", WriteOpts(contentType = Some("text/plain")))
          content <- OpenDAL.read("content-type.txt")
          result = new String(content, StandardCharsets.UTF_8)
        } yield assertTrue(result == "Hello!")
      }.provideLayer(memoryLayer),
      test("stat operations with options") {
        val data = "test data".getBytes(StandardCharsets.UTF_8)
        for {
          _    <- OpenDAL.write("checksum.txt", data)
          meta <- OpenDAL.stat("checksum.txt", StatOpts(checksum = true))
        } yield assertTrue(meta.getContentLength == 9L)
      }.provideLayer(memoryLayer),
      test("file operations - copy and rename") {
        val testData = "copy test data"
        for {
          _           <- OpenDAL.write("source.txt", testData)
          _           <- OpenDAL.copy("source.txt", "copied.txt")
          copiedData  <- OpenDAL.read("copied.txt")
          _           <- OpenDAL.rename("copied.txt", "renamed.txt")
          renamedData <- OpenDAL.read("renamed.txt")
          copiedStr  = new String(copiedData, StandardCharsets.UTF_8)
          renamedStr = new String(renamedData, StandardCharsets.UTF_8)
        } yield assertTrue(copiedStr == testData && renamedStr == testData)
      }.provideLayer(memoryLayer),
      test("directory operations") {
        for {
          _       <- OpenDAL.createDir("test-dir/")
          _       <- OpenDAL.write("test-dir/file1.txt", "file 1")
          _       <- OpenDAL.write("test-dir/file2.txt", "file 2")
          entries <- OpenDAL.list("test-dir/")
          _       <- OpenDAL.removeAll("test-dir/")
        } yield assertTrue(entries.length >= 2)
      }.provideLayer(memoryLayer),
      test("list operations with options") {
        for {
          _              <- OpenDAL.write("list-test/file1.txt", "data1")
          _              <- OpenDAL.write("list-test/file2.txt", "data2")
          _              <- OpenDAL.write("list-test/file3.txt", "data3")
          allEntries     <- OpenDAL.list("list-test/")
          limitedEntries <- OpenDAL.list("list-test/", ListOpts(recursive = false, limit = Some(2)))
        } yield assertTrue(allEntries.length >= 3 && limitedEntries.length <= 2)
      }.provideLayer(memoryLayer),
      test("delete operations") {
        for {
          _      <- OpenDAL.write("to-delete.txt", "will be deleted")
          _      <- OpenDAL.delete("to-delete.txt")
          result <- OpenDAL.read("to-delete.txt").either
        } yield assertTrue(result.isLeft)
      }.provideLayer(memoryLayer),
      test("info and capabilities") {
        for {
          info         <- OpenDAL.info
          capabilities <- OpenDAL.capabilities
        } yield assertTrue(info.getScheme == "memory" && capabilities != null)
      }.provideLayer(memoryLayer),
      test("presigned operations should fail in test layer") {
        for {
          result <- OpenDAL.presignRead("test.txt", 1.hour).either
        } yield assertTrue(result.isLeft)
      }.provideLayer(memoryLayer)
    )
  )
}
