package zio.opendal.integration

import zio._
import zio.test._
import zio.opendal.OpenDAL
import zio.opendal.config.LayerConfig
import zio.opendal.scheme.CloudStorage
import zio.opendal.error.OpenDALError
import com.dimafeng.testcontainers.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}
import software.amazon.awssdk.services.s3.model._
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import java.net.URI
import scala.jdk.CollectionConverters._

/**
 * Integration test utilities for testing ZIO OpenDAL with LocalStack
 */
object IntegrationTestUtils {

  /** Environment variable to enable integration tests */
  val INTEGRATION_TESTS_ENABLED = "ENABLE_INTEGRATION_TESTS"
  
  /** Default LocalStack configuration */
  object LocalStackConfig {
    val region = "us-east-1"
    val accessKeyId = "test"
    val secretAccessKey = "test"
    val bucketName = "zio-opendal-test-bucket"
    val localstackPort = 4566
  }

  /** Check if integration tests are enabled */
  def integrationTestsEnabled: Boolean = 
    sys.env.get(INTEGRATION_TESTS_ENABLED).contains("true")

  /** 
   * Create a LocalStack container for integration tests 
   */
  def createLocalStackContainer(): LocalStackContainer = {
    val container = LocalStackContainer(
      DockerImageName.parse("localstack/localstack:latest"),
      services = List(Service.S3, Service.IAM)
    )
    
    container.container.withEnv("DEBUG", "1")
    container.container.withEnv("AWS_DEFAULT_REGION", LocalStackConfig.region)
    container.container.withEnv("AWS_ACCESS_KEY_ID", LocalStackConfig.accessKeyId)
    container.container.withEnv("AWS_SECRET_ACCESS_KEY", LocalStackConfig.secretAccessKey)
    container.container.withEnv("S3_SKIP_SIGNATURE_VALIDATION", "1")
    container.container.withEnv("S3_FORCE_PATH_STYLE", "1")
    
    container
  }

  /**
   * Create an AWS S3 client configured for LocalStack
   */
  def createS3Client(localstackEndpoint: String): S3Client = {
    val credentials = StaticCredentialsProvider.create(
      AwsBasicCredentials.create(LocalStackConfig.accessKeyId, LocalStackConfig.secretAccessKey)
    )

    S3Client.builder()
      .endpointOverride(URI.create(localstackEndpoint))
      .region(Region.of(LocalStackConfig.region))
      .credentialsProvider(credentials)
      .serviceConfiguration(
        S3Configuration.builder()
          .pathStyleAccessEnabled(true)
          .build()
      )
      .build()
  }

