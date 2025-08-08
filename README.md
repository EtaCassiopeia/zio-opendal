# ZIO OpenDAL

A ZIO-friendly Scala wrapper for [Apache OpenDAL](https://opendal.apache.org/), providing unified access to 40+ storage services including S3, Azure Blob Storage, Google Cloud Storage, local filesystem, and many more.

[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

## ✨ Features

- 🚀 **Unified API** - Single interface for 40+ storage services
- ⚡ **ZIO Integration** - Native ZIO effects with proper error handling
- 🔧 **Type Safety** - Comprehensive error types and typed configurations  
- 🏗️ **Modular Design** - Well-organized packages for maintainability
- 🧪 **Testing Support** - Built-in test layer for mocking without native dependencies
- 🔄 **Cross-Platform** - Support for Scala 2.13 and Scala 3.x

## 🚀 Quick Start

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.etacassiopeia" %% "zio-opendal" % "0.1.0"
```

**Basic usage:**

```scala
import zio.*
import zio.opendal.OpenDAL
import zio.opendal.config.LayerConfig

object MyApp extends ZIOAppDefault {
  
  val s3Config = LayerConfig.s3(
    bucket = "my-bucket",
    region = "us-east-1",
    accessKeyId = sys.env("AWS_ACCESS_KEY_ID"),
    secretAccessKey = sys.env("AWS_SECRET_ACCESS_KEY")
  )
  
  val program = for {
    _ <- OpenDAL.writeText("hello.txt", "Hello, World!")
    content <- OpenDAL.readString("hello.txt")
    _ <- Console.printLine(s"Content: $content")
  } yield ()
  
  def run = program.provide(OpenDAL.live(s3Config))
}
```

## 📚 Documentation

### For Library Users
- **[Getting Started](docs/users/getting-started.md)** - Installation and first steps
- **[Configuration Guide](docs/users/configuration.md)** - Storage backend setup
- **[API Reference](docs/users/api-reference.md)** - Complete API documentation
- **[Examples](docs/users/examples.md)** - Working examples for common use cases

### For Project Maintainers
- **[Build Guide](docs/maintainers/build-guide.md)** - How to build and test the project
- **[Architecture](docs/maintainers/architecture.md)** - Project structure and design decisions
- **[Contributing](docs/maintainers/contributing.md)** - Development workflow and guidelines
- **[Release Process](docs/maintainers/release-process.md)** - How to publish new versions

## 🛠️ Supported Storage Services

**Cloud Storage:** AWS S3, Azure Blob Storage, Google Cloud Storage, Alibaba Cloud OSS, Tencent Cloud COS, Huawei Cloud OBS, Backblaze B2

**Local Storage:** Filesystem, Memory (for testing)

**Databases:** Redis, MySQL, PostgreSQL, MongoDB, SQLite, and many more...

**See the complete list in our [Configuration Guide](docs/users/configuration.md#supported-services)**

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](docs/maintainers/contributing.md) for details on:

- Setting up the development environment
- Running tests and examples (including Docker testing for cross-platform compatibility)
- Code style and conventions
- Pull request process

**Quick Docker Testing** (especially useful for Apple Silicon users):
```bash
docker-compose up --build -d
docker-compose exec zio-opendal-test ./test-docker.sh
```
See [Docker Testing Guide](docs/maintainers/docker-testing.md) for detailed instructions.

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙋‍♂️ Support

- **Issues**: [GitHub Issues](https://github.com/etacassiopeia/zio-opendal/issues)
- **Discussions**: [GitHub Discussions](https://github.com/etacassiopeia/zio-opendal/discussions)
- **Documentation**: [GitHub Pages](https://etacassiopeia.github.io/zio-opendal/)
