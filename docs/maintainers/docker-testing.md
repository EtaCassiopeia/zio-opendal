# Docker Testing Guide

This guide explains how to use Docker for cross-platform testing of ZIO OpenDAL, particularly useful for Apple Silicon (M1/M2/M3) Mac users who need to test with Linux x86_64 native libraries.

## Why Use Docker Testing?

### Platform Compatibility Issues
- **Apple Silicon Macs**: The OpenDAL native libraries may not be available for `osx-aarch_64`
- **Cross-platform validation**: Test your code on the same Linux platform used in CI/CD
- **Native library conflicts**: Avoid JVM crashes from version mismatches between local and target platforms
- **Consistent environment**: Ensure the same Java version and platform as production

### When to Use Docker Testing
- You're developing on Apple Silicon and need to test native library integration
- You want to verify cross-platform compatibility before pushing to CI
- Local native tests are failing due to platform-specific library issues
- You need to test with specific Linux AMD64 native library classifiers

## Quick Start

### Prerequisites
- Docker Desktop installed and running
- Docker Compose available (included with Docker Desktop)

### Basic Usage

1. **Start the testing environment:**
   ```bash
   docker-compose up --build -d
   ```

2. **Run tests inside the container:**
   ```bash
   docker-compose exec zio-opendal-test ./test-docker.sh
   ```

3. **Clean up when done:**
   ```bash
   docker-compose down
   ```

## Detailed Usage

### Environment Setup

The Docker environment provides:
- **Platform**: Linux AMD64 (even on Apple Silicon hosts)
- **Java**: OpenJDK 17 with native access enabled
- **SBT**: Version 1.11.4 (matches project requirements)
- **Native libraries**: Correct `linux-x86_64` classifier for OpenDAL
- **Caching**: Persistent volumes for SBT, Ivy, and Coursier caches

### Available Testing Commands

#### 1. Comprehensive Test Suite
```bash
docker-compose exec zio-opendal-test ./test-docker.sh
```

This script runs:
- Mock tests using `OpenDAL.testLayer()` (always work)
- Native tests using `OpenDAL.live()` with actual native libraries
- Platform detection and compatibility checks
- Detailed test result summary

#### 2. Integration Tests with LocalStack
```bash
# Run integration tests using Docker Compose
docker-compose -f docker-compose-it.yml up --build -d
docker-compose -f docker-compose-it.yml exec zio-opendal-it sbt IntegrationTest/test
docker-compose -f docker-compose-it.yml down
```

Integration tests provide:
- Real S3-compatible storage via LocalStack
- Comprehensive API testing
- Performance and concurrency validation
- Error handling verification

#### 3. Individual Test Commands

**Mock tests only** (recommended for most development):
```bash
docker-compose exec zio-opendal-test sbt "clean; testOnly zio.opendal.OpenDALSpec"
```

**Native tests** (requires compatible OpenDAL versions):
```bash
docker-compose exec zio-opendal-test bash -c "ENABLE_OPENDAL_NATIVE_TESTS=true sbt 'testOnly zio.opendal.NativeTestSpec'"
```

**All tests**:
```bash
docker-compose exec zio-opendal-test sbt "clean; test"
```

#### 4. Interactive Development

**Open a shell in the container:**
```bash
docker-compose exec zio-opendal-test bash
```

**Run SBT interactively:**
```bash
docker-compose exec zio-opendal-test sbt
```

### Container Configuration

#### Volume Mounts
- **Source code**: `.` → `/app` (live sync with your local changes)
- **SBT cache**: Persistent volume for faster subsequent builds
- **Ivy cache**: Persistent volume for Maven dependencies
- **Coursier cache**: Persistent volume for Scala dependencies

#### Environment Variables
- `JAVA_OPTS`: Enables native access for OpenDAL JNI calls
- `SBT_OPTS`: Memory settings optimized for containerized builds
- `ENABLE_OPENDAL_NATIVE_TESTS`: Set to `true` to run native library tests

### Platform Detection

The setup automatically handles platform-specific configuration:

```bash
# In container - shows linux-x86_64
uname -s | tr '[:upper:]' '[:lower:]'  # linux  
uname -m                               # x86_64

# On Apple Silicon host - would show darwin-arm64
uname -s | tr '[:upper:]' '[:lower:]'  # darwin
uname -m                               # arm64
```

## Troubleshooting

### Common Issues

#### 1. Container Won't Start
```bash
# Check Docker is running
docker version

# Rebuild with no cache
docker-compose build --no-cache

# Check logs
docker-compose logs zio-opendal-test
```

#### 2. Native Library Errors
This is expected due to version mismatches between OpenDAL core (0.47.0) and Java bindings (0.46.4):

```
❌ Native tests (live layer): JVM crashed - version incompatibility
```

**Solution**: Use `testLayer` for development, wait for compatible versions, or pin to matching versions.

#### 3. Permission Issues on Linux Hosts
```bash
# Fix file ownership after container operations
sudo chown -R $USER:$USER .
```

#### 4. Slow Performance
```bash
# Clear caches if needed
docker-compose down -v  # Removes volumes
docker-compose up --build

# Or just clear SBT caches
docker-compose exec zio-opendal-test sbt clean
```

### Debugging Native Library Issues

#### Check Available Native Libraries
```bash
docker-compose exec zio-opendal-test find /root/.ivy2 -name "*opendal*" -type f
```

#### Verify Platform Classifier
```bash
docker-compose exec zio-opendal-test sbt "show libraryDependencies" | grep opendal
```

#### Test JNI Loading
```bash
docker-compose exec zio-opendal-test java -Djava.library.path=/path/to/natives -cp classpath YourTestClass
```

## Integration with Development Workflow

### During Development
1. Write code locally using your preferred IDE
2. Use `testLayer` for unit tests (fast, no native dependencies)
3. Periodically run Docker tests to verify cross-platform compatibility

### Before Pull Requests
```bash
# Full validation
docker-compose up --build -d
docker-compose exec zio-opendal-test ./test-docker.sh
docker-compose down

# Should see:
# ✅ Mock tests (testLayer): Working
# ❌ Native tests (live layer): JVM crashed - version incompatibility (expected)
```

### CI/CD Comparison
The Docker environment closely mirrors the GitHub Actions CI:
- Same Linux AMD64 platform
- Similar Java and SBT versions
- Same native library classifiers

## Performance Tips

### Cache Optimization
```bash
# Pre-populate caches (run once)
docker-compose exec zio-opendal-test sbt update

# Use cached builds
docker-compose exec zio-opendal-test sbt compile
```

### Resource Limits
Adjust in `docker-compose.yml` if needed:
```yaml
services:
  zio-opendal-test:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 4G
```

## Alternative Docker Commands

If you prefer `docker run` over `docker-compose`:

```bash
# Build image
docker build -t zio-opendal-test .

# Run with volume mounts
docker run -it --rm \
  -v $(pwd):/app \
  -v zio-opendal-sbt-cache:/root/.sbt \
  -v zio-opendal-ivy-cache:/root/.ivy2 \
  -v zio-opendal-coursier-cache:/root/.cache/coursier \
  --platform linux/amd64 \
  zio-opendal-test ./test-docker.sh
```

## Contributing

When modifying Docker configuration:
- Test changes on both Apple Silicon and Intel Macs if possible
- Update this documentation for any significant changes
- Ensure the environment stays close to CI/CD configuration
- Consider impact on cache performance and build times

## See Also

- [Build Guide](building-and-development.md) - Local development setup
- [Contributing Guide](contributing.md) - Development workflow
- [GitHub Actions Configuration](../../.github/WORKFLOWS.md) - CI/CD setup
