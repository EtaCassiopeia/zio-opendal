package zio.opendal.integration

import zio._
import zio.test._
import zio.opendal.OpenDAL
import zio.opendal.integration.IntegrationTestUtils._

/**
 * Simple integration test to verify basic S3 functionality with LocalStack
 * This is a minimal test suite that focuses on core operations only.
 */
object SimpleIntegrationSpec extends ZIOSpecDefault {

  def spec: Spec[Any, TestFailure[Any]] = 
    suite("Simple S3 Integration Test")(
      test("basic write and read operation") {
        skipIfDisabled {
          withLocalStack { (endpoint, bucket) =>
            val testContent = "Hello, Integration Test!"
            val testPath = "simple-test.txt"
            
            (for {
              // Write a simple file
              _ <- OpenDAL.writeText(testPath, testContent)
              
              // Read it back
              content <- OpenDAL.readString(testPath)
              
              // Verify file exists
              exists <- OpenDAL.exists(testPath)
              
              // Clean up
              _ <- OpenDAL.delete(testPath)
              
              // Verify it's deleted
              existsAfter <- OpenDAL.exists(testPath)
              
            } yield assertTrue(
              content == testContent &&
              exists == true &&
              existsAfter == false
            )).provideLayer(createOpenDALLayer(endpoint))
          }
        }
      } @@ TestAspect.timeout(60.seconds)
    ) @@ TestAspect.sequential @@ TestAspect.timeout(120.seconds)
}
