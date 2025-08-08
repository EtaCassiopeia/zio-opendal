# Configuration Guide

This guide covers all supported storage services and how to configure them with ZIO OpenDAL.

## Configuration Basics

ZIO OpenDAL uses `LayerConfig` for type-safe configuration with built-in validation:

```scala
import zio.opendal.config.LayerConfig

// Basic configuration
val config = LayerConfig(
  scheme = scheme,           // Storage backend type
  config = Map[String, String](), // Backend-specific configuration
  retrySchedule = ...,       // Retry policy (optional)
  enableNativeAccess = true, // Use native libraries (optional)
  platformClassifier = None  // Platform override (optional)
)

// Create a layer
val layer = OpenDAL.live(config)
```

## Supported Services

### Cloud Storage

#### AWS S3

**Standard S3:**
```scala
val s3Config = LayerConfig.s3(
  bucket = "my-bucket",
  region = "us-east-1",
  accessKeyId = sys.env("AWS_ACCESS_KEY_ID"),
  secretAccessKey = sys.env("AWS_SECRET_ACCESS_KEY")
)
```

**S3 with custom endpoint (MinIO, DigitalOcean Spaces, etc.):**
```scala
val minioConfig = LayerConfig.s3Compatible(
  bucket = "my-bucket",
  region = "us-east-1", 
  accessKeyId = "minio-user",
  secretAccessKey = "minio-password",
  endpoint = "http://localhost:9000"
)
```

**Advanced S3 configuration:**
```scala
val s3Config = LayerConfig(
  scheme = CloudStorage.S3,
  config = Map(
    "bucket" -> "my-bucket",
    "region" -> "us-west-2", 
    "access_key_id" -> "...",
    "secret_access_key" -> "...",
    "endpoint" -> "https://custom-endpoint.com",
    "virtual_hosted_style_request" -> "false",
    "server_side_encryption" -> "AES256",
    "sse_kms_key_id" -> "arn:aws:kms:...",
    "default_storage_class" -> "STANDARD_IA"
  )
)
```

#### Azure Blob Storage

**Basic Azure Blob:**
```scala
val azureConfig = LayerConfig.azureBlob(
  container = "my-container",
  accountName = "mystorageaccount",
  accountKey = sys.env("AZURE_STORAGE_KEY")
)
```

**Azure with connection string:**
```scala
val azureConfig = LayerConfig(
  scheme = CloudStorage.Azblob,
  config = Map(
    "container" -> "my-container",
    "connection_string" -> sys.env("AZURE_STORAGE_CONNECTION_STRING")
  )
)
```

**Azure with SAS token:**
```scala
val azureConfig = LayerConfig(
  scheme = CloudStorage.Azblob,
  config = Map(
    "container" -> "my-container",
    "account_name" -> "mystorageaccount",
    "sas_token" -> sys.env("AZURE_SAS_TOKEN")
  )
)
```

#### Google Cloud Storage

**Basic GCS:**
```scala
val gcsConfig = LayerConfig.gcs(
  bucket = "my-gcs-bucket",
  serviceAccount = "/path/to/service-account.json",
  projectId = "my-gcp-project"
)
```

**GCS with ADC (Application Default Credentials):**
```scala
val gcsConfig = LayerConfig(
  scheme = CloudStorage.Gcs,
  config = Map(
    "bucket" -> "my-bucket",
    "project_id" -> "my-project"
    // ADC will be used automatically
  )
)
```

**GCS with service account key:**
```scala
val gcsConfig = LayerConfig(
  scheme = CloudStorage.Gcs,
  config = Map(
    "bucket" -> "my-bucket", 
    "project_id" -> "my-project",
    "credential" -> serviceAccountJson,
    "predefined_acl" -> "publicRead"
  )
)
```

#### Other Cloud Providers

**Alibaba Cloud OSS:**
```scala
val ossConfig = LayerConfig(
  scheme = CloudStorage.Oss,
  config = Map(
    "bucket" -> "my-bucket",
    "endpoint" -> "oss-cn-hangzhou.aliyuncs.com",
    "access_key_id" -> "...",
    "secret_access_key" -> "..."
  )
)
```

