package examples

import zio._
import zio.opendal.config.LayerConfig

/** Example showing how to configure different cloud storage backends */
object CloudStorageExample extends ZIOAppDefault {

  def run = for {
    _ <- Console.printLine("=== Cloud Storage Configuration Examples ===")
    _ <- s3Example
    _ <- azureExample  
    _ <- gcsExample
    _ <- Console.printLine("\n=== Configuration examples completed ===")
  } yield ()

  // S3 configuration example
  def s3Example = for {
    _ <- Console.printLine("\n1. AWS S3 Configuration:")
    
    // Basic S3 configuration
    s3Config = LayerConfig.s3(
      bucket = "my-bucket",
      region = "us-east-1", 
      accessKeyId = "AKIAIOSFODNN7EXAMPLE",
      secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
    )
    _ <- Console.printLine(s"S3 Config: ${s3Config.config}")
    
    // S3-compatible storage (MinIO, DigitalOcean Spaces, etc.)
    minioConfig = LayerConfig.s3Compatible(
      bucket = "my-bucket",
      region = "us-east-1",
      accessKeyId = "minio-access-key",
      secretAccessKey = "minio-secret-key", 
      endpoint = "http://localhost:9000"
    )
    _ <- Console.printLine(s"MinIO Config: ${minioConfig.config}")
    
  } yield ()

  // Azure Blob Storage configuration example  
  def azureExample = for {
    _ <- Console.printLine("\n2. Azure Blob Storage Configuration:")
    
    azureConfig = LayerConfig.azureBlob(
      container = "my-container",
      accountName = "mystorageaccount",
      accountKey = "YourStorageAccountKey=="
    )
    _ <- Console.printLine(s"Azure Config: ${azureConfig.config}")
    
  } yield ()

  // Google Cloud Storage configuration example
  def gcsExample = for {
    _ <- Console.printLine("\n3. Google Cloud Storage Configuration:")
    
    gcsConfig = LayerConfig.gcs(
      bucket = "my-gcs-bucket",
      serviceAccount = "/path/to/service-account.json",
      projectId = "my-gcp-project"
    )
    _ <- Console.printLine(s"GCS Config: ${gcsConfig.config}")
    
  } yield ()

  // Example of using a configured layer (commented out as it requires real credentials)
  /*
  def liveS3Example = {
    val s3Config = LayerConfig.s3(
      bucket = sys.env("AWS_BUCKET"),
      region = sys.env("AWS_REGION"),
      accessKeyId = sys.env("AWS_ACCESS_KEY_ID"),
      secretAccessKey = sys.env("AWS_SECRET_ACCESS_KEY")
    )
    
    val s3Layer = OpenDAL.live(s3Config)
    
    val program = for {
      _ <- OpenDAL.writeText("example.txt", "Hello from S3!")
      content <- OpenDAL.readString("example.txt")
      _ <- Console.printLine(s"Content from S3: $content")
      _ <- OpenDAL.delete("example.txt")
    } yield ()
    
    program.provide(s3Layer)
  }
  */
}
