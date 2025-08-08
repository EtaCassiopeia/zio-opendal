# Building and Development

This guide covers how to set up the development environment, build the project, run tests, and work with the ZIO OpenDAL codebase.

## Prerequisites

### Required Software

- **Java Development Kit (JDK) 11 or higher**
  ```bash
  # Check your Java version
  java -version
  javac -version
  ```

- **sbt (Scala Build Tool) 1.9.x or higher**
  ```bash
  # Install via SDKMAN (recommended)
  sdk install sbt
  
  # Or via Homebrew (macOS)
  brew install sbt
  
  # Check sbt version
  sbt --version
  ```

- **Git**
  ```bash
  git --version
  ```

### Optional but Recommended

- **Docker** (for cross-platform testing and native library compatibility)
- **AWS CLI** (for S3 integration testing)
- **Azure CLI** (for Azure Blob integration testing)

## Development Environment Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/zio-opendal.git
cd zio-opendal
```

### 2. Verify Setup

```bash
# This should compile the project and run basic tests
sbt test
```

### 3. IDE Setup

#### IntelliJ IDEA
1. Install the Scala plugin
2. Import the project as an sbt project
3. Configure the project SDK to use JDK 11+
4. Enable auto-import for sbt

#### Visual Studio Code
1. Install the Metals extension
2. Open the project folder
3. Metals should automatically detect the sbt project
4. Import the build when prompted

#### Vim/Neovim
1. Install metals language server
2. Use coc-metals or nvim-metals plugin

## Project Structure

```
zio-opendal/
├── project/                 # sbt build configuration
│   ├── build.properties     # sbt version
│   ├── plugins.sbt          # sbt plugins
│   └── Dependencies.scala   # dependency versions
├── core/                    # Core library module
│   └── src/
│       ├── main/scala/      # Main source code
│       └── test/scala/      # Unit tests
├── examples/                # Example applications
│   └── src/main/scala/
├── native/                  # Native library integration
│   └── src/main/scala/
├── docs/                    # Documentation
│   ├── users/              # User documentation
│   └── maintainers/        # Maintainer documentation
├── scripts/                 # Build and utility scripts
├── build.sbt               # Main build configuration
└── README.md
```

## Building the Project

### Basic Build Commands

```bash
# Clean the build
sbt clean

# Compile all modules
sbt compile

# Compile tests
sbt Test/compile

# Run all tests
sbt test

# Run tests for specific module
sbt core/test

# Create JAR files
sbt package

# Create documentation
sbt doc
```

### Cross-Compilation

The project supports multiple Scala versions:

```bash
# Compile for all supported Scala versions
sbt +compile

# Test all Scala versions
sbt +test

# Publish for all Scala versions
sbt +publishLocal
```

### Assembly (Fat JAR)

```bash
# Create fat JAR for examples
sbt examples/assembly

# Run assembled example
java -jar examples/target/scala-*/examples-assembly-*.jar
```

## Running Tests

### Unit Tests

```bash
# Run all unit tests
sbt test

# Run specific test class
sbt "testOnly *OpenDALServiceSpec"

# Run tests with pattern matching
sbt "testOnly *Service*"

# Run tests continuously (watches for changes)
sbt ~test
```

### Integration Tests

Integration tests use LocalStack to provide S3-compatible storage for realistic testing:

```bash
# Run integration tests with LocalStack
sbt IntegrationTest/test

# Run specific integration test
sbt "IntegrationTest/testOnly *S3IntegrationSpec"

# Run integration tests with Docker Compose
docker-compose -f docker-compose-it.yml up --build -d
docker-compose -f docker-compose-it.yml exec zio-opendal-it sbt IntegrationTest/test
docker-compose -f docker-compose-it.yml down
```

**Prerequisites for integration tests:**
- Docker and Docker Compose installed
- Set `ENABLE_INTEGRATION_TESTS=true` environment variable
- LocalStack container running (or use Docker Compose setup)

**Integration test features:**
- Tests against LocalStack S3-compatible storage
- Comprehensive coverage of all OpenDAL operations
- Automatic setup and teardown of test environment
- Performance and concurrency testing
- Error handling validation

### Test Configuration

Integration tests can be configured via environment variables:

```bash
# S3 integration tests
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export S3_BUCKET="test-bucket"
export AWS_REGION="us-east-1"

# Azure integration tests
export AZURE_STORAGE_ACCOUNT="your-account"
export AZURE_STORAGE_KEY="your-key"
export AZURE_CONTAINER="test-container"

# Run integration tests with real backends
sbt it:test
```

### Docker Testing (Cross-Platform)

For cross-platform testing, especially on Apple Silicon Macs, use Docker to test with Linux AMD64 native libraries:

```bash
# Start Docker testing environment
docker-compose up --build -d

# Run comprehensive test suite in container
docker-compose exec zio-opendal-test ./test-docker.sh

# Run specific tests in container
docker-compose exec zio-opendal-test sbt "testOnly zio.opendal.OpenDALSpec"

# Run native library tests (may crash due to version incompatibility)
docker-compose exec zio-opendal-test bash -c "ENABLE_OPENDAL_NATIVE_TESTS=true sbt 'testOnly zio.opendal.NativeTestSpec'"

