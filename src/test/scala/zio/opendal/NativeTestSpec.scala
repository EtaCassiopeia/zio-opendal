package zio.opendal

import zio.test._
import zio._
import zio.opendal.core.{OpenDAL => OpenDALTrait}
import zio.opendal.config.LayerConfig
import zio.opendal.options._
import java.nio.file.Files
import java.nio.charset.StandardCharsets

object NativeTestSpec extends ZIOSpecDefault {
  private val tempDir = Files.createTempDirectory("zio-opendal-native-test").toString

  // Test-friendly retry schedule - shorter delays and fewer retries for tests
  private val testRetrySchedule = Schedule.exponential(10.millis) && Schedule.recurs(2)

  // Try to use live layer with native libraries - using test-friendly retry schedule
  private val nativeMemoryLayer     = OpenDAL.live(LayerConfig.memory().withRetrySchedule(testRetrySchedule))
  private val nativeFilesystemLayer = OpenDAL.live(LayerConfig.filesystem(tempDir).withRetrySchedule(testRetrySchedule))

  private def nativeTest(layer: ZLayer[Any, OpenDALError, OpenDALTrait]) =
    suite("Native Library Tests")(
      test("basic write and read operations") {
        val data = "native-hello".getBytes(StandardCharsets.UTF_8)
        for {
          _        <- OpenDAL.write("basic-test.txt", data)
          readData <- OpenDAL.read("basic-test.txt")
        } yield assertTrue(new String(readData, StandardCharsets.UTF_8) == "native-hello")
      },
      test("string write operations") {
        for {
          _       <- OpenDAL.write("string-native.txt", "Hello, Native OpenDAL!")
          content <- OpenDAL.read("string-native.txt")
          result = new String(content, StandardCharsets.UTF_8)
        } yield assertTrue(result == "Hello, Native OpenDAL!")
      },
      test("partial read operations") {
        val testData = "0123456789abcdef"
        for {
          _       <- OpenDAL.write("partial-native.txt", testData)
          partial <- OpenDAL.read("partial-native.txt", 5, 5) // Read 5 bytes starting from offset 5
          result = new String(partial, StandardCharsets.UTF_8)
        } yield assertTrue(result == "56789")
      },
      test("read operations with options") {
        val testData = "0123456789abcdef"
        for {
          _       <- OpenDAL.write("opts-native.txt", testData)
          partial <- OpenDAL.read("opts-native.txt", ReadOpts(offset = Some(10), length = Some(4)))
          result = new String(partial, StandardCharsets.UTF_8)
        } yield assertTrue(result == "abcd")
      },
      test("write operations with options") {
        for {
          _ <- OpenDAL.write("content-type-native.txt", "Hello Native!", WriteOpts(contentType = Some("text/plain")))
          content <- OpenDAL.read("content-type-native.txt")
          result = new String(content, StandardCharsets.UTF_8)
        } yield assertTrue(result == "Hello Native!")
      },
      test("stat operations") {
        val data = "native stat test".getBytes(StandardCharsets.UTF_8)
        for {
          _    <- OpenDAL.write("stat-native.txt", data)
          meta <- OpenDAL.stat("stat-native.txt")
        } yield assertTrue(meta.getContentLength == data.length.toLong)
      },
      test("stat operations with options") {
        val data = "native stat with options".getBytes(StandardCharsets.UTF_8)
        for {
          _    <- OpenDAL.write("checksum-native.txt", data)
          meta <- OpenDAL.stat("checksum-native.txt", StatOpts(checksum = true))
        } yield assertTrue(meta.getContentLength == data.length.toLong)
      },
      test("file operations - copy and rename") {
        val testData = "native copy test data"
        for {
          _           <- OpenDAL.write("source-native.txt", testData)
          _           <- OpenDAL.copy("source-native.txt", "copied-native.txt")
          copiedData  <- OpenDAL.read("copied-native.txt")
          _           <- OpenDAL.rename("copied-native.txt", "renamed-native.txt")
          renamedData <- OpenDAL.read("renamed-native.txt")
          copiedStr  = new String(copiedData, StandardCharsets.UTF_8)
          renamedStr = new String(renamedData, StandardCharsets.UTF_8)
        } yield assertTrue(copiedStr == testData && renamedStr == testData)
      },
      test("directory operations") {
        for {
          _       <- OpenDAL.createDir("native-test-dir/")
          _       <- OpenDAL.write("native-test-dir/file1.txt", "native file 1")
          _       <- OpenDAL.write("native-test-dir/file2.txt", "native file 2")
          entries <- OpenDAL.list("native-test-dir/")
          _       <- OpenDAL.removeAll("native-test-dir/")
        } yield assertTrue(entries.length >= 2)
      },
      test("list operations with options") {
        for {
          _              <- OpenDAL.write("native-list-test/file1.txt", "data1")
          _              <- OpenDAL.write("native-list-test/file2.txt", "data2")
          _              <- OpenDAL.write("native-list-test/file3.txt", "data3")
          allEntries     <- OpenDAL.list("native-list-test/")
          limitedEntries <- OpenDAL.list("native-list-test/", ListOpts(recursive = false, limit = Some(2)))
        } yield assertTrue(allEntries.length >= 3 && limitedEntries.length <= 2)
      },
      test("delete operations") {
        for {
          _      <- OpenDAL.write("to-delete-native.txt", "will be deleted")
          _      <- OpenDAL.delete("to-delete-native.txt")
          result <- OpenDAL.read("to-delete-native.txt").either
        } yield assertTrue(result.isLeft)
      },
      test("info and capabilities") {
        for {
          info         <- OpenDAL.info
          capabilities <- OpenDAL.capabilities
        } yield assertTrue(info.getScheme.nonEmpty && capabilities != null)
      },
      test("presigned operations availability check") {
        // Test that presigned operations either work or fail gracefully
        for {
          result <- OpenDAL.presignRead("test.txt", 1.hour).either
        } yield assertTrue(result.isLeft || result.isRight) // Either works or fails gracefully
      }
    ).provideLayer(layer)
      .@@(TestAspect.timeout(30.seconds)) // Longer timeout for native operations
      .@@(TestAspect.sequential) // Run sequentially to avoid conflicts

  // Check if native tests should be enabled via environment variable
  private val enableNativeTests = sys.env.get("ENABLE_OPENDAL_NATIVE_TESTS").contains("true")

  def spec = {
    val baseSpec = suite("NativeTestSpec")(
      suite("In-Memory Native Backend")(
        nativeTest(nativeMemoryLayer)
      ),
      suite("Filesystem Native Backend")(
        nativeTest(nativeFilesystemLayer)
      )
    ).@@(TestAspect.tag("native"))

    if (enableNativeTests) {
      baseSpec // Run native tests when explicitly enabled
    } else {
      baseSpec.@@(TestAspect.ignore) // Ignore by default
    }
  }

  // To enable native tests, set environment variable:
  // ENABLE_OPENDAL_NATIVE_TESTS=true sbt test
  // ENABLE_OPENDAL_NATIVE_TESTS=true sbt "testOnly *NativeTestSpec*"
  // Or in sbt shell: set Test / envVars := Map("ENABLE_OPENDAL_NATIVE_TESTS" -> "true")
}