**Tencent Cloud COS:**
```scala
val cosConfig = LayerConfig(
  scheme = CloudStorage.Cos,
  config = Map(
    "bucket" -> "my-bucket-1234567890",
    "region" -> "ap-beijing", 
    "secret_id" -> "...",
    "secret_key" -> "..."
  )
)
```

**Backblaze B2:**
```scala
val b2Config = LayerConfig(
  scheme = CloudStorage.B2,
  config = Map(
    "bucket" -> "my-bucket",
    "application_key_id" -> "...",
    "application_key" -> "..."
  )
)
```

### Local Storage

#### Filesystem

**Basic filesystem:**
```scala
val fsConfig = LayerConfig.filesystem("/path/to/storage")
```

**Filesystem with options:**
```scala
val fsConfig = LayerConfig(
  scheme = LocalStorage.Fs,
  config = Map(
    "root" -> "/path/to/storage",
    "atomic_write_dir" -> "/tmp/atomic-writes",
    "enable_versioning" -> "true"
  )
)
```

#### In-Memory Storage

Perfect for testing and caching:

```scala
val memoryConfig = LayerConfig.memory()

// Or with custom settings
val memoryConfig = LayerConfig(
  scheme = LocalStorage.Memory,
  config = Map(
    "enable_versioning" -> "true"
  )
)
```

### Databases

#### Redis

**Basic Redis:**
```scala
val redisConfig = LayerConfig(
  scheme = Database.Redis,
  config = Map(
    "connection_string" -> "redis://localhost:6379",
    "db" -> "0"
  )
)
```

**Redis with authentication:**
```scala
val redisConfig = LayerConfig(
  scheme = Database.Redis,
  config = Map(
    "connection_string" -> "redis://username:password@localhost:6379",
    "db" -> "0",
    "key_prefix" -> "myapp:"
  )
)
```

#### PostgreSQL

```scala
val postgresConfig = LayerConfig(
  scheme = Database.Postgresql,
  config = Map(
    "connection_string" -> "postgresql://user:password@localhost:5432/dbname",
    "table" -> "opendal_kv",
    "key_field" -> "key",
    "value_field" -> "value"
  )
)
```

#### MySQL

```scala
val mysqlConfig = LayerConfig(
  scheme = Database.Mysql,
  config = Map(
    "connection_string" -> "mysql://user:password@localhost:3306/dbname",
    "table" -> "opendal_kv",
    "key_field" -> "path",
    "value_field" -> "content"
  )
)
```

#### MongoDB

```scala
val mongoConfig = LayerConfig(
  scheme = Database.Mongodb,
  config = Map(
    "connection_string" -> "mongodb://localhost:27017",
    "database" -> "myapp",
    "collection" -> "files"
  )
)
```

### Specialized Services

#### GitHub

Store files in GitHub repositories:

```scala
val githubConfig = LayerConfig(
  scheme = Specialized.GitHub,
  config = Map(
    "owner" -> "my-username",
    "repo" -> "my-repo",
    "token" -> sys.env("GITHUB_TOKEN"),
    "branch" -> "main"
  )
)
```

#### Dropbox

```scala
val dropboxConfig = LayerConfig(
  scheme = Specialized.Dropbox,
  config = Map(
    "access_token" -> sys.env("DROPBOX_ACCESS_TOKEN"),
    "refresh_token" -> sys.env("DROPBOX_REFRESH_TOKEN"),
    "client_id" -> "...",
    "client_secret" -> "..."
  )
)
```

#### OneDrive

```scala
val onedriveConfig = LayerConfig(
  scheme = Specialized.Onedrive,
  config = Map(
    "access_token" -> sys.env("ONEDRIVE_ACCESS_TOKEN"),
    "refresh_token" -> sys.env("ONEDRIVE_REFRESH_TOKEN")
  )
)
```

#### HDFS

```scala
val hdfsConfig = LayerConfig(
  scheme = Specialized.Hdfs,
  config = Map(
    "name_node" -> "hdfs://namenode:8020",
    "user" -> "hadoop"
  )
)
```

