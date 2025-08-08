# Contributing to ZIO OpenDAL

Thank you for your interest in contributing to ZIO OpenDAL! This document provides guidelines for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Process](#development-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Issue Guidelines](#issue-guidelines)
- [Documentation](#documentation)
- [Community](#community)

## Code of Conduct

ZIO OpenDAL follows the [Scala Code of Conduct](https://www.scala-lang.org/conduct/). Please read and follow these guidelines when participating in our community.

### Key Points

- **Be respectful**: Treat everyone with respect and kindness
- **Be inclusive**: Welcome newcomers and different perspectives  
- **Be constructive**: Focus on helping the project and community grow
- **Be professional**: Maintain professionalism in all interactions

## Getting Started

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
# Then clone your fork
git clone https://github.com/YOUR_USERNAME/zio-opendal.git
cd zio-opendal

# Add upstream remote
git remote add upstream https://github.com/ORIGINAL_OWNER/zio-opendal.git
```

### 2. Set Up Development Environment

Follow the [Building and Development Guide](building-and-development.md) to set up your local environment.

### 3. Find an Issue

- Check [Issues](https://github.com/your-org/zio-opendal/issues) for open issues
- Look for issues tagged with `good first issue` or `help wanted`
- Create a new issue if you want to propose a feature or report a bug

## Development Process

### 1. Create a Feature Branch

```bash
# Update your main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/your-feature-name
```

### 2. Make Your Changes

- Write code following our [coding standards](#coding-standards)
- Add tests for new functionality
- Update documentation as needed
- Ensure your changes don't break existing functionality

### 3. Test Your Changes

```bash
# Run all tests
sbt test

# Run formatting checks
sbt scalafmtCheckAll

# Run linting
sbt scalafixAll --check

# Test examples
sbt "project examples" "runMain examples.BasicUsageExample"
```

### 4. Commit Your Changes

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```bash
# Examples of good commit messages
git commit -m "feat: add support for Google Cloud Storage"
git commit -m "fix: handle null values in metadata parsing"
git commit -m "docs: update configuration examples"
git commit -m "test: add integration tests for S3"
git commit -m "refactor: simplify error handling logic"
```

#### Commit Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `test`: Adding or updating tests
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `chore`: Maintenance tasks
- `ci`: CI/CD changes

## Coding Standards

### Scala Style Guide

We follow the [Scala Style Guide](https://docs.scala-lang.org/style/) with some modifications:

#### 1. Formatting

We use Scalafmt for automatic formatting. Configuration is in `.scalafmt.conf`:

```bash
# Format your code
sbt scalafmtAll

# Check formatting
sbt scalafmtCheckAll
```

#### 2. Naming Conventions

```scala
// Classes and traits: PascalCase
class OpenDALService
trait StorageBackend

// Methods and variables: camelCase
def readFile(path: String): ZIO[Any, OpenDALError, Array[Byte]]
val configLayer = ZLayer.succeed(Config.default)

// Constants: UPPER_SNAKE_CASE
val DEFAULT_TIMEOUT = 30.seconds
val MAX_RETRY_ATTEMPTS = 3

// Package names: lowercase
package zio.opendal.config
```

#### 3. Code Organization

```scala
// Order of class members:
class Example {
  // 1. Type definitions
  type Result = Either[Error, Success]
  
  // 2. Values and variables
  private val logger = LoggerFactory.getLogger(getClass)
  
  // 3. Primary constructor logic
  require(condition, "validation message")
  
  // 4. Methods (public first, then private)
  def publicMethod(): Unit = ???
  
  private def privateMethod(): Unit = ???
}
```

#### 4. Error Handling

```scala
// Use ZIO error types consistently
def readFile(path: String): ZIO[Any, OpenDALError, String] = ???

// Provide meaningful error messages
ZIO.fail(NotFoundError(s"File not found: $path", None))

// Use typed errors over exceptions
sealed trait OpenDALError extends Throwable
case class NetworkError(message: String, cause: Option[Throwable]) extends OpenDALError
```

#### 5. Documentation

```scala
/**
 * Reads the entire contents of a file as a string.
 * 
 * @param path The file path to read
 * @param encoding The character encoding to use (defaults to UTF-8)
 * @return A ZIO effect that succeeds with the file contents or fails with an OpenDALError
 * 
 * @example {{{
 * val content: ZIO[OpenDAL, OpenDALError, String] = 
 *   OpenDAL.readString("config.txt")
 * }}}
 */
def readString(
  path: String, 
  encoding: String = "UTF-8"
): ZIO[OpenDAL, OpenDALError, String] = ???
```

### Functional Programming Guidelines

#### 1. Immutability

```scala
// Prefer immutable data structures
case class Config(bucket: String, region: String, timeout: Duration)

// Use immutable collections
val providers: List[String] = List("s3", "gcs", "azure")

// Make fields private and immutable when possible
class Service private (config: Config) {
  // ...
}
```

#### 2. Pure Functions

```scala
// Good: Pure function
def parseConfig(json: String): Either[ConfigError, Config] = ???

// Avoid: Functions with side effects
def parseConfigImpure(json: String): Config = {
  println("Parsing config...") // Side effect!
  // ...
}
```

#### 3. ZIO Best Practices

```scala
// Use ZIO constructors appropriately
ZIO.succeed(value)           // For pure values
ZIO.attempt(riskOperation)   // For operations that can throw
ZIO.fail(error)             // For known errors
ZIO.fromEither(either)       // Convert from Either

// Compose effects properly
for {
  config <- loadConfig()
  client <- createClient(config)
  result <- client.operation()
} yield result

// Handle resources safely
ZIO.scoped {
  ZIO.acquireRelease(openFile)(closeFile).flatMap(useFile)
}
```

## Testing Guidelines

### 1. Test Structure

```scala
import zio.test._

object ServiceSpec extends ZIOSpecDefault {
  
  def spec = suite("Service")(
    suite("read operations")(
      test("should read existing files") {
        for {
          _ <- OpenDAL.writeText("test.txt", "content")
          result <- OpenDAL.readString("test.txt")
        } yield assertTrue(result == "content")
      },
      
      test("should handle missing files") {
        for {
          result <- OpenDAL.readString("missing.txt").either
        } yield assertTrue(result.isLeft)
      }
    ),
    
    suite("write operations")(
      test("should write and read back data") {
        // ... test implementation
      }
    )
  ).provide(testLayer)
  
  val testLayer = OpenDAL.testLayer(LayerConfig.memory())
}
```

### 2. Test Categories

#### Unit Tests
- Test individual functions and methods
- Use test doubles/mocks when needed
- Fast execution
- No external dependencies

```scala
test("config parser should handle valid JSON") {
  val json = """{"bucket": "test", "region": "us-east-1"}"""
  val result = ConfigParser.parse(json)
  assertTrue(result.isRight)
}
```

#### Integration Tests
- Test component interactions
- Use real storage backends when possible
- May require external setup (Docker, credentials)

```scala
test("should work with real S3 backend") {
  for {
    _ <- OpenDAL.writeText("integration-test.txt", "test data")
    result <- OpenDAL.readString("integration-test.txt")
    _ <- OpenDAL.delete("integration-test.txt")
  } yield assertTrue(result == "test data")
}.provide(realS3Layer) @@ TestAspect.ifEnvSet("RUN_INTEGRATION_TESTS")
```

### 3. Test Data and Fixtures

```scala
object TestData {
  val sampleConfig = Config(
    bucket = "test-bucket",
    region = "us-east-1",
    timeout = 30.seconds
  )
  
  val sampleFileContent = "sample file content for testing"
  
  def randomFileName: String = s"test-${Random.nextInt(10000)}.txt"
}
```

### 4. Property-Based Testing

```scala
import zio.test.Gen

test("round-trip property: write then read should return same content") {
  check(Gen.alphaNumericString) { content =>
    val fileName = TestData.randomFileName
    for {
      _ <- OpenDAL.writeText(fileName, content)
      readContent <- OpenDAL.readString(fileName)
      _ <- OpenDAL.delete(fileName)
    } yield assertTrue(readContent == content)
  }
}
```

## Pull Request Process

### 1. Before Submitting

- [ ] Code compiles without warnings
- [ ] All tests pass
- [ ] Code is formatted (`sbt scalafmtAll`)
- [ ] Code passes linting (`sbt scalafixAll --check`)
- [ ] Documentation is updated if needed
- [ ] Examples work if applicable

### 2. PR Description Template

```markdown
## Description
Brief description of the changes and why they're needed.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Refactoring

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed
- [ ] Examples tested

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No breaking changes (or clearly documented)

## Related Issues
Closes #123
Relates to #456
```

### 3. Review Process

1. **Automated Checks**: CI must pass
2. **Code Review**: At least one maintainer review required
3. **Testing**: Reviewer may test the changes locally
4. **Documentation**: Ensure docs are adequate
5. **Approval**: Maintainer approval required for merge

### 4. Addressing Review Feedback

```bash
# Make requested changes
git add .
git commit -m "fix: address review feedback"

# Force push if you need to rewrite history (use carefully)
git push --force-with-lease origin feature/your-feature-name
```

## Issue Guidelines

### 1. Bug Reports

Use the bug report template:

```markdown
## Bug Description
Clear description of what the bug is.

## Steps to Reproduce
1. Step one
2. Step two
3. Step three

## Expected Behavior
What you expected to happen.

## Actual Behavior
What actually happened.

## Environment
- ZIO OpenDAL version: 
- Scala version:
- JVM version:
- OS:
- Storage backend:

## Additional Context
Any other relevant information.
```

### 2. Feature Requests

```markdown
## Feature Description
Clear description of the feature you'd like to see.

## Use Case
Why is this feature needed? What problem does it solve?

## Proposed Solution
How you think this should be implemented.

## Alternatives Considered
Other solutions you've considered.

## Additional Context
Any other relevant information.
```

### 3. Issue Labels

- `bug`: Something isn't working
- `enhancement`: New feature or request
- `documentation`: Improvements or additions to documentation
- `good first issue`: Good for newcomers
- `help wanted`: Extra attention is needed
- `question`: Further information is requested
- `wontfix`: This will not be worked on

## Documentation

### 1. Code Documentation

- Write ScalaDoc for public APIs
- Include usage examples
- Document parameter and return types
- Explain complex algorithms or business logic

### 2. README Updates

- Keep README.md up to date
- Include simple usage examples
- Link to comprehensive documentation

### 3. Documentation Site

- Update user guides when adding features
- Add examples for new functionality
- Keep configuration documentation current

## Community

### 1. Communication Channels

- **GitHub Issues**: Bug reports, feature requests
- **GitHub Discussions**: Questions, ideas, general discussion
- **Discord/Slack**: Real-time chat (if available)

### 2. Getting Help

- Check existing documentation first
- Search GitHub issues for similar problems
- Ask questions in discussions before creating issues
- Provide minimal reproducible examples

### 3. Helping Others

- Answer questions in discussions and issues
- Review pull requests
- Contribute to documentation
- Share your experiences using the library

## Release Process

See [Release Process](release-process.md) for information about how releases are managed.

## Recognition

Contributors will be:
- Added to the contributors list in README.md
- Mentioned in release notes for significant contributions
- Invited to become maintainers for sustained contributions

Thank you for contributing to ZIO OpenDAL! 🎉
