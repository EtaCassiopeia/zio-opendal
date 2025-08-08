# Architecture Overview

This document provides a comprehensive overview of the ZIO OpenDAL architecture, design decisions, module structure, and technical implementation details for maintainers.

## Table of Contents

- [High-Level Architecture](#high-level-architecture)
- [Module Structure](#module-structure)
- [Core Components](#core-components)
- [Design Decisions](#design-decisions)
- [Native Integration](#native-integration)
- [Error Handling](#error-handling)
- [Testing Strategy](#testing-strategy)
- [Performance Considerations](#performance-considerations)

## High-Level Architecture

ZIO OpenDAL is a Scala wrapper around the Apache OpenDAL Java bindings, providing a functional, type-safe interface for storage operations across multiple backends.

```
┌─────────────────────────────────────────────────────────────┐
│                    User Application                         │
└─────────────────┬───────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────┐
│                ZIO OpenDAL API                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ OpenDAL.xx  │  │ Syntax Ext  │  │   Error Handling    │  │
│  │ (service)   │  │ (optional)  │  │   (typed errors)    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────┬───────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────┐
│                 Core Abstractions                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ OpenDAL     │  │ Operations  │  │    Configuration    │  │
│  │ Trait       │  │ Traits      │  │    (LayerConfig)    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────┬───────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────┐
│                Implementation Layer                         │
│  ┌─────────────┐  ┌─────────────────────────────────────┐   │
│  │ Native      │  │         Test                        │   │
│  │ OpenDAL     │  │         OpenDAL                     │   │
│  │ (prod)      │  │         (testing)                   │   │
│  └─────────────┘  └─────────────────────────────────────┘   │
└─────────────────┬───────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────┐
│               OpenDAL Java Bindings                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              AsyncOperator                          │    │
│  │   (handles all storage backend interactions)        │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────┬───────────────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────────────┐
│            Storage Backends                                 │
│  ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌─────────────┐    │
│  │  S3   │ │ Azure │ │  GCS  │ │  FS   │ │   Memory    │    │
│  │       │ │ Blob  │ │       │ │       │ │     ...     │    │
│  └───────┘ └───────┘ └───────┘ └───────┘ └─────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Module Structure

### Project Layout

```
zio-opendal/
├── build.sbt                    # Main build configuration
├── project/                     # sbt build definition
│   ├── Dependencies.scala       # Dependency management
│   └── BuildSettings.scala      # Build settings and configuration
├── src/
│   ├── main/
│   │   ├── scala/              # Cross-platform Scala code
│   │   ├── scala-2.13/         # Scala 2.13 specific code
│   │   └── scala-3/            # Scala 3 specific code
│   └── test/
│       └── scala/              # Test code
└── examples/                    # Example applications
    └── src/main/scala/
```

### Package Structure

```scala
zio.opendal
├── OpenDAL                      // Main service object
├── package.scala               // Package-level imports and aliases
├── config/
│   └── LayerConfig             // Configuration management
├── core/
│   ├── OpenDAL                 // Core trait definition
│   ├── BasicOperations         // Read/write operations
│   ├── FileOperations          // File-specific operations  
│   ├── DirectoryOperations     // Directory operations
│   ├── InfoOperations          // Metadata operations
│   └── PresignedOperations     // Pre-signed URL operations
├── impl/
│   ├── NativeOpenDAL           // Native implementation
│   └── TestOpenDAL             // Test implementation
├── error/
│   └── OpenDALError            // Error hierarchy
├── options/
│   ├── ReadOpts                // Read operation options
│   ├── WriteOpts               // Write operation options
│   ├── ListOpts                // List operation options
│   └── StatOpts                // Stat operation options
├── scheme/
│   ├── Scheme                  // Storage scheme definitions
│   └── SchemeConfig            // Scheme-specific configurations
└── syntax/                     // Optional syntax extensions
```

## Core Components

### 1. OpenDAL Trait

The core abstraction that defines all storage operations:

```scala
trait OpenDAL 
  extends BasicOperations
  with FileOperations  
  with DirectoryOperations
  with InfoOperations
  with PresignedOperations
```

**Design Rationale**: 
- Modular design allows for composition and testing
- Each trait focuses on a specific aspect of functionality
- Easy to extend with new operation types
- Maintains clean separation of concerns

### 2. Implementation Layer

#### NativeOpenDAL
- Production implementation using native OpenDAL libraries
- Handles all actual storage backend interactions
- Implements retry logic and error handling
- Manages native resource lifecycle

#### TestOpenDAL  
- In-memory implementation for testing
- No external dependencies
- Deterministic behavior for unit tests
- Fast execution for CI/CD pipelines

### 3. Configuration System

#### LayerConfig
Central configuration management:

```scala
case class LayerConfig(
  scheme: Scheme,
  config: Map[String, String],
  retrySchedule: Schedule[Any, OpenDALError, Any]
) {
  def validate: ZIO[Any, OpenDALError, Unit]
}
```

**Features**:
- Type-safe configuration construction
- Built-in validation
- Support for all OpenDAL backends
- Retry policy configuration

### 4. Error Handling

Hierarchical error system:

```scala
sealed trait OpenDALError extends Throwable
case class NotFoundError(message: String, cause: Option[Throwable]) extends OpenDALError
case class NetworkError(message: String, cause: Option[Throwable]) extends OpenDALError
case class UnauthorizedError(message: String, cause: Option[Throwable]) extends OpenDALError
// ... more specific error types
```

**Benefits**:
- Type-safe error handling
- Structured error information
- Easy error recovery patterns
- Clear error categorization

## Design Decisions

### 1. ZIO as the Effect System

**Decision**: Use ZIO as the primary effect system instead of cats-effect or standard Future

**Rationale**:
- Excellent error handling with typed errors
- Built-in retry and timeout capabilities
- Resource management with acquire/release
- Great testing support
- Performance and composability
- Strong ecosystem integration

**Trade-offs**:
- ✅ Type safety and composability
- ✅ Built-in retry and resource management
- ❌ Learning curve for ZIO newcomers
- ❌ Additional dependency for some projects

### 2. Native Library Integration

**Decision**: Wrap OpenDAL Java bindings rather than create native Scala bindings

**Rationale**:
- Leverage existing, mature OpenDAL implementation
- Automatic support for new storage backends
- Reduced maintenance burden
- Performance benefits of native code

**Trade-offs**:
- ✅ Rapid development and feature parity
- ✅ Performance and reliability
- ❌ Native library deployment complexity
- ❌ Platform-specific binaries required

### 3. Dual Implementation Strategy

**Decision**: Provide both native and test implementations

**Rationale**:
- Enable testing without external dependencies
- Support CI/CD environments without storage setup
- Faster test execution
- Deterministic test behavior

**Implementation**:
```scala
// Production
val layer = OpenDAL.live(config)

// Testing  
val layer = OpenDAL.testLayer(config)
```

### 4. Scala Version Compatibility

**Decision**: Support both Scala 2.13 and Scala 3.x with version-specific optimizations

**Rationale**:
- Broad ecosystem compatibility
- Smooth migration path for users
- Access to latest language features where beneficial

**Implementation Strategy**:
- Common code in `src/main/scala/`
- Version-specific code in `src/main/scala-2.13/` and `src/main/scala-3/`
- Use of package objects for ergonomic differences

### 5. Options Pattern

**Decision**: Use dedicated Options classes instead of many method overloads

**Rationale**:
- Better API discoverability
- Easier to extend with new options
- Cleaner method signatures
- Type-safe option combinations

**Example**:
```scala
// Instead of multiple overloads
def read(path: String, offset: Long = 0, length: Long = -1, bufferSize: Int = 8192): ZIO[...]

// Use options pattern  
def read(path: String, options: ReadOpts = ReadOpts.empty): ZIO[...]
```

## Native Integration

### Library Loading Strategy

```scala
object NativeLibrary {
  def loadLibrary(): Unit = {
    // 1. Try to load from classpath resources
    // 2. Fall back to system library path
    // 3. Allow custom library path via system property
  }
}
```

### Platform Detection

```scala
object Platform {
  lazy val classifier: String = {
    val osName = System.getProperty("os.name").toLowerCase
    val osArch = System.getProperty("os.arch").toLowerCase
    
    (osName, osArch) match {
      case (os, arch) if os.contains("mac") && (arch == "aarch64" || arch == "arm64") => 
        "osx-aarch_64"
      case (os, _) if os.contains("mac") => 
        "osx-x86_64"
      // ... more platform detection
    }
  }
}
```

### Resource Management

```scala
ZLayer.scoped {
  ZIO.acquireRelease(
    ZIO.attemptBlocking(AsyncOperator.of(scheme, config))
  )(op => ZIO.attemptBlocking(op.close()).ignore)
}
```

## Error Handling

### Error Translation Strategy

```scala
def fromThrowable(t: Throwable): OpenDALError = t match {
  case ex if ex.getMessage.contains("NotFound") => 
    NotFoundError(ex.getMessage, Some(ex))
  case ex if ex.getMessage.contains("Unauthorized") => 
    UnauthorizedError(ex.getMessage, Some(ex))
  case ex => 
    GenericError(ex.getMessage, Some(ex))
}
```

### Retry Logic

```scala
class NativeOpenDAL(
  operator: AsyncOperator,
  retrySchedule: Schedule[Any, OpenDALError, Any]
) {
  def read(path: String): ZIO[Any, OpenDALError, Array[Byte]] =
    performOperation(operator.read(path))
      .retry(retrySchedule.filterInput(_.isRetriable))
}
```

## Testing Strategy

### Multi-Level Testing

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test with real storage backends
3. **Property Tests**: Test invariants and edge cases
4. **Example Tests**: Ensure examples compile and run

### Test Implementation Pattern

```scala
object OpenDALSpec extends ZIOSpecDefault {
  def spec = suite("OpenDAL")(
    // Unit tests with test layer
    suite("unit tests")(
      test("should read written data") {
        // ... test implementation
      }
    ).provide(OpenDAL.testLayer(LayerConfig.memory())),
    
    // Integration tests with real backend
    suite("integration tests")(
      test("should work with S3") {
        // ... integration test
      }
    ).provide(realS3Layer) @@ TestAspect.ifEnvSet("RUN_INTEGRATION_TESTS")
  )
}
```

## Performance Considerations

### 1. Async Operations

All operations use OpenDAL's async interface:
```scala
ZIO.attemptBlocking(operator.readAsync(path))
  .flatMap(future => ZIO.fromCompletableFuture(future))
```

### 2. Resource Pooling

Native resources are properly managed:
```scala
ZLayer.scoped {
  ZIO.acquireRelease(createOperator)(cleanupOperator)
}
```

### 3. Streaming for Large Files

Large file operations use streaming:
```scala
def readStream(path: String): ZStream[Any, OpenDALError, Byte] =
  ZStream.fromZIO(read(path)).flatMap(ZStream.fromIterable)
```

### 4. Batch Operations

Support for efficient batch operations:
```scala
def batchRead(paths: List[String]): ZIO[Any, OpenDALError, Map[String, Array[Byte]]] =
  ZIO.foreachPar(paths)(path => read(path).map(path -> _)).map(_.toMap)
```

## Cross-Platform Considerations

### Version-Specific Code

**Scala 2.13**:
```scala
package object opendal {
  // Scala 2.13 specific imports and syntax
  import scala.collection.JavaConverters._
  
  type OpenDALError = zio.opendal.error.OpenDALError
}
```

**Scala 3**:
```scala
package zio.opendal

// Scala 3 specific imports and syntax
import scala.jdk.CollectionConverters.*

type OpenDALError = zio.opendal.error.OpenDALError
```

### Syntax Extensions

Optional syntax extensions for more ergonomic APIs:
```scala
// Scala 3 extension methods
extension (openDAL: OpenDAL.type)
  def readText(path: String): ZIO[OpenDALTrait, OpenDALError, String] =
    openDAL.readString(path)
```

## Deployment Architecture

### Native Library Distribution

```
zio-opendal-core.jar           # Main library code
├── linux-x86_64/
│   └── libopendal_java.so     # Linux native library
├── osx-x86_64/  
│   └── libopendal_java.dylib  # macOS Intel native library
├── osx-aarch64/
│   └── libopendal_java.dylib  # macOS ARM native library
└── windows-x86_64/
    └── opendal_java.dll       # Windows native library
```

### Dependency Management

```scala
// Core library with platform detection
libraryDependencies += "dev.zio" %% "zio-opendal" % version

// Platform-specific native binaries auto-detected
resolvers += Resolver.sonatypeRepo("snapshots")
```

This architecture provides a solid foundation for building a reliable, performant, and maintainable storage abstraction layer while leveraging the power of ZIO and OpenDAL.