  /**
   * Set up S3 bucket and test data in LocalStack
   */
  def setupS3Bucket(s3Client: S3Client, bucketName: String): ZIO[Any, Throwable, Unit] = {
    ZIO.attempt {
      // Create bucket if it doesn't exist
      val bucketExists = try {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build())
        true
      } catch {
        case _: NoSuchBucketException => false
      }

      if (!bucketExists) {
        // For us-east-1, AWS S3 expects no location constraint
        val createBucketRequestBuilder = CreateBucketRequest.builder().bucket(bucketName)
        
        val createBucketRequest = if (LocalStackConfig.region == "us-east-1") {
          createBucketRequestBuilder.build() // No createBucketConfiguration for us-east-1
        } else {
          createBucketRequestBuilder
            .createBucketConfiguration(
              CreateBucketConfiguration.builder()
                .locationConstraint(BucketLocationConstraint.fromValue(LocalStackConfig.region))
                .build()
            )
            .build()
        }
        
        val _ = s3Client.createBucket(createBucketRequest)
      }

      // Create some test directories and files
      val testFiles = Map(
        "test-data/hello.txt" -> "Hello, ZIO OpenDAL Integration Tests!",
        "test-data/sample.json" -> """{"message": "Integration test data", "timestamp": "2024-01-01T00:00:00Z"}""",
        "integration-tests/readme.md" -> "# Integration Test Files\n\nThis directory contains test files for integration testing.",
        "temp/placeholder.txt" -> "This is a placeholder file in the temp directory."
      )

      testFiles.foreach { case (key, content) =>
        s3Client.putObject(
          PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(if (key.endsWith(".json")) "application/json" else "text/plain")
            .build(),
          software.amazon.awssdk.core.sync.RequestBody.fromString(content)
        )
      }
    }
  }

  /**
   * Create OpenDAL layer configuration for LocalStack S3
   */
  def createOpenDALLayer(localstackEndpoint: String): ZLayer[Any, OpenDALError, zio.opendal.core.OpenDAL] = {
    val config = LayerConfig(
      scheme = CloudStorage.S3,
      config = Map(
        "bucket" -> LocalStackConfig.bucketName,
        "region" -> LocalStackConfig.region,
        "access_key_id" -> LocalStackConfig.accessKeyId,
        "secret_access_key" -> LocalStackConfig.secretAccessKey,
        "endpoint" -> localstackEndpoint,
        "enable_path_style" -> "true"
      )
    )
    
    // Use live layer for real S3 integration testing
    OpenDAL.live(config)
  }

  /**
   * Create a test environment with LocalStack container
   */
  def withLocalStack[R, E, A](
    test: (String, String) => ZIO[R, E, A]
  ): ZIO[R, E, A] = {
    ZIO.scoped {
      for {
        container <- ZIO.acquireRelease(
          ZIO.attempt(createLocalStackContainer()).orDie
        )(container => ZIO.attempt(container.stop()).orDie)
        
        _ <- ZIO.attempt(container.start()).orDie
        
        endpoint = s"http://${container.host}:${container.mappedPort(LocalStackConfig.localstackPort)}"
        
        // Set up S3 bucket and test data
        s3Client <- ZIO.attempt(createS3Client(endpoint)).orDie
        _ <- setupS3Bucket(s3Client, LocalStackConfig.bucketName).orDie
        _ <- ZIO.attempt(s3Client.close()).orDie
        _ <- ZIO.attemptBlocking(Thread.sleep(1000)).orDie // Give LocalStack time to process
        
        result <- test(endpoint, LocalStackConfig.bucketName)
      } yield result
    }
  }

  /**
   * Skip test if integration tests are not enabled
   */
  def skipIfDisabled[R](test: => ZIO[R, Any, TestResult]): ZIO[R, TestFailure[Any], TestResult] = {
    if (integrationTestsEnabled) {
      test.mapError(TestFailure.fail)
    } else {
      ZIO.succeed(assertTrue(true))
    }
  }

  /**
   * Test specification for integration tests that can be skipped
   */
  def integrationSpec[R](
    name: String
  )(
    spec: => Spec[R, TestFailure[Any]]
  ): Spec[R, TestFailure[Any]] = {
    if (integrationTestsEnabled) {
      suite(name)(spec) @@ TestAspect.timeout(60.seconds) @@ TestAspect.sequential
    } else {
      suite(name)(
        zio.test.test("Integration tests disabled") {
          ZIO.succeed(assertTrue(true))
        } @@ TestAspect.ignore
      )
    }
  }

  /**
   * Common test data and utilities
   */
  object TestData {
    val smallText = "Hello, World!"
    val mediumText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " * 50
    val largeText = "Large text content for testing. " * 1000
    
    val jsonData = """{"name": "ZIO OpenDAL", "version": "0.1.0", "description": "Integration test data"}"""
    
    val binaryData = Array.range(0, 256).map(_.toByte)
    
    val testFiles = Map(
      "small.txt" -> smallText,
      "medium.txt" -> mediumText,
      "large.txt" -> largeText,
      "data.json" -> jsonData
    )
  }

  /**
   * Verify file content matches expected value
   */
  def verifyFileContent(path: String, expectedContent: String): ZIO[zio.opendal.core.OpenDAL, OpenDALError, TestResult] = {
    for {
      actualContent <- OpenDAL.readString(path)
    } yield assertTrue(actualContent == expectedContent)
  }

  /**
   * Verify file exists
   */
  def verifyFileExists(path: String): ZIO[zio.opendal.core.OpenDAL, OpenDALError, TestResult] = {
    for {
      exists <- OpenDAL.exists(path)
    } yield assertTrue(exists)
  }

  /**
   * Verify file does not exist
   */
  def verifyFileNotExists(path: String): ZIO[zio.opendal.core.OpenDAL, OpenDALError, TestResult] = {
    for {
      exists <- OpenDAL.exists(path)
    } yield assertTrue(!exists)
  }

  /**
   * Create temporary file for testing
   */
  def createTempFile(content: String): ZIO[zio.opendal.core.OpenDAL, OpenDALError, String] = {
    val path = s"temp/test-${java.util.UUID.randomUUID()}.txt"
    OpenDAL.writeText(path, content).as(path)
  }

  /**
   * Clean up test files
   */
  def cleanup(paths: List[String]): ZIO[zio.opendal.core.OpenDAL, Nothing, Unit] = {
    ZIO.foreach(paths) { path =>
      OpenDAL.delete(path).ignore
    }.unit
  }

  /**
   * Retry helper for flaky operations
   */
  def retryFlaky[R, E, A](operation: ZIO[R, E, A]): ZIO[R, E, A] = {
    operation.retry(
      Schedule.exponential(100.millis) && Schedule.recurs(3)
    )
  }

  /**
   * Performance measurement helper
   */
  def timeOperation[R, E, A](name: String, operation: ZIO[R, E, A]): ZIO[R, E, A] = {
    for {
      start <- Clock.nanoTime
      result <- operation
      end <- Clock.nanoTime
      duration = (end - start) / 1000000 // Convert to milliseconds
      _ <- Console.printLine(s"$name took ${duration}ms").orDie
    } yield result
  }
}
