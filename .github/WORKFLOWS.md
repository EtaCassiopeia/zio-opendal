# GitHub Actions Configuration

This directory contains GitHub Actions workflows for the zio-opendal project.

## Workflows

### 1. CI/CD Pipeline (`.github/workflows/ci.yml`)

**Purpose**: Continuous Integration and Deployment pipeline

**Triggers**:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches  
- Push of version tags (e.g., `v0.1.0`)

**Jobs**:

1. **Lint**: Code formatting and binary compatibility checks
   - `scalafmtCheckAll`: Ensures code is properly formatted
   - `mimaReportBinaryIssues`: Checks for binary compatibility issues

2. **Test**: Cross-platform and cross-version testing
   - **OS Matrix**: Ubuntu, macOS, Windows
   - **Scala Matrix**: 2.13.16, 3.3.4, 3.4.3, 3.5.2  
   - **Java Matrix**: 11, 17, 21
   - **Integration Tests**: Native library tests (Ubuntu only)

3. **Documentation**: API documentation generation
   - Generates Scaladoc
   - Publishes to GitHub Pages on main branch

4. **Publish**: Automated publishing to Sonatype
   - **Snapshots**: Published on push to main/develop
   - **Releases**: Published on version tag creation
   - **Dependencies**: Submitted to GitHub's dependency graph

### 2. Release Workflow (`.github/workflows/release.yml`)

**Purpose**: Streamlined release process with version management

**Trigger**: Manual workflow dispatch with version inputs

**Process**:
1. Updates `build.sbt` with release version
2. Creates and pushes git tag
3. Creates GitHub release with generated notes
4. Bumps to next development version

## Required Secrets

Configure these secrets in GitHub repository settings:

### Publishing Secrets
- `SONATYPE_USERNAME`: Sonatype account username
- `SONATYPE_PASSWORD`: Sonatype account password  
- `PGP_PRIVATE_KEY`: PGP private key for artifact signing
- `PGP_PASSPHRASE`: Passphrase for PGP private key

See [PUBLISHING.md](../PUBLISHING.md) for detailed setup instructions.

## Workflow Features

### Cross-Platform Testing
- Tests on Linux, macOS, and Windows
- Ensures broad compatibility
- Native library tests (Linux only due to OpenDAL availability)

### Cross-Version Support
- Tests all supported Scala versions
- Tests multiple Java versions
- Matrix strategy with smart exclusions to optimize CI time

### Security
- Uses official GitHub actions with pinned versions
- Secrets are never exposed in logs
- PGP signing ensures artifact integrity
- Minimal permissions (read-only for most steps)

### Performance Optimizations
- SBT dependency caching
- Parallel job execution
- Selective matrix exclusions
- Incremental builds where possible

### Quality Gates
- All tests must pass before publishing
- Code formatting checks
- Binary compatibility verification
- Documentation generation success

## Manual Operations

### Running Workflows Locally

```bash
# Test cross-compilation (mimics CI)
sbt +test

# Check formatting (mimics lint job)
sbt scalafmtCheckAll

# Generate documentation (mimics docs job)
sbt doc

# Test publishing locally (without upload)
sbt +publishLocal
```

### Releasing

1. **Automated** (Recommended):
   - Use the Release workflow in GitHub Actions
   - Go to Actions → Release → Run workflow
   - Enter version numbers

2. **Manual**:
   ```bash
   # Update version in build.sbt
   # Commit, tag, and push
   git tag v0.1.0
   git push origin v0.1.0
   ```

### Emergency Procedures

If CI is failing and you need to publish urgently:

```bash
# Set up environment
export SONATYPE_USERNAME="..."
export SONATYPE_PASSWORD="..."  
export PGP_PASSPHRASE="..."

# Publish manually
sbt +publishSigned sonatypeBundleRelease
```

## Monitoring

- **Build Status**: Check the Actions tab for current status
- **Dependencies**: GitHub automatically tracks dependencies
- **Security**: Dependabot monitors for vulnerable dependencies
- **Performance**: Track CI job duration for optimizations

## Troubleshooting

### Common Issues

1. **Test Failures**: Check logs for specific Scala version or platform issues
2. **Publishing Issues**: Verify secrets are correctly configured
3. **Documentation**: Ensure all code has proper documentation
4. **Binary Compatibility**: Use MiMa to check compatibility requirements

### Getting Help

- Check workflow logs in the Actions tab
- Review [PUBLISHING.md](../PUBLISHING.md) for publishing issues
- Open an issue for workflow problems
- Check sbt plugin documentation for build issues

## Contributing

When modifying workflows:
- Test changes on a fork first
- Use pinned action versions for security
- Update this documentation for any significant changes
- Consider impact on CI performance and costs