# Clean up
docker-compose down
```

**When to use Docker testing:**
- Developing on Apple Silicon with native library compatibility issues
- Validating cross-platform compatibility before CI
- Testing with the same Linux environment as CI/CD
- Debugging platform-specific native library issues

See [Docker Testing Guide](docker-testing.md) for detailed Docker testing documentation.

## Development Workflow

### 1. Making Changes

1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes

3. Run tests to ensure nothing breaks:
   ```bash
   sbt test
   ```

4. Run formatting and linting:
   ```bash
   sbt scalafmtAll
   sbt scalafixAll
   ```

### 2. Testing Your Changes

```bash
# Test specific modules
sbt core/test
sbt examples/test

# Test examples by running them
sbt "project examples" "runMain examples.BasicUsageExample"

# Run integration tests if applicable
sbt it:test
```

### 3. Code Quality Checks

```bash
# Format code
sbt scalafmtAll
sbt scalafmtSbt

# Check formatting (CI check)
sbt scalafmtCheckAll
sbt scalafmtSbtCheck

# Run scalafix
sbt scalafixAll

# Check scalafix rules
sbt "scalafixAll --check"

# Generate coverage reports
sbt coverage test coverageReport
```

## Adding Dependencies

### 1. Edit Dependencies.scala

```scala
// project/Dependencies.scala
object Dependencies {
  val zio = "2.0.19"
  val newLibrary = "1.0.0"  // Add your version
  
  val zioCore = "dev.zio" %% "zio" % zio
  val newLib = "org.example" %% "new-library" % newLibrary  // Add dependency
}
```

### 2. Update build.sbt

```scala
// build.sbt
lazy val core = project
  .in(file("core"))
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.zioCore,
      Dependencies.newLib  // Add to module
    )
  )
```

### 3. Test the Addition

```bash
sbt reload
sbt compile
```

## Working with Native Libraries

The project integrates with OpenDAL's native libraries:

### 1. Native Library Location

Native libraries are loaded from:
- `src/main/resources/native/` in the classpath
- System library path
- Custom path via `opendal.native.path` system property

### 2. Building with Native Libraries

```bash
# Build with native library debugging
sbt -Dopendal.native.debug=true compile

# Use custom native library path
sbt -Dopendal.native.path=/path/to/libs compile
```

### 3. Adding New Native Bindings

1. Update the native bindings in `native/src/main/scala/`
2. Ensure native libraries are compatible
3. Update tests to cover new functionality
4. Document the new features

## Debugging and Troubleshooting

### Common Issues

1. **Native Library Loading Errors**
   ```bash
   # Enable native debugging
   sbt -Dopendal.native.debug=true run
   
   # Check library path
   sbt -Djava.library.path=/path/to/native/libs run
   ```

2. **Out of Memory Errors**
   ```bash
   # Increase heap size
   export SBT_OPTS="-Xmx4G -Xms1G"
   sbt test
   ```

3. **Compilation Errors with Scala Versions**
   ```bash
   # Clean and recompile for specific version
   sbt ++2.13.12 clean compile
   ```

### Debug Mode

Enable debug logging for development:

```scala
// In your application
System.setProperty("zio.opendal.debug", "true")
```

### Memory Profiling

```bash
# Run with JVM profiling
sbt -J-XX:+UnlockExperimentalVMOptions \
    -J-XX:+UseZGC \
    -J-Xmx4G \
    test
```

## Performance Testing

### Benchmarking

```bash
# Run performance benchmarks
sbt bench/jmh:run

# Run specific benchmark
sbt "bench/jmh:run .*ReadWriteBenchmark.*"

# Profile with specific JVM options
sbt -J-Xmx8G bench/jmh:run
```

### Load Testing

```bash
# Run examples with large datasets
sbt "examples/runMain examples.BatchProcessing"

# Monitor resource usage
sbt -J-XX:+UseG1GC -J-Xmx4G examples/run
```

## Documentation Generation

### API Documentation

```bash
# Generate Scaladoc
sbt doc

# View generated docs
open core/target/scala-*/api/index.html
```

### Documentation Website

```bash
# Build documentation site (if using mdoc/docusaurus)
sbt docs/mdoc

# Serve documentation locally
cd docs && npm start
```

## Continuous Integration

The project uses GitHub Actions for CI. Local CI simulation:

```bash
# Run the same checks as CI
sbt scalafmtCheckAll scalafixAll test it:test

# Cross-compile like CI
sbt +test
```

### Pre-commit Hooks

Set up pre-commit hooks to catch issues early:

```bash
# Create .git/hooks/pre-commit
#!/bin/bash
sbt scalafmtCheckAll scalafixAll --check && sbt test
```

## IDE Configuration

### IntelliJ IDEA Settings

1. **Code Style**: Import `.scalafmt.conf` settings
2. **Inspections**: Enable Scala-specific inspections
3. **Compiler**: Use sbt shell for imports and builds
4. **Run Configurations**: Create configurations for tests and examples

### VS Code Settings

Create `.vscode/settings.json`:

```json
{
  "files.watcherExclude": {
    "**/target/**": true
  },
  "metals.bloopSbtAlreadyInstalled": true,
  "metals.enableStripMarginOnTypeFormatting": true
}
```

## Profiling and Monitoring

### Memory Usage

```bash
# Monitor memory usage during tests
sbt -J-XX:+PrintGCDetails test

# Generate heap dumps
sbt -J-XX:+HeapDumpOnOutOfMemoryError test
```

### Performance Monitoring

```bash
# Enable JFR profiling
sbt -J-XX:+FlightRecorder \
    -J-XX:StartFlightRecording=duration=60s,filename=recording.jfr \
    test
```

This development guide should help you get started with building and developing ZIO OpenDAL. For specific questions about the codebase architecture, see the [Architecture Overview](architecture.md) document.