#### WebDAV

```scala
val webdavConfig = LayerConfig(
  scheme = Specialized.Webdav,
  config = Map(
    "endpoint" -> "https://webdav.example.com",
    "username" -> "user",
    "password" -> "password"
  )
)
```

#### HTTP

Read-only HTTP access:

```scala
val httpConfig = LayerConfig(
  scheme = Specialized.Http,
  config = Map(
    "endpoint" -> "https://api.example.com/files/"
  )
)
```

## Advanced Configuration Options

### Retry Policies

Configure retry behavior for resilient applications:

```scala
import zio.Schedule

// Exponential backoff with max attempts
val basicRetry = LayerConfig.s3(...)
  .withRetrySchedule(
    Schedule.exponential(100.millis) && Schedule.recurs(3)
  )

// Complex retry policy
val advancedRetry = LayerConfig.s3(...)
  .withRetrySchedule(
    Schedule.exponential(100.millis, 2.0) &&  // Exponential backoff
    Schedule.recurs(5) &&                     // Max 5 retries
    Schedule.whileOutput(_ < 30.seconds)      // Total timeout
  )

// Disable retry
val noRetry = LayerConfig.s3(...).withoutRetry
```

### Native Library Configuration

Control native library usage:

```scala
// Enable native access (default)
val nativeConfig = LayerConfig.s3(...)
  .withNativeAccess(true)

// Disable native access (test mode)
val testConfig = LayerConfig.s3(...)
  .withNativeAccess(false)

// Override platform detection
val customPlatform = LayerConfig.s3(...)
  .withPlatformClassifier("linux-aarch_64")
```

### Platform Classifiers

Supported platform classifiers:
- `osx-x86_64` - macOS Intel
- `osx-aarch_64` - macOS Apple Silicon
- `linux-x86_64` - Linux x64
- `linux-aarch_64` - Linux ARM64
- `windows-x86_64` - Windows x64

### Configuration Validation

LayerConfig includes built-in validation:

```scala
val config = LayerConfig.s3(
  bucket = "my-bucket",
  region = "us-east-1", 
  accessKeyId = "",  // This will fail validation
  secretAccessKey = ""
)

// Validation happens when creating the layer
val layer = OpenDAL.live(config) // Will fail with InvalidConfigError
```

## Environment-Based Configuration

### Using Environment Variables

```scala
// Simple environment loading
val s3Config = LayerConfig.s3(
  bucket = sys.env.getOrElse("S3_BUCKET", "default-bucket"),
  region = sys.env.getOrElse("AWS_REGION", "us-east-1"),
  accessKeyId = sys.env("AWS_ACCESS_KEY_ID"),
  secretAccessKey = sys.env("AWS_SECRET_ACCESS_KEY")
)

// ZIO Environment loading
val configFromEnv = for {
  bucket <- System.env("S3_BUCKET")
  region <- System.env("AWS_REGION")
  accessKey <- System.env("AWS_ACCESS_KEY_ID") 
  secretKey <- System.env("AWS_SECRET_ACCESS_KEY")
} yield LayerConfig.s3(
  bucket = bucket.getOrElse("default-bucket"),
  region = region.getOrElse("us-east-1"),
  accessKeyId = accessKey.getOrElse(""),
  secretAccessKey = secretKey.getOrElse("")
)

val dynamicLayer = ZLayer.fromZIO(configFromEnv.map(OpenDAL.live))
```

### Configuration Files

```scala
// Using ZIO Config for HOCON/JSON configuration
import zio.config._
import zio.config.typesafe._

case class StorageConfig(
  provider: String,
  bucket: String,
  region: String,
  accessKeyId: String,
  secretAccessKey: String
)

val configDescriptor = descriptor[StorageConfig]

val layerFromConfig = for {
  config <- read(configDescriptor from TypesafeConfigProvider.fromResourcePath())
  layerConfig = config.provider match {
    case "s3" => LayerConfig.s3(config.bucket, config.region, config.accessKeyId, config.secretAccessKey)
    case "azure" => LayerConfig.azureBlob(config.bucket, config.region, config.secretAccessKey)
    case _ => throw new IllegalArgumentException(s"Unknown provider: ${config.provider}")
  }
} yield OpenDAL.live(layerConfig)
```

