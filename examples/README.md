# ZIO OpenDAL Examples

This directory contains comprehensive examples demonstrating how to use the ZIO OpenDAL library effectively.

## Available Examples

### 1. BasicUsageExample
Demonstrates fundamental OpenDAL operations:
- **Writing and reading files** - Basic file operations with text and binary data
- **Partial reads** - Reading specific byte ranges from files
- **File operations** - Copy, rename, delete, and existence checks
- **Directory operations** - Creating directories, listing files, recursive operations
- **Metadata** - Getting file information like size and content type

**Run with:**
```bash
sbt "project examples" "runMain examples.BasicUsageExample"
```

### 2. CloudStorageExample  
Shows configuration for different cloud storage providers:
- **AWS S3** - Standard S3 configuration with credentials
- **S3-Compatible Services** - MinIO, DigitalOcean Spaces, etc.
- **Azure Blob Storage** - Account name and key configuration
- **Google Cloud Storage** - Service account and project setup

**Run with:**
```bash
sbt "project examples" "runMain examples.CloudStorageExample"
```

### 3. SyntaxExtensionsExample
Demonstrates the main API usage patterns:
- **Standard API usage** - The recommended approach
- **File operations** - Read, write, copy, rename, delete
- **Metadata operations** - File size, existence checks
- **Directory operations** - Listing, creating, removing directories

**Run with:**
```bash
sbt "project examples" "runMain examples.SyntaxExtensionsExample"
```

### 4. SyntaxExtensionsWorkingExample
Shows how to properly use syntax extensions when needed:
- **Local scope imports** - Using extensions in limited scope
- **Extension methods** - Byte array and metadata extensions that work
- **Best practices** - When to use extensions vs. regular API

**Run with:**
```bash
sbt "project examples" "runMain examples.SyntaxExtensionsWorkingExample"
```

## Cross-Compilation Support

All examples compile and run on multiple Scala versions:
- **Scala 2.13.16** - Full compatibility
- **Scala 3.3.4** - LTS version support  
- **Scala 3.4.3** - Latest stable support
- **Scala 3.5.2** - Cutting edge support

### Testing All Versions
```bash
# Compile for all versions
sbt "project examples" "+compile"

# Run with specific version
sbt "project examples" "++3.3.4 runMain examples.BasicUsageExample"
sbt "project examples" "++2.13.16 runMain examples.BasicUsageExample"
```

## Key Features Demonstrated

### Configuration Patterns
- **Test layers** - For development and testing (no native dependencies)
- **Live layers** - For production use with real storage backends
- **Builder patterns** - Type-safe configuration with validation

### Error Handling
- **Typed errors** - Specific error types for different failure modes
- **Retry logic** - Automatic retry for transient failures
- **Error categories** - Distinguishing between retriable and non-retriable errors

### Best Practices
- **Resource management** - Proper cleanup of files and directories
- **Memory efficiency** - Streaming operations for large files
- **Cross-platform compatibility** - Works on all supported Scala versions

## Notes

### Syntax Extensions
The syntax extensions provide convenient methods but may conflict with Scala standard library implicits. The examples show:
- How to use them safely in local scopes
- When to prefer the regular API
- Cross-version compatibility considerations

### Test vs. Live Layers
Examples use test layers by default to avoid requiring real cloud credentials or native library installations. For production use:
- Replace `OpenDAL.testLayer()` with `OpenDAL.live(config)` 
- Provide real credentials and configuration
- Ensure native libraries are available in your deployment

### Performance
- Examples use small test files for demonstration
- For production workloads, consider streaming APIs for large files
- Configure appropriate buffer sizes and retry policies

## Getting Started

1. **Choose an example** that matches your use case
2. **Run it** to see the basic functionality  
3. **Modify the configuration** to match your storage backend
4. **Adapt the code** to your specific requirements

The examples are designed to be educational and can serve as starting points for your own applications.