## Testing Configuration

### Test Layer

For development and testing, use the test layer:

```scala
// In-memory test layer
val testLayer = OpenDAL.testLayer(LayerConfig.memory())

// Filesystem test layer
val fsTestLayer = OpenDAL.testLayer(LayerConfig.filesystem("/tmp/test-data"))

// Mock with specific scheme
val mockS3Layer = OpenDAL.testLayer(LayerConfig.s3("test-bucket", "us-east-1", "test", "test"))
```

### Configuration Best Practices

1. **Use environment variables** for credentials and deployment-specific settings
2. **Use test layers** for development and unit tests
3. **Validate configuration early** during application startup
4. **Set appropriate retry policies** for production resilience
5. **Use platform auto-detection** unless you have specific requirements

### Security Considerations

1. **Never commit credentials** to version control
2. **Use environment variables** or secret management systems
3. **Rotate credentials regularly** 
4. **Use IAM roles** when possible (AWS, GCP)
5. **Limit permissions** to the minimum required
6. **Enable audit logging** on storage backends

## Troubleshooting

### Common Configuration Issues

**Invalid credentials:**
```scala
// Error: UnauthorizedError
// Solution: Check credentials and permissions
val config = LayerConfig.s3(
  bucket = "my-bucket",
  region = "us-east-1",
  accessKeyId = "CORRECT_ACCESS_KEY",
  secretAccessKey = "CORRECT_SECRET_KEY"
)
```

**Wrong endpoint:**
```scala
// Error: NetworkError  
// Solution: Verify endpoint URL
val config = LayerConfig.s3Compatible(
  bucket = "my-bucket",
  region = "us-east-1",
  accessKeyId = "...",
  secretAccessKey = "...",
  endpoint = "http://localhost:9000" // Check this is correct
)
```

**Missing native libraries:**
```scala
// Error: UnsupportedOperationError
// Solution: Use test layer for development
val testConfig = OpenDAL.testLayer(LayerConfig.memory())
// Or ensure native libraries are available for production
```

### Platform-Specific Issues

**Docker containers:**
```scala
// Use linux-x86_64 classifier in containers
val config = LayerConfig.s3(...)
  .withPlatformClassifier("linux-x86_64")
```

**Apple Silicon Macs:**
```scala
// Auto-detection should work, but can be overridden
val config = LayerConfig.s3(...)
  .withPlatformClassifier("osx-aarch_64")
```

### Debug Configuration

```scala
// Enable detailed logging
val config = LayerConfig.s3(...)
  .withRetrySchedule(
    Schedule.exponential(100.millis) && Schedule.recurs(3)
  )

// Test configuration without network calls
val testLayer = OpenDAL.testLayer(config) // Uses mock implementation
```

## Migration Guide

### From Version 0.x to 1.x

```scala
// Old API
val layer = OpenDAL.live(Scheme.S3, Map("bucket" -> "my-bucket", ...))

// New API  
val layer = OpenDAL.live(LayerConfig.s3("my-bucket", "us-east-1", "...", "..."))
```

### From Other Libraries

**From AWS SDK:**
```scala
// AWS SDK
val s3Client = S3Client.builder()
  .region(Region.US_EAST_1)
  .credentialsProvider(DefaultCredentialsProvider.create())
  .build()

// ZIO OpenDAL
val s3Config = LayerConfig.s3(
  bucket = "my-bucket",
  region = "us-east-1", 
  accessKeyId = sys.env("AWS_ACCESS_KEY_ID"),
  secretAccessKey = sys.env("AWS_SECRET_ACCESS_KEY")
)
val s3Layer = OpenDAL.live(s3Config)
```

This configuration guide covers all major storage backends and configuration options. For specific use cases not covered here, check the [examples](examples.md) or [API reference](api-reference.md).
